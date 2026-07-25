# Hermi: High-Precision Edge AI Assistant for Automotive

Hermi is an innovative, privacy-focused voice assistant designed for Android Automotive OS (AAOS). It leverages advanced **Edge AI** technologies to provide high-precision voice control without requiring an internet connection.

## 🚀 Key Innovation: Edge AI

Unlike traditional voice assistants, Hermi processes everything locally on the device.

*   **Privacy-First:** User data never leaves the vehicle; no audio is uploaded to the cloud.
*   **Offline Reliability:** Works seamlessly in areas with poor or no cellular connectivity.
*   **High Precision:** 100% on the intent model's own phrase bank; 80% end-to-end on clean synthesized speech across seven accents (see the report below).
*   **Optimized Performance:** 4.7 MB INT8 intent model, optimized for on-device NPU/CPU execution using ONNX Runtime.

## 🛠 Technical Architecture

The system is built on a modular architecture using modern Android standards and specialized AI components:

### 1. Voice Activity Detection (VAD) - `SileroVAD`
Powered by **Silero VAD v5** via ONNX Runtime.
*   **Efficiency:** Highly optimized to distinguish speech from car noise with minimal latency.
*   **Adaptive State:** Maintains a rank-3 state [2, 1, 128] for temporal consistency.
*   **Performance:** Operates on 16kHz mono audio chunks with a 1000ms silence threshold.

### 2. Intent Recognition - `ONNXAIProcessor`
A custom NLU engine using **BERT-Tiny** quantized to INT8 ONNX (4.7 MB).
*   **Task:** Maps transcribed text to structured vehicle commands.
*   **Coverage:** 110 labels collapsing onto 102 cabin commands (see below).
*   **Speed:** Inference happens in milliseconds on standard mobile processors.

### 3. Speech-to-Text (STT) - `VoskManager`
Uses the **Vosk** offline engine for reliable transcription.
*   **Streaming:** Audio is processed in real-time chunks for lower perceived latency.
*   **Accuracy:** Calibrated specifically for command-based automotive interactions.

## 🏗 Project Structure

*   `shared/`: Core AI logic, interfaces, and ONNX models.
*   `automotive/`: AAOS-specific implementation, `VoiceInteractionService`, and Car App integration.
*   `mobile/`: Mobile companion app (under development).

## 📊 Performance Metrics

*   **Accuracy:** 100% on the model's phrase bank; 80% end-to-end on clean TTS speech, 38% under white noise at SNR 10 dB
*   **Model Size:** 4.7 MB (intent) + 2.3 MB (VAD) + 40 MB (Vosk)
*   **VAD Threshold:** 1000ms silence detection
*   **Command Support:** 102 cabin commands (climate, body & access, seats, lights, media, phone, navigation, cameras, wipers)

## 🛠 Development & Build

The project uses Hilt for Dependency Injection and Kotlin Coroutines/Flow for reactive data handling.

```bash
# Clean and build the project
./gradlew clean assembleDebug

# Install on Automotive device/emulator
./gradlew :automotive:installDebug
```


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

---
*Developed by Artem Khateev*
