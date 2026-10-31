# gotube-plus
# GoTube Plus

Aplikasi pemutar media Android native untuk Android 6.0 (API 23) ke atas. UI menggunakan Kotlin, Jetpack Compose, dan Material 3; playback dikelola AndroidX Media3.

## Build

Persyaratan: JDK 17 dan Android SDK dengan platform 36 serta Android Build Tools 35.0.0 atau lebih baru.

```bash
./gradlew assembleDebug
```

APK debug dibuat di `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions membangun APK pada push, pull request, atau workflow dispatch dan mengunggahnya sebagai artifact `gotube-plus-debug-apk`.
APK debug dibuat di `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions membangun APK pada push, pull request, atau workflow dispatch dan mengunggah artifact bernama `gotube-plus-v1.0.0-<nomor-run>` selama 30 hari. Buka workflow run yang selesai, lalu unduh file dari bagian **Artifacts** atau tautan yang ditampilkan di ringkasan run.
APK debug dibuat di `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions membangun APK pada push, pull request, atau workflow dispatch dan mengunggah artifact bernama `gotube-plus-v1.0.0-<nomor-run>` selama 30 hari. Ringkasan workflow run menyediakan tautan langsung untuk mengunduh APK.

## Fitur

- Beranda, pencarian teks dan suara `id-ID`, daftar video, favorit, riwayat, dan mode gelap.
- Playback video Media3 dengan play/pause, volume, orientasi layar penuh, antrean next, dan Picture-in-Picture pada Android 8.0+.
- `MediaSessionService` mempertahankan playback saat Activity ditutup atau pengguna berpindah aplikasi. Media3 menangani audio focus; headset yang dilepas ditangani sebagai audio becoming noisy.
- Posisi media dan status putar terakhir disimpan lokal untuk pemulihan saat service dibuat kembali.
- Tidak ada iklan, SDK iklan, atau pop-up promosi.

## Sumber Media dan YouTube

Katalog bawaan berisi URL video sampel publik, bukan konten YouTube. Pencarian saat ini mencari katalog lokal tersebut. Integrasi YouTube Data API memerlukan API key dan belum dipasang; API tersebut hanya menyediakan metadata pencarian, bukan stream pemutaran. Tidak ada ekstraksi stream atau bypass iklan/pembatasan YouTube. Pemutaran latar belakang hanya untuk sumber langsung yang mengizinkannya. Kualitas mengikuti file sumber; kualitas adaptif memerlukan sumber yang menyediakannya.

## Batas Service Android

Playback yang sedang aktif memakai foreground service bertipe `mediaPlayback` dan notifikasi sesi media. Android masih dapat menghentikan proses karena force-stop, penghentian dari pengelola aplikasi aktif, tekanan memori ekstrem, atau kebijakan baterai/pengelolaan proses dari vendor perangkat. Pengguna juga dapat menghentikan playback melalui kontrol media sistem. Pemulihan posisi berlaku ketika service dibuat kembali, tetapi tidak menjamin pemutaran otomatis setelah force-stop atau reboot.
