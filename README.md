# GoTube Plus

Aplikasi pemutar YouTube dan media langsung untuk Android 6.0 (API 23) ke atas. Dibuat dengan Kotlin, Jetpack Compose, Material 3, AndroidX Media3, dan Preferences DataStore.

## Unduh APK

Unduh APK debug langsung dari [GoTubePlus-v1.0.0.apk](GoTubePlus-v1.0.0.apk), atau dari [GitHub Release](https://github.com/patrissiuspaulus-ui/gotube-plus/releases/tag/v1.0.0). APK dapat dipasang untuk pengujian; Android mungkin meminta izin memasang aplikasi dari sumber ini.

## Konfigurasi YouTube API

1. Buat project Google Cloud gratis, aktifkan **YouTube Data API v3**, lalu buat API key.
2. Salin `secrets.properties.example` menjadi `secrets.properties` dan isi `YOUTUBE_API_KEY=...`. File rahasia ini diabaikan Git.
3. Untuk GitHub Actions, simpan key sebagai repository secret bernama `YOUTUBE_API_KEY`. Build tetap berjalan tanpa secret, tetapi pencarian akan memakai katalog demo lokal.
4. Batasi key pada YouTube Data API v3 dan pantau kuota. `search.list` menggunakan kuota API; ketika kuota harian habis aplikasi menampilkan pesan dan pencarian dapat dicoba lagi setelah reset kuota.

Key yang dimasukkan ke aplikasi klien dapat diekstrak dari APK. Jangan memakai key dengan akses layanan lain atau billing sensitif; batasi API dan kuota di Google Cloud. Hindari mengunggah APK berisi key privat ke publik.

## Build

Persyaratan: JDK 17 dan Android SDK platform 36.

```bash
./gradlew assembleDebug
```

APK dibuat di `app/build/outputs/apk/debug/app-debug.apk`. Workflow GitHub Actions berjalan pada push ke `main`/`master`, pull request, atau `workflow_dispatch`; job menjalankan lint dan build dengan JDK 17, lalu mengunggah artifact selama 30 hari.

## Fitur Tersedia

- Pencarian YouTube Data API v3 di aplikasi, kategori, pagination, bahasa/region Indonesia, dan input suara `id-ID`.
- Pemutar YouTube resmi IFrame di dalam aplikasi, penanganan video yang menolak embedding, serta tombol untuk membuka aplikasi YouTube.
- Katalog video demo langsung untuk mencoba Media3 tanpa API key.
- Favorit dan riwayat video lengkap disimpan lokal dengan Preferences DataStore; riwayat bisa dihapus.
- Untuk URL media langsung yang mengizinkan background: MediaSessionService, notifikasi media, audio focus, headset becoming noisy, serta pemulihan posisi sesi.
- Mode gelap, navigasi bawah, Picture-in-Picture untuk pemutar Media3, dan tanpa iklan aplikasi/SDK iklan.

## Batas Pemutaran

Data API hanya memberi metadata pencarian, bukan stream video. Video YouTube diputar lewat IFrame resmi dan mengikuti iklan, batas embedding, serta kebijakan YouTube; aplikasi tidak mengekstrak stream video dari YouTube. Bila video tidak mengizinkan embed, aplikasi otomatis membuka YouTube di aplikasi resmi.

### Catatan penting

- Jika video tidak diputar di WebView, kemungkinan karena pembuat video membatasi embedding atau YouTube menolak referer/origin dari WebView Android.
- Solusi yang aman adalah coba putar tersemat dulu, lalu buka video di aplikasi YouTube jika gagal.
- Semua file video dari sumber langsung tetap diputar oleh Media3, bukan lewat YouTube API.
































































































































































































"}]}   