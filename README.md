# 🚗 Hermi — Android Automotive Voice Assistant

Hermi is an offline voice assistant built for Android Automotive OS. It understands natural speech commands for navigation, vehicle status monitoring, and car controls — all without an internet connection.

---

## Demo

https://github.com/user-attachments/assets/98630c60-e378-4727-8e57-437d5a4f5116

---

## Features

### 🗺️ Navigation & Search
Hermi can launch Google Maps or the system navigation app with a relevant search query:

| Command example | Action |
|---|---|
| *"Find food nearby"* | Search for restaurants |
| *"Find a gas station"* | Search for gas stations |
| *"Find a charging station"* | Search for EV charging |
| *"Find parking"* | Search for parking |

### 🚘 Vehicle Status
Query live vehicle data read from the VHAL (Vehicle Hardware Abstraction Layer):

| Command example | Action |
|---|---|
| *"What's the vehicle status?"* | General vehicle health summary |
| *"Check engine"* | Engine warning status |
| *"Check tires"* | Tire pressure status |
| *"Check fuel level"* | Current fuel level |
| *"Check battery level"* | High-voltage battery level |

### 🔧 Vehicle Controls

| Command example | Action |
|---|---|
| *"Lock doors"* | Lock all doors |
| *"Unlock doors"* | Unlock all doors |
| *"Turn on lights"* | Turn on headlights |
| *"Turn off lights"* | Turn off headlights |

### 📊 Real-Time Info

| Command example | Action |
|---|---|
| *"What's my range?"* | Remaining driving range |
| *"Show odometer"* | Total distance traveled |
| *"What's my speed?"* | Current vehicle speed |
| *"What's the outside temperature?"* | Exterior temperature with unit |

---

## Architecture

```
┌─────────────────────────────────────────────┐
│              Android Automotive OS           │
├─────────────────────────────────────────────┤
│  HermiViewModel (MVVM)                      │
│    ├── AudioRecorder  → captures microphone │
│    ├── VoskManager    → offline STT         │
│    ├── AIProcessor    → intent classifier   │
│    ├── VoiceManager   → TTS responses       │
│    └── VhalReader     → vehicle data        │
└─────────────────────────────────────────────┘
```

- **Speech-to-Text**: [Vosk](https://alphacephei.com/vosk/) — fully offline, on-device recognition
- **Intent Classification**: DistilBERT model converted to TFLite, 20 intent classes, ~99.455% accuracy
- **Vehicle Data**: Read from VHAL via `VhalReader` (speed, range, odometer, temperature, etc.)
- **TTS**: Android `TextToSpeech` for voice responses
- **UI**: Jetpack Compose
- **DI**: Dagger 2 / Hilt
- **Architecture**: MVVM + StateFlow

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM, Clean Architecture |
| Speech Recognition | Vosk (offline) |
| ML / NLU | DistilBERT → TFLite |
| Vehicle Data | Android Automotive VHAL |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Testing | JUnit4, Mockito, kotlinx-coroutines-test |

---

## How It Works

1. User taps the microphone button
2. `AudioRecorder` captures raw audio from the microphone
3. `VoskManager` transcribes the audio to text **on-device** (no internet required)
4. `AIProcessor` runs the TFLite DistilBERT model to classify the intent
5. `HermiViewModel` handles the recognized `Command` and:
   - reads live data from `VhalReader` if needed
   - emits a navigation event or
   - speaks a response via `VoiceInterfaceManager`

---

## ML Model

The intent classifier is based on **DistilBERT**, fine-tuned on automotive voice commands:

- **20 intent classes** covering navigation, vehicle status, controls, and info queries
- **~99.45% accuracy** on test set (183 samples)
- Exported to **ONNX → TFLite** with Flex ops for on-device inference
- Input: tokenized text (input_ids + attention_mask)
- Output: softmax probabilities over 20 classes

---

## Project Structure

```
Hermi/
├── automotive/          # Android Automotive app module
│   ├── src/main/
│   │   ├── ui/          # Compose screens
│   │   └── viewmodel/   # HermiViewModel
│   └── src/test/        # Unit tests
└── shared/              # Shared module
    ├── ai/              # TFLiteAIProcessor, tokenizer
    ├── audio/           # AudioRecorderImpl
    ├── manager/         # VoiceInterfaceManager
    ├── model/           # Command sealed class
    └── interfaces/      # AIProcessor, AudioRecorder, RecognitionManager
```

---

## Getting Started

### Requirements

- Android Studio Hedgehog or later
- Android Automotive OS emulator or physical device
- Vosk model file (place in `assets/`)

### Setup

```bash
git clone https://github.com/yourusername/hermi.git
cd hermi
```

1. Download a [Vosk model](https://alphacephei.com/vosk/models) (e.g. `vosk-model-small-en-us`) and place it in `shared/src/main/assets/model/`
2. Open the project in Android Studio
3. Run on an Android Automotive emulator or device

---

## Running Tests

```bash
./gradlew :automotive:testDebugUnitTest
./gradlew :shared:testDebugUnitTest
```

Tests cover: `HermiViewModel`, `TFLiteAIProcessor`, `VoskManager`, `VoiceInterfaceManager`, `AudioRecorderImpl`

---

## License

MIT License. See [LICENSE](LICENSE) for details.
