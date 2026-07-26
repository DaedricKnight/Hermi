# Hermi
Your digital guide.

## Intent model

Hermi runs the **automotive intent model v2.2** (bert-tiny, INT8, 4.7 MB,
`shared/src/main/assets/automotive_intent_quant.onnx`) — 110 cabin-control labels
plus a `none` reject class: body & access, climate, seats & heating, lights,
media, radio, phone, navigation, cameras, parking aids, wipers, system.

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

**Rejecting what is not a command.** Two mechanisms, measured on device:

- the `none` class refuses off-topic speech — **25 of 25** held-out everyday
  phrases on device (v1, which had no reject class, managed 16/25).
  `ONNXAIProcessorIntegrationTest` measures the rate and fails if it regresses.
- `CommandMapper`'s 0.95 confidence gate catches the rest. It costs nothing
  in-domain (100% of the phrase bank at every gate up to 0.98 — see
  `tools/measure_gate.py`).

**Constrained decoding.** Vosk decodes against `grammar_phrases.txt` (the command
phrase bank *plus the reject-class phrasings*, generated with the catalog) instead
of the open vocabulary, and `[unk]` fillers are stripped from the result. The car
understands ~100 commands, so there is no reason to let the recognizer invent
words: "the on the headlights" becomes "turn on the headlights", "cancel the
roots" becomes "cancel the route". The reject phrasings matter as much as the
commands — without them off-topic speech gets forced into a command and
out-of-domain safety drops from 100% to 39%. Pass
`VoskManager(context, useGrammar = false)` for the open-vocabulary baseline;
`VoicePipelineTest.grammarDecoding_beatsOpenVocabulary` runs both on the same clips.

The 40 MB `vosk-model-small-en-us-0.15` stays on purpose: the 204 MB
`vosk-model-en-us-0.22-lgraph` was measured on the same grid and lost on every
axis — commands 93% against 97%, aged voice 75% against 86%, white noise 77%
against 89%. A bigger language model prefers the *fluent* hypothesis, and for
two-word car commands that is the wrong prior ("turn up the volume" → "turn off
the volume"). See the report.

**Phrasings are measured, not guessed.** `CommandCatalog` leads with the phrasing
`tools/candidate_phrases_test.py` found most robust — "increase the volume", not
"turn up the volume", which comes back as "turn off the volume" under noise and
mutes the car. One-word commands ("louder") transcribe perfectly but carry too
little signal for a 111-class model, so they stay out of the taught set.

Actuation is still pending a VHAL writer — `core_vhal` is read-only, so commands
are recognized and acknowledged, and navigation intents hand off to the nav app.

## Voice testing

Test phrases are synthesized with macOS TTS (`say -v Samantha/Daniel/Karen/…
--file-format=WAVE --data-format=LEI16@16000`) — no human speaker, nothing
played out loud — and run through the same chain the app uses: Vosk →
`automotive_intent_quant.onnx` → CommandMapper's 0.95 confidence gate. Two
levels:

1. **Host accent stand** (`tools/accent_stress_test.py`) — 22 phrases × 7
   accents (US/GB/AU/IE/IN/ZA + aged-GB) × 5 conditions (140/180/220 wpm,
   180 wpm with road noise and with white noise), with a text baseline per phrase
   so a model error is never mistaken for an accent error. Current numbers:
   commands 645/665 (97%), off-topic refused 105/105, road noise 99%, white noise
   89%. Methodology and the per-step gains:
   [tools/accent_test_out/REPORT.md](tools/accent_test_out/REPORT.md).

   ```
   pip3 install vosk onnxruntime numpy
   python3 tools/accent_stress_test.py           # full grid (~12 min)
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
