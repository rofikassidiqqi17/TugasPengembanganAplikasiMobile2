# News Feed Simulator - Tugas 2 Praktikum PAM (Pengembangan Aplikasi Mobile)

Aplikasi simulasi umpan berita (*News Feed Simulator*) berbasis Android modern yang dibangun menggunakan bahasa pemrograman **Kotlin**, **Kotlin Coroutines**, **Asynchronous Flow**, **StateFlow**, dan **Jetpack Compose (Material 3)**.

Proyek ini dikembangkan untuk memenuhi ketentuan **Tugas Praktikum 2 - Pengembangan Aplikasi Mobile (ITERA)** dengan topik *Coroutines & Flow*.

---

## 👨‍💻 Identitas Mahasiswa
- **Nama** : [Nama Lengkap Anda]
- **NIM** : [NIM Anda]
- **Mata Kuliah** : Pengembangan Aplikasi Mobile (PAM)
- **Institusi** : Institut Teknologi Sumatera (ITERA)

---

## 🚀 Fitur Utama & Implementasi

Proyek ini telah mengimplementasikan ke-5 poin spesifikasi tugas secara lengkap:

| No | Kebutuhan Tugas | Implementasi Kode | Lokasi File |
|---|---|---|---|
| **1** | **Flow Simulasi Berita Baru Setiap 2 Detik** | Menggunakan fungsi `flow { ... }` dengan `while(isActive)` dan `delay(2000L)` untuk memancarkan artikel berita baru secara berkala. | [`NewsViewModel.kt`](file:///C:/Users/LENOVO/AndroidStudioProjects/PAMTugas2/app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) (`getRawNewsStream()`) |
| **2** | **Filter Berita Berdasarkan Kategori** | Menggunakan operator Flow `.filter { news -> ... }` untuk menyaring berita berdasarkan kategori yang dipilih pengguna (Semua, Teknologi, Ekonomi, Sukan, Semasa). | [`NewsViewModel.kt`](file:///C:/Users/LENOVO/AndroidStudioProjects/PAMTugas2/app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) (`getProcessedNews()`) |
| **3** | **Transform Data Menjadi Format Tampilan** | Menggunakan operator Flow `.map { news -> ... }` untuk mentransformasi objek mentah `News` menjadi `FormattedNews` (judul terformat berlabel kategori `[KATEGORI]`, cuplikan ringkasan, dan penanda waktu WIB). | [`NewsViewModel.kt`](file:///C:/Users/LENOVO/AndroidStudioProjects/PAMTugas2/app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) (`transformToFormattedNews()`) |
| **4** | **StateFlow untuk Jumlah Berita Dibaca** | Menggunakan `_readCount = MutableStateFlow(0)` dan `readCount: StateFlow<Int>` untuk melacak jumlah berita unik yang telah dibaca. Nilai ditampilkan secara reaktif pada TopAppBar dan Logcat. | [`NewsViewModel.kt`](file:///C:/Users/LENOVO/AndroidStudioProjects/PAMTugas2/app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) & [`NewsScreen.kt`](file:///C:/Users/LENOVO/AndroidStudioProjects/PAMTugas2/app/src/main/java/com/example/pamtugas2/NewsScreen.kt) |
| **5** | **Coroutines untuk Ambil Detail Berita Async** | Fungsi `suspend fun fetchNewsDetailAsync(newsId: Int): String` yang berjalan secara asynchronous pada `withContext(Dispatchers.IO)` dengan simulasi delay jaringan 1000ms. Menampilkan loading state di UI dialog sebelum konten artikel lengkap disajikan. | [`NewsViewModel.kt`](file:///C:/Users/LENOVO/AndroidStudioProjects/PAMTugas2/app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) (`fetchNewsDetailAsync()`) |

---

## 📱 Tampilan Antarmuka (Jetpack Compose)

Aplikasi dilengkapi antarmuka interaktif:
1. **Top Bar Dinamis**: Menampilkan judul aplikasi dan counter badge jumlah berita yang sudah dibaca yang terhubung langsung ke `StateFlow`.
2. **Filter Kategori (Chips)**: Pilihan kategori interaktif (*Semua, Teknologi, Ekonomi, Sukan, Semasa*) yang langsung memfilter aliran berita menggunakan operator Flow `.filter`.
3. **Kontrol Simulasi**: Tombol *Jeda / Lanjut* untuk mengontrol aliran *streaming* coroutine dan tombol *Hapus* untuk membersihkan daftar.
4. **Kartu Berita (News Cards)**: Daftar kartu berita dengan animasi rapi, penanda badge kategori warna-warni, format waktu, dan status baca.
5. **Dialog Detail Asinkron**: Menampilkan indikator loading saat coroutine sedang mengambil data dari IO Dispatcher secara asinkron, lalu menampilkan isi artikel lengkap serta otomatis menandai berita telah dibaca.

---

## 🛠️ Arsitektur & Teknologi

- **Bahasa**: Kotlin 2.2+
- **Min SDK**: 24 (Android 7.0+)
- **Target / Compile SDK**: 37 (Android 15+)
- **UI Framework**: Jetpack Compose (Material 3)
- **Arsitektur**: Model-View-ViewModel (MVVM)
- **Asynchronous / Reactive**: Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Main`, `viewModelScope`, `lifecycleScope`) & Kotlin Flow (`Flow`, `filter`, `map`, `StateFlow`, `collectAsState`)
- **Unit Testing**: JUnit 4 & `kotlinx-coroutines-test`

---

## 📂 Struktur Direktori Proyek

```
app/src/
├── main/
│   ├── java/com/example/pamtugas2/
│   │   ├── MainActivity.kt      # Activity utama (Compose setContent & Logcat collector)
│   │   ├── News.kt              # Data model (News, FormattedNews, NewsDetailState, Dataset)
│   │   ├── NewsViewModel.kt     # ViewModel (Flow producer, filter, map, StateFlow, async coroutine)
│   │   ├── NewsScreen.kt        # Jetpack Compose UI (TopBar, Chips, Cards, Detail Dialog)
│   │   └── ui/theme/            # Konfigurasi Tema (Color, Theme, Type)
│   ├── res/                     # Asset XML, ikon launcher, dan values
│   └── AndroidManifest.xml
└── test/
    └── java/com/example/pamtugas2/
        └── NewsViewModelTest.kt # Unit test lengkap untuk memverifikasi 5 fitur tugas
```

---

## 💻 Cara Menjalankan Proyek

### 1. Menjalankan Melalui Android Studio
1. Buka aplikasi **Android Studio**.
2. Pilih **File > Open**, lalu arahkan ke folder proyek ini (`PAMTugas2`).
3. Tunggu hingga proses **Gradle Sync** selesai secara otomatis.
4. Hubungkan perangkat fisik Android via USB Debugging atau jalankan **Android Emulator (AVD)**.
5. Klik tombol **Run 'app'** (tombol ikon segitiga hijau ▶ atau kombinasi tombol `Shift + F10`).
6. Aplikasi akan terinstal dan berjalan pada perangkat/emulator.

### 2. Menjalankan Pengujian Unit Test (Verifikasi 5 Fitur)
Untuk memastikan seluruh logika *Coroutines*, *Flow*, *filter*, *map*, dan *StateFlow* berfungsi dengan benar, jalankan perintah berikut di terminal:

```bash
# Di Command Prompt / PowerShell Windows
.\gradlew.bat testDebugUnitTest
```
Semua 5 skenario uji pada [`NewsViewModelTest.kt`](file:///C:/Users/LENOVO/AndroidStudioProjects/PAMTugas2/app/src/test/java/com/example/pamtugas2/NewsViewModelTest.kt) akan dieksekusi dengan status **SUCCESS**.

### 3. Membangun Berkas APK Debug
```bash
.\gradlew.bat assembleDebug
```
Berkas APK akan dihasilkan pada folder:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📋 Verifikasi Aliran Data pada Logcat

Selain pada tampilan visual UI, aplikasi juga mencetak alur simulasi secara otomatis ke **Logcat** dengan tag `NewsApp`:

```log
D/NewsApp: JUMLAH DIBACA: 0 berita
D/NewsApp: Memulakan suapan berita untuk kategori Teknologi...
D/NewsApp: BERITA MASUK: [TEKNOLOGI] Peluncuran Chipset Snapdragon Generasi Terbaru (22:45:10 WIB)
D/NewsApp: Pengguna klik berita ID 2. Memuat turun detail...
D/NewsApp: BUTIRAN LENGKAP:
Detail Artikel #2:
Judul   : Peluncuran Chipset Snapdragon Generasi Terbaru
Kategori: Teknologi
Penulis : Budi Santoso
Waktu   : Jumat, 25 September 2026 22:45:15 WIB
...
D/NewsApp: JUMLAH DIBACA: 1 berita
```

---

## 📤 Petunjuk Pengunggahan ke GitHub

1. Buka terminal di direktori proyek:
   ```bash
   git init
   git add .
   git commit -m "feat: Implementasi lengkap News Feed Simulator Tugas 2 PAM ITERA"
   ```
2. Buat repositori baru di akun GitHub Anda (misal `PAM-Tugas2`).
3. Hubungkan repositori lokal ke GitHub:
   ```bash
   git branch -M main
   git remote add origin https://github.com/<username-anda>/<nama-repo>.git
   git push -u origin main
   ```
4. Salin URL repositori GitHub tersebut dan kumpulkan pada submission LMS ITERA.
