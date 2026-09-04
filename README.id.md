<div align="center">
  <img src="docs/assets/app_icon.png" width="100" alt="KZKT Logo" />
  <h1>KZKT</h1>

  <p>
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge&logo=open-source-initiative&logoColor=white" alt="License: MIT"></a>
    <a href="https://github.com/kouzen-neo/kzkt/releases"><img src="https://img.shields.io/github/v/release/kouzen-neo/kzkt?style=for-the-badge&color=teal" alt="Rilis Terbaru"></a>
    <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-API%2026%2B-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white" alt="Android SDK"></a>
    <a href="https://ai.google.dev/edge/litert"><img src="https://img.shields.io/badge/Google_LiteRT-GPU_Accelerated-4285F4.svg?style=for-the-badge&logo=google&logoColor=white" alt="LiteRT"></a>
    <a href="https://opencv.org/"><img src="https://img.shields.io/badge/OpenCV-v4.10.0-5C3EE8.svg?style=for-the-badge&logo=opencv&logoColor=white" alt="OpenCV"></a>
  </p>

  <p>
    <a href="README.md"><img src="https://img.shields.io/badge/EN-6e7681.svg?style=for-the-badge" alt="English"></a>
    <a href="README.id.md"><img src="https://img.shields.io/badge/ID-0078D4.svg?style=for-the-badge" alt="Bahasa Indonesia"></a>
  </p>
</div>

KZKT adalah aplikasi Android untuk menerjemahkan manga, manhwa, manhua, dan komik secara otomatis. Balon kata dideteksi langsung di perangkat menggunakan AI (on-device), diterjemahkan via model LLM pilihan (atau 100% offline via GPU LiteRT), teks asli dihapus dengan rapi, dan hasil terjemahan di-render langsung ke halaman secara natural tanpa memerlukan akses root.

<div align="center">
  <table>
    <tr>
      <td align="center" width="33%"><b>Layar Terjemah</b></td>
      <td align="center" width="33%"><b>Layar Riwayat</b></td>
      <td align="center" width="33%"><b>Layar Pengaturan</b></td>
    </tr>
    <tr>
      <td align="center" width="33%"><img src="docs/screenshots/translate.png" width="100%" alt="Layar Terjemah"></td>
      <td align="center" width="33%"><img src="docs/screenshots/history.png" width="100%" alt="Layar Riwayat"></td>
      <td align="center" width="33%"><img src="docs/screenshots/settings.png" width="100%" alt="Layar Pengaturan"></td>
    </tr>
  </table>
</div>

---

## Fitur & Keunggulan

- **Format Input Lengkap**: Terjemahkan gambar tunggal, folder penuh, seleksi banyak gambar sekaligus, arsip (ZIP / CBZ / EPUB), hingga dokumen PDF — dengan sistem **PDF masuk → PDF terjemahan keluar**.
- **100% Offline AI di Perangkat (Google LiteRT)**: Terjemahkan tanpa internet dan tanpa API Key menggunakan akselerasi native GPU Adreno OpenCL (Gemma 4, Qwen 3, Gemma 3).
- **Dukungan Cloud LLM Luas**: Google Gemini, Anthropic (Claude), OpenAI (GPT), OpenRouter, Zen, OpenCode Go, atau endpoint custom OpenAI-compatible mana pun (Ollama, LM Studio, LocalAI, vLLM).
- **Deteksi Balon Kata YOLO On-Device**: Kaskade ONNX 3 tahap mendeteksi balon dialog lokal secara instan tanpa mengunggah gambar ke server luar.
- **Multi-Script Local OCR**: Pengenalan teks Google ML Kit untuk Bahasa Jepang, Latin, Korea, dan Mandarin dengan kemampuan deteksi area teks bebas.
- **Tipografi Cerdas & Inpainting**: Inpainting OpenCV multi-core tanpa jeda, pembungkusan teks kurva elips diamond, perataan vertikal optik, dan pemenggalan suku kata otomatis.
- **Reader & Editor Touch-up Bawaan**: Baca hasil per halaman atau scroll vertikal ala webtoon, dan ketuk balon kata mana pun untuk mengedit teks secara langsung.
- **Pembaca PDF Instan**: Membuka dokumen PDF terjemahan berukuran besar secara lazy-load per halaman tanpa waktu tunggu.
- **Glosarium & Memory Terjemahan**: Kamus istilah kustom untuk menjaga konsistensi nama karakter dan istilah khusus.
- **Background Worker & Auto-Updater**: Proses translasi tetap berjalan di latar belakang via WorkManager, dilengkapi pemeriksa update otomatis dari GitHub Releases.

---

## Download & Panduan Instalasi

Unduh file APK rilis terbaru langsung dari **[GitHub Releases](https://github.com/kouzen-neo/kzkt/releases/latest)**.

### Varian APK mana yang harus saya pilih?

| Varian APK | Perangkat yang Disarankan |
|---|---|
| **`KZKT-arm64-v8a-*.apk`** | **Sangat disarankan untuk HP modern** (Snapdragon, Dimensity, Tensor, Exynos 64-bit). Ukuran file paling hemat (~125 MB). |
| **`KZKT-armeabi-v7a-*.apk`** | Untuk ponsel Android 32-bit lawas. |
| **`KZKT-x86_64-*.apk`** | Emulator Android di PC (LDPlayer, BlueStacks, Waydroid) / ChromeOS. |
| **`KZKT-universal-*.apk`** | Kompatibel dengan semua arsitektur prosesor (ukuran file lebih besar). |

---

## Alur Translasi

```text
[ Gambar / Manga / PDF ]
           │
           ▼
[ 1. Deteksi Balon YOLO (ONNX) ]     ──> Mendeteksi posisi balon kata di perangkat
           │
           ▼
[ 2. OCR Multi-Bahasa (ML Kit) ]     ──> Ekstraksi teks (Jepang, Korea, Mandarin, Latin)
           │
           ▼
[ 3. Translasi LLM ]                 ──> LiteRT (Offline GPU) / Gemini / Claude / GPT
           │
           ▼
[ 4. OpenCV Inpainting & Masking ]   ──> Menghapus teks asli secara bersih
           │
           ▼
[ 5. Dynamic Diamond Text Render ]   ──> Merender teks terjemahan berbalut rapi
           │
           ▼
[ Hasil di /Download/KZKT/ ]         ──> Tersimpan otomatis ke galeri & history
```

---

## Privasi & Keamanan

- **Kredensial Terenkripsi**: Semua API Key dan token disimpan terenkripsi di perangkat menggunakan Android Keystore dan enkripsi perangkat keras AES-GCM.
- **Privasi Total**: Saat menggunakan provider LiteRT, deteksi YOLO, dan OCR lokal, seluruh pemrosesan data berlangsung 100% di perangkat Anda tanpa lalu lintas jaringan.

---

## Lisensi

Proyek ini dilisensikan di bawah [Lisensi MIT](LICENSE).
