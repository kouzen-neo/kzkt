# Changelog

Semua perubahan signifikan pada CyKt dicatat di file ini.

Format mengikuti [Keep a Changelog](https://keepachangelog.com/id-ID/1.1.0/), versi mengikuti [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Pengaturan collapsible**: halaman Settings dirombak menjadi accordion Material — setiap grup (Provider, Target Language, API Keys, Model, Tweak Parameters, SFX Filter Mode) bisa di-hide/show dengan menekan header. Provider dan Target Language terbuka secara default karena paling sering dipakai.
- **Komponen `SettingsSection`** baru: Card Material dengan header clickable, ikon chevron naik/turun, dan animasi buka-tutup. State ekspansi disimpan via `rememberSaveable` sehingga bertahan saat rotasi layar.

### Changed

- **Model & Custom URL digabung**: bagian `Model` sekarang berisi pengaturan base URL custom beserta tombol "Detect Models from API" di dalamnya, tidak lagi terpisah di tengah layar.
- **Toggler API Keys diseragamkan**: tombol `Show/Hide API Keys` yang lama diganti accordion yang konsisten dengan section lain.

### Fixed (terbaru, commit `b275190`)

- **JSON parsing toleran duplicate key**: LLM kadang mengembalikan JSON dengan key duplikat (mis. `"5_1": "..."` muncul 2×). Parser sebelumnya crash total, sekarang pakai fallback `JsonReader` streaming yang *skipValue()* duplikat — batch translasi selamat, halaman tengah tidak kehilangan terjemahan.
- **YOLO init dipindahkan ke background**: inisialisasi model ONNX + dekripsi `eyecypy.dat` kini jalan di `Dispatchers.IO` via `viewModelScope.launch` — main thread tidak diblokir saat startup.

## [v1.0.0] - 2026-07-xx

### Added

- **In-App Fullscreen Image Viewer** dengan gestur pinch-to-zoom & pan (`a6e06d5`).
- **Quick Action buttons** untuk buka hasil terjemahan langsung di System Gallery atau share via sosial media (`a6e06d5`).
- **Official CYPY app launcher icon** di semua kepadatan layar Android (mdpi hingga xxxhdpi) (`bb4d524`).

### Fixed

- **Translation cancellation delay**: menekan Cancel sekarang menghentikan coroutine dan request jaringan instan — tidak lagi nunggu batch selesai (`8470ccb`).
- **Custom LLM remote endpoint compatibility**: memaksa `stream: false` dan menambah parsing JSON dinamis untuk Ollama, LM Studio, vLLM, serta tunnel Cloudflare/Ngrok (`d11b068`).
- **PhotoPicker synthetic path crash pada Android 13+**: routing file output langsung ke publik `/Download/CYPY/` (`51f330c`).
- **OpenCV JNI `JNIEnv` library loading issue pada release build**: menggunakan uncompressed legacy packaging (`51f330c`).
- **JSON parsing errors pada response LLM non-standar**: pakai lenient Gson parsing (`b275190` — perbaikan lebih lanjut di [Unreleased] di atas).
- **Rilis native Mat/ONNX resources**, 3-stage YOLO cascade, bubble-sized overlay (`7dea054`).
- **PDF input/output** via built-in `PdfRenderer`/`PdfDocument`, shared render path (`9911db9`).
- **Tema warna** diganti dari ungu ke Light Blue (Sky Blue) modern (`4c8a657`).

---

*Catatan: Riwayat penuh lihat `git log --oneline`.*