# CyPy Mobile - Native Android Kotlin

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/Android-SDK%2026%2B-green.svg)](https://developer.android.com)

A high-performance, native Android application for automatic manga and comic translation.

> **Fork & Porting Information:**
> This project is a **Native Android Mobile Porting** of the original Python desktop application [indravoyager/cypy](https://github.com/indravoyager/cypy). It has been completely re-engineered from the ground up into a 100% Kotlin native mobile application optimized for Android smartphones and tablets.

---

## Key Features

- **Native Mobile Performance:** Fully built with **Jetpack Compose**, **Kotlin Coroutines**, and **OpenCV C++ Native JNI** for instant UI responsiveness and low memory overhead.
- **YOLO Speech Bubble Detection:** On-device speech bubble detection using **Microsoft ONNX Runtime Android SDK**.
- **Multi-Provider Vision LLM Support:**
  - **Google Gemini** (`gemini-3.1-flash-lite`, etc.)
  - **OpenAI** (`gpt-5.4-mini`, etc.)
  - **OpenRouter** (`qwen2.5-vl-72b-instruct`, etc.)
  - **Zen / OpenCode Go**
  - **Custom / Local AI:** Dynamic Base URL & Auto-Model Detection for **Ollama**, **LM Studio**, **LocalAI**, and **vLLM** (with/without API Key).
- **Smart Mosaic Batching:** Automatically crops and packs speech bubbles into high-density mosaics with vertical RTL (Manga) reading order to cut API costs by up to 80%.
- **Dynamic Text Rendering & Masking:** Automatic bubble masking (rounded rectangle / oval) with adaptive font sizing and word-wrapping (`Canvas` & `StaticLayout`).
- **Public Gallery Storage:** Automatically saves translated pages directly to `/Download/CYPY/` so they instantly appear in your **Android Gallery** and **Google Photos**.
- **Scoped Storage & PhotoPicker Safe:** Fully compatible with Android 10 to Android 15+ (no root or dangerous permissions required).

---

## Technology Stack

| Layer | Technologies & Libraries |
| :--- | :--- |
| **Language & Core** | Kotlin 1.9+, Java 17 |
| **UI Framework** | Jetpack Compose, Material 3, Navigation Compose, Coil Image Loader |
| **Concurrency & State** | Kotlin Coroutines, Flow, ViewModel, DataStore Preferences |
| **Machine Learning** | Microsoft ONNX Runtime Android SDK (`com.microsoft.onnxruntime:onnxruntime-android:1.21.0`) |
| **Computer Vision** | OpenCV 4.10.0 Android SDK (`libopencv_java4.so` C++ JNI) |
| **Networking & JSON** | OkHttp 4.12.0, Gson (Lenient Parsing Mode) |
| **Target Android API** | `minSdk = 26` (Android 8.0), `targetSdk = 36` (Android 15+) |

---

## Architecture & Pipeline Workflow

```text
[ Input Image / Manga Page ]
           |
           v
[ 1. YOLO ONNX Bubble Detection ] --> Detect Bounding Boxes
           |
           v
[ 2. OpenCV Filtering & Smart Crop ] --> Filter Giant Boxes, SFX, & Merge Overlaps
           |
           v
[ 3. Mosaic Builder ] --> Pack Crops into Vertical RTL Mosaic + Red ID Labels
           |
           v
[ 4. Vision LLM Provider API ] --> Gemini / OpenAI / Custom Local LLM
           |
           v
[ 5. Text Renderer & Masking ] --> Draw In-Bubble Mask + Auto-scaled Wrapped Text
           |
           v
[ Output Image in /Download/CYPY/ ] --> Instant Access in Android Gallery
```

---

## Project Structure

```text
├── app/
│   ├── src/main/
│   │   ├── java/com/cypy/app/
│   │   │   ├── core/           # Translation Pipeline, YOLO ONNX Engine, OpenCV, TextRenderer
│   │   │   ├── core/providers/ # Gemini, OpenAI, Custom LLM Provider Implementations
│   │   │   ├── data/           # SettingsRepository (DataStore Persistence)
│   │   │   ├── ui/             # Jetpack Compose Screens (MainScreen, SettingsScreen) & MainViewModel
│   │   │   └── util/           # JsonUtils & Helper utilities
│   │   ├── assets/             # Encrypted YOLO ONNX model (eyecypy.dat) & Custom Fonts
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── opencv/                      # OpenCV 4.x Android SDK Module (Native JNI)
├── build.gradle.kts             # Root Gradle build script
├── settings.gradle.kts          # Root settings script
└── README.md
```

---

## How to Build the APK

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/kouzen-neo/cypy-mobile-native-kotlin.git
   cd cypy-mobile-native-kotlin
   ```

2. **Open in Android Studio:**
   - Open Android Studio (Ladybug, Jellyfish, or newer).
   - Allow Gradle to sync dependencies.

3. **Build Debug or Release APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Credits & Acknowledgments
- Original Python Application by [indravoyager/cypy](https://github.com/indravoyager/cypy).
- Native Android Kotlin Porting by [kouzen-neo](https://github.com/kouzen-neo).

---

## License
This project is licensed under the [MIT License](LICENSE).
