# Hermi: High-Precision Edge AI Assistant for Automotive

Hermi is an innovative, privacy-focused voice assistant designed for Android Automotive OS (AAOS). It leverages advanced **Edge AI** technologies to provide high-precision voice control without requiring an internet connection.

## 🚀 Key Innovation: Edge AI

Unlike traditional voice assistants, Hermi processes everything locally on the device.

*   **Privacy-First:** User data never leaves the vehicle; no audio is uploaded to the cloud.
*   **Offline Reliability:** Works seamlessly in areas with poor or no cellular connectivity.
*   **High Precision:** Achieves **99.2% recognition accuracy** for specific automotive commands.
*   **Optimized Performance:** Small footprint (~115 MB model) optimized for on-device NPU/CPU execution using ONNX Runtime.

## 🛠 Technical Architecture

The system is built on a modular architecture using modern Android standards and specialized AI components:

### 1. Voice Activity Detection (VAD) - `SileroVAD`
Powered by **Silero VAD v5** via ONNX Runtime.
*   **Efficiency:** Highly optimized to distinguish speech from car noise with minimal latency.
*   **Adaptive State:** Maintains a rank-3 state [2, 1, 128] for temporal consistency.
*   **Performance:** Operates on 16kHz mono audio chunks with a 1000ms silence threshold.

### 2. Intent Recognition - `ONNXAIProcessor`
A custom NLU engine using **MobileBERT** quantized to ONNX.
*   **Task:** Maps transcribed text to structured vehicle commands.
*   **Vocabulary:** Uses a specialized automotive vocabulary for precise intent extraction.
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

*   **Accuracy:** 99.2% (Intent classification)
*   **Model Size:** ~115 MB
*   **VAD Threshold:** 1000ms silence detection
*   **Command Support:** 30+ automotive-specific intents (Fuel, Oil, Navigation, Engine status, etc.)

## 🛠 Development & Build

The project uses Hilt for Dependency Injection and Kotlin Coroutines/Flow for reactive data handling.

```bash
# Clean and build the project
./gradlew clean assembleDebug

# Install on Automotive device/emulator
./gradlew :automotive:installDebug
```

---
*Developed by Artem Khateev*
