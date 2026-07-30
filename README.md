# 📱 CyPy Translator - Native Android Kotlin

A high-performance, native Android application for automatic manga and comic translation powered by **ONNX Runtime (YOLO)**, **OpenCV C++ Native**, and **Multi-Provider Vision LLMs** (Gemini, OpenAI, OpenRouter, Zen, OpenCode Go, and Custom OpenAI-Compatible Endpoints).

---

## 🌟 Key Features

- **🚀 Native Performance:** Built with Jetpack Compose, Kotlin Coroutines, and OpenCV C++ JNI for instant UI responsiveness and minimal memory consumption.
- **🎯 YOLO Speech Bubble Detection:** Uses embedded ONNX Runtime to detect speech bubbles on-device with high precision.
- **🤖 Multi-Provider LLM Support:**
  - **Google Gemini** (`gemini-3.1-flash-lite`, etc.)
  - **OpenAI** (`gpt-5.4-mini`, etc.)
  - **OpenRouter** (`qwen2.5-vl-72b`, etc.)
  - **Zen / OpenCode Go**
  - **Custom / Local AI:** Dynamic Base URL & Auto-Model Detection for Ollama, LM Studio, vLLM, and local servers (with/without API Key).
- **🧩 Smart Mosaic Batching:** Partitions comic bubbles into high-density mosaics with vertical RTL reading order to minimize API requests and costs.
- **🎨 Dynamic Typography & Masking:** Automatic background masking (rounded rectangle / oval) and font scaling for seamless in-bubble rendering.
- **📂 Public Gallery Output:** Translated pages save directly to `/Download/CYPY/` so they appear instantly in Galeri HP and Google Photos.
- **⚡ PhotoPicker & Scoped Storage Safe:** Compatible with Android 10 to Android 15+.

---

## 🛠️ Project Structure

```text
├── app/
│   ├── src/main/
│   │   ├── java/com/cypy/app/
│   │   │   ├── core/           # Translation Pipeline, YOLO, OpenCV, TextRenderer
│   │   │   ├── core/providers/ # Gemini, OpenAI, Custom LLM Provider Integrations
│   │   │   ├── data/           # Settings & DataStore Persistence
│   │   │   ├── ui/             # Jetpack Compose Screens & MainViewModel
│   │   │   └── util/           # JsonUtils & Helper utilities
│   │   ├── assets/             # YOLO ONNX model (eyecypy.dat) & Custom Fonts
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── opencv/                      # OpenCV 4.x Android SDK Module (Native JNI)
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📦 Building the APK

1. Clone the repository:
   ```bash
   git clone https://github.com/kouzen-neo/cypy-mobile-native-kotlin.git
   cd cypy-mobile-native-kotlin
   ```

2. Open the project in **Android Studio (Ladybug / Jellyfish or newer)**.

3. Build Debug or Release APK:
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📄 License
This project is licensed under the MIT License.
