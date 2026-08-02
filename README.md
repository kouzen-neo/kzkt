# KZKT

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-purple.svg)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/Android-SDK%2026%2B-green.svg)](https://developer.android.com)

KZKT is a native Android application for automatic manga and comic translation. It detects speech bubbles on-page with on-device AI, sends the text to the vision LLM of your choice, and renders the translated text back into the page — all locally, with no root access required.

---

## Highlights

- **On-device bubble detection** — YOLO via Microsoft ONNX Runtime Android, with a 3-stage cascade for accurate detection of speech bubbles, SFX, and overlapping boxes.
- **Multi-provider vision LLM** — Google Gemini, OpenAI, OpenRouter, Zen, OpenCode Go, plus any custom or local endpoint (Ollama, LM Studio, LocalAI, vLLM) with automatic model detection.
- **Cost-efficient mosaic batching** — speech bubbles are cropped and packed into vertical RTL mosaics to cut API requests by up to 80%.
- **PDF in, PDF out** — render PDF pages to images and reassemble translated pages back into PDF using only the built-in Android `PdfRenderer` and `PdfDocument`.
- **Resilient JSON parsing** — tolerates duplicate keys and malformed LLM output so a single bad response never aborts a batch.
- **Adaptive text rendering** — automatic bubble masking with rounded or oval shapes, font auto-scaling, and word wrapping via `Canvas` and `StaticLayout`.
- **Gallery-ready output** — translated pages are saved to `/Download/KZKT/` and appear instantly in your gallery and Google Photos.
- **Modern, safe storage** — built for Android 10 through Android 15+, scoped storage only; no dangerous permissions, no root.

---

## Technology Stack

| Layer | Technologies |
| :--- | :--- |
| Language & Core | Kotlin, Java 17 |
| UI | Jetpack Compose, Material 3, Navigation Compose, Coil |
| Concurrency & State | Coroutines, Flow, ViewModel, DataStore Preferences |
| Machine Learning | ONNX Runtime Android (`com.microsoft.onnxruntime:onnxruntime-android:1.21.0`) |
| Computer Vision | OpenCV 4.10.0 Android SDK (`libopencv_java4.so` C++ JNI) |
| Networking & JSON | OkHttp 4.12, Gson (lenient mode) |
| Target API | `minSdk = 26` (Android 8.0), `targetSdk = 36` (Android 15+) |

---

## Pipeline

```text
[ Input image / manga page ]
           |
           v
[ 1. YOLO ONNX bubble detection ] --> bounding boxes
           |
           v
[ 2. OpenCV filtering & smart crop ] --> remove SFX, merge overlaps
           |
           v
[ 3. Mosaic builder ] --> pack crops into vertical RTL mosaic + red ID labels
           |
           v
[ 4. Vision LLM provider ] --> Gemini / OpenAI / OpenRouter / custom local
           |
           v
[ 5. Text renderer & masking ] --> in-bubble mask + auto-scaled wrapped text
           |
           v
[ Output in /Download/KZKT/ ] --> visible in gallery instantly
```

---

## Project Structure

```text
├── app/
│   └── src/main/
│       ├── java/com/kzkt/app/
│       │   ├── core/           # pipeline, YOLO ONNX engine, OpenCV, text renderer
│       │   ├── core/providers/ # Gemini, OpenAI, OpenRouter, custom providers
│       │   ├── data/           # settings & history (DataStore persistence)
│       │   ├── ui/             # Compose screens (Translate, History, Settings)
│       │   ├── ui/component/   # reusable Material 3 components
│       │   └── util/           # helpers
│       ├── assets/             # encrypted YOLO model (kzkt.dat) & fonts
│       └── AndroidManifest.xml
├── opencv/                     # OpenCV 4.x Android SDK module (native JNI)
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Build the APK

1. Clone the repository:
   ```bash
   git clone https://github.com/kouzen-neo/kzkt.git
   cd kzkt
   ```

2. Open in Android Studio, let Gradle sync, then build:
   ```bash
   ./gradlew assembleDebug
   ```

3. Install the APK:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## License

This project is licensed under the [MIT License](LICENSE).

---

## Credits

- Original application: [indravoyager/cypy](https://github.com/indravoyager/cypy)
- Native Android rewrite: [kouzen-neo](https://github.com/kouzen-neo)
