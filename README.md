# Hermi
Your digital guide.

## Intent model

Hermi runs the **110-label automotive intent model** (bert-tiny, INT8, 4.7 MB,
`shared/src/main/assets/automotive_intent_quant.onnx`) — cabin control rather
than diagnostics: body & access, climate, seats & heating, lights, media, radio,
phone, navigation, cameras, parking aids, wipers, system.

Eight of those labels are synonym pairs (`media_play` / `media_play_music`,
`navi_zoom_in` / `navigation_zoom_in`, …), so **110 labels collapse onto 102
commands**. The Kotlin command surface is generated from the label set — never
hand-edited:

```bash
python3 tools/generate_command_catalog.py   # Command.kt, CommandCatalog.kt, TestPhrases.kt
```

`CommandCatalog` is the single source of truth: label(s) → command, spoken
acknowledgement, FREE/PRO tier, example phrasings. The ViewModel, the command
list UI and the tests all read from it, so adding an intent to the model is a
regeneration, not a refactor.

**Known limitation:** the model has no `unknown`/`none` class, so the 0.95
confidence gate in `CommandMapper` is the only thing between off-topic speech and
a car command — it rejects 16 of 25 everyday non-car phrases; the rest fire
something ("order a pizza" → find restaurant, "what is your name" → zoom in).
The fix belongs on the model side: train a `none` class. `ONNXAIProcessorIntegrationTest`
measures the rate and fails if it regresses.

Actuation is still pending a VHAL writer — `core_vhal` is read-only, so commands
are recognized and acknowledged, and navigation intents hand off to the nav app.

## Voice testing

Test phrases are synthesized with macOS TTS (`say -v Samantha/Daniel/Karen/…
--file-format=WAVE --data-format=LEI16@16000`) — no human speaker, nothing
played out loud — and run through the same chain the app uses: Vosk →
`automotive_intent_quant.onnx` → CommandMapper's 0.95 confidence gate. Two
levels:

1. **Host accent stand** (`tools/accent_stress_test.py`) — 22 phrases × 7
   accents (US/GB/AU/IE/IN/ZA + aged-GB) × 4 conditions (140/180/220 wpm and
   180 wpm with white noise), with a text baseline per phrase so a model error is
   never mistaken for an accent error. Results and methodology:
   [tools/accent_test_out/REPORT.md](tools/accent_test_out/REPORT.md).

   ```
   pip3 install vosk onnxruntime numpy
   python3 tools/accent_stress_test.py           # full grid (~30 min)
   python3 tools/accent_stress_test.py --quick   # 3 accents x 2 conditions
   ```

2. **Instrumented end-to-end** (`VoicePipelineTest`) — 21 clips bundled as
   androidTest assets (`shared/src/androidTest/assets/tts/`, regenerate with
   `./tools/generate_test_wavs.sh`): one per accent, slow/fast delivery of the
   same command, a synonym-label probe, an out-of-domain probe, plus Silero VAD
   speech/silence discrimination and VADManager end-of-speech detection.

```bash
./gradlew test                             # unit tests
./gradlew :shared:connectedDebugAndroidTest -Pandroid.injected.build.abi=arm64-v8a
```

`-Pandroid.injected.build.abi` matters: with all four ABIs the androidTest APK
is ~490 MB (TFLite flex + onnxruntime + Vosk natives) and stock emulator images
refuse to install it. Text-level accuracy over the full phrase bank lives in
`ONNXAIProcessorIntegrationTest`.
