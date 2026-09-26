# 📰 News Feed Simulator — Tugas 2 Praktikum PAM

Aplikasi **simulasi umpan berita (*News Feed Simulator*)** yang menampilkan berita baru secara otomatis dari sebuah **asynchronous data stream**. Dibangun sepenuhnya menggunakan **Kotlin** dengan memanfaatkan **Kotlin Coroutines**, **Asynchronous Flow**, **StateFlow**, dan **Jetpack Compose (Material 3)**.

Proyek ini dibuat untuk memenuhi ketentuan **Tugas Praktikum 2 — Pengembangan Aplikasi Mobile (PAM)**, Institut Teknologi Sumatera (ITERA), dengan topik utama **Coroutines & Flow**.

> **Ringkasan:** 5 dari 5 poin spesifikasi tugas terimplementasi, **6/6 unit test lulus**, aplikasi sudah teruji jalan di perangkat nyata (Infinix X6853, Android 16) melalui **USB Debugging**.

---

## 👨‍💻 Identitas Mahasiswa

| Field | Keterangan |
|---|---|
| Nama | `ISI NAMA LENGKAP ANDA DI SINI` |
| NIM | `ISI NIM ANDA DI SINI` |
| Mata Kuliah | Pengembangan Aplikasi Mobile (PAM) |
| Kelompok | `ISI KELOMPOK ANDA` |
| Institusi | Institut Teknologi Sumatera (ITERA) |
| Dosen Pengampu | `ISI NAMA DOSEN ANDA` |

> ⚠️ **Wajib diisi sebelum submit.** Ganti semua teks `ISI ...` di tabel ini.

---

## 📌 Ringkasan 5 Fitur Tugas

| No | Kebutuhan Tugas | Fitur Terlihat di HP |
|---|---|---|
| **1** | Flow mensimulasikan data berita baru setiap 2 detik | Berita baru otomatis turun tiap 2 detik |
| **2** | Filter berita berdasarkan kategori tertentu | Chip kategori: Semua, Teknologi, Ekonomi, Sukan, Semasa |
| **3** | Transform data menjadi format yang ditampilkan | Judul berformat `[KATEGORI] Judul Berita` + waktu WIB |
| **4** | StateFlow menyimpan jumlah berita yang sudah dibaca | Badge `📖 Dibaca: N` di TopBar, warna kartu jadi redup |
| **5** | Coroutines mengambil detail berita secara async | Dialog loading spinner 1 detik → isi artikel lengkap |

---

## 🚀 Implementasi Kode per Fitur

### FITUR 1 — Flow yang mensimulasikan data berita baru setiap 2 detik

Menggunakan **cold Flow** dengan `flow { }`, perulangan `while (isActive)`, dan `delay(2000L)`.

📄 [`NewsViewModel.kt`](app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) → `getRawNewsStream()`

```kotlin
fun getRawNewsStream(): Flow<News> = flow {
    var templateIndex = 0
    while (currentCoroutineContext().isActive) {
        val template = realNewsData[templateIndex % realNewsData.size]
        templateIndex++
        val simulatedNews = template.copy(
            id = newsIdCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis()
        )
        emit(simulatedNews)
        delay(2000L)                        // jeda 2 detik simulasi berita masuk
    }
}
```

**Kenapa `while (isActive)`?** Agar coroutine otomatis berhenti saat `viewModelScope` dibatalkan — tidak menyebabkan *memory leak* saat aplikasi ditutup.

**Kenapa `AtomicInteger` untuk ID?** Supaya penomoran berita bersifat global dan monoton. Kalau counter di-reset tiap stream dimulai ulang, ID berita bisa terulang sehingga berita lama ikut ditandai "sudah dibaca".

---

### FITUR 2 — Filter berita berdasarkan kategori tertentu

Menggunakan operator **`filter`**. Kategori `Semua` meloloskan semua berita, selain itu hanya yang cocok.

📄 [`NewsViewModel.kt`](app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) → `getProcessedNews()`

```kotlin
fun getProcessedNews(category: String): Flow<FormattedNews> {
    return getRawNewsStream()
        .filter { news ->                                    // ← FITUR 2
            if (category.equals("Semua", ignoreCase = true)) {
                true
            } else {
                news.category.equals(category, ignoreCase = true)
            }
        }
        .map { news -> transformToFormattedNews(news) }      // ← FITUR 3
}
```

> **Catatan:** `ignoreCase = true` dipakai agar pencocokan kategori tidak bergantung pada huruf besar/kecil.

---

### FITUR 3 — Transform data menjadi format yang ditampilkan

Menggunakan operator **`map`** untuk mengubah model mentah `News` menjadi `FormattedNews` yang siap tampil.

📄 [`NewsViewModel.kt`](app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) → `transformToFormattedNews()`

```kotlin
private fun transformToFormattedNews(news: News): FormattedNews {
    val locale = Locale.forLanguageTag("id-ID")
    val publishDate = Date(news.timestamp)
    val timeFormatter = SimpleDateFormat("HH:mm:ss", locale)
    val dateFormatter = SimpleDateFormat("EEEE, dd MMMM yyyy", locale)

    return FormattedNews(
        id = news.id,
        originalTitle   = news.title,
        formattedTitle  = "[${news.category.uppercase(locale)}] ${news.title}",  // transformasi judul
        category        = news.category,
        previewContent  = if (news.content.length > 80) news.content.take(80) + "..." else news.content,
        formattedTime   = "${timeFormatter.format(publishDate)} WIB",             // transformasi waktu
        author          = news.author,
        publishedDate   = dateFormatter.format(publishDate),
        fullContent     = news.content,
        isRead          = _readNewsIds.value.contains(news.id)
    )
}
```

**Transformasi yang dilakukan:**
- Judul diberi label kategori → `[TEKNOLOGI] Peluncuran Chipset Snapdragon...`
- Cuplikan konten dipotong maksimal 80 karakter + `...`
- Timestamp di-format menjadi jam lokal `HH:mm:ss WIB`
- Tanggal lengkap di-format `EEEE, dd MMMM yyyy` (locale Indonesia)

---

### FITUR 4 — StateFlow untuk menyimpan jumlah berita yang sudah dibaca

Menggunakan **`MutableStateFlow`** sebagai sumber data, di-*expose* sebagai read-only `StateFlow`.

📄 [`NewsViewModel.kt`](app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) & [`NewsScreen.kt`](app/src/main/java/com/example/pamtugas2/NewsScreen.kt)

```kotlin
// Di ViewModel — sumber kebenaran
private val _readCount = MutableStateFlow(0)
val readCount: StateFlow<Int> = _readCount.asStateFlow()      // read-only untuk UI

// Mencatat ID berita agar tidak double count
private val _readNewsIds = MutableStateFlow<Set<Int>>(emptySet())

fun markAsRead(newsId: Int? = null) {
    if (newsId != null) {
        if (!_readNewsIds.value.contains(newsId)) {          // cegah duplikat
            _readNewsIds.value = _readNewsIds.value + newsId
            _readCount.value = _readNewsIds.value.size
            _newsFeedList.value = _newsFeedList.value.map { item ->
                if (item.id == newsId) item.copy(isRead = true) else item
            }
        }
    } else {
        _readCount.value += 1
    }
}
```

```kotlin
// Di UI — consumption reaktif
val readCount by viewModel.readCount.collectAsState()
```

> **Kenapa `MutableStateFlow` + `asStateFlow()` (bukan `var` biasa)?**
> 1. **Type-safe:** UI hanya bisa membaca, tidak bisa mengubah nilai dari luar.
> 2. **Otomatis reaktif:** setiap perubahan langsung memicu UI tanpa `setOnClickListener` manual.
> 3. **Punya nilai awal** (`0`), jadi UI tidak pernah `null`.
> 4. **Cepat:** `StateFlow` berbasis `MutableStateFlow` yang thread-safe dan mendukung *conflation* (hanya mengiru state terbaru).

---

### FITUR 5 — Coroutines untuk mengambil detail berita secara async

Fungsi **`suspend`** yang berjalan di **`Dispatchers.IO`** dengan simulasi latency jaringan 1000 ms.

📄 [`NewsViewModel.kt`](app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) → `fetchNewsDetailAsync()`

```kotlin
suspend fun fetchNewsDetailAsync(newsId: Int): String = withContext(Dispatchers.IO) {
    delay(1000L)                        // simulasi network latency 1 detik

    val displayed = _newsFeedList.value.firstOrNull { it.id == newsId }
    val fallback  = realNewsData[Math.floorMod(newsId - 1, realNewsData.size)]

    val title = displayed?.originalTitle ?: fallback.title
    // ...
    """
    |Detail Artikel #${newsId}:
    |Judul   : ${title}
    |Kategori: ${category}
    |Penulis : ${author}
    |Waktu   : ${publishedDate} WIB
    |
    |Konten Berita Lengkap:
    |${body}
    """.trimMargin()
}
```

**Kenapa `withContext(Dispatchers.IO)`?**
`Dispatchers.IO` adalah dispatcher yang bersifat *I/O-bound* (menunggu jaringan/disk). Karena `delay(1000L)` adalah operasi yang tidak memblokir thread, dengan memindahkannya ke `Dispatchers.IO` **UI thread tetap responsif** dan spinner di dialog bisa berputar dengan halus.

**Kenapa `suspend` + `launch`, bukan `runBlocking`?**
`runBlocking` akan **memblokir thread** sampai selesai — kalau dipakai di MainThread, aplikasi akan freeze. Dengan `suspend`, coroutine hanya "menunggu" tanpa memblokir thread apa pun.

📄 Dialog async + pembaruan StateFlow: [`NewsViewModel.kt`](app/src/main/java/com/example/pamtugas2/NewsViewModel.kt) → `openNewsDetail()`

```kotlin
fun openNewsDetail(news: FormattedNews) {
    viewModelScope.launch {                             // dispatcher otomatis Main
        _detailState.value = NewsDetailState(isOpen = true, isLoading = true, /* ... */)

        val fullContent = fetchNewsDetailAsync(news.id)   // pindah ke IO dispatcher

        _detailState.value = _detailState.value.copy(isLoading = false, detailContent = fullContent)
        markAsRead(news.id)                               // Feature 4 ikut ter-update
    }
}
```

---

## 📱 Tampilan Antarmuka (Jetpack Compose)

Aplikasi dilengkapi antarmuka interaktif dan reaktif:

1. **Top Bar Dinamis** — menampilkan judul aplikasi dan badge jumlah berita yang sudah dibaca, terhubung langsung ke `StateFlow`.
2. **Filter Kategori (Chips)** — pilihan *Semua, Teknologi, Ekonomi, Sukan, Semasa* yang langsung memfilter aliran berita memakai operator `.filter`.
3. **Indikator Status Flow** — titik hijau "Flow aktif (setiap 2 detik)" atau oranye "Flow dijeda".
4. **Kontrol Simulasi** — tombol *Jeda / Lanjut* untuk mengontrol aliran streaming coroutine, dan tombol *Hapus* untuk membersihkan daftar.
5. **Kartu Berita (News Cards)** — badge kategori berwarna-warni, judul terformat, cuplikan konten, waktu WIB, nama penulis, dan status `Baru` / `✓ Dibaca`.
6. **Dialog Detail Asinkron** — menampilkan spinner selama coroutine mengambil data dari `Dispatchers.IO`, lalu menampilkan isi artikel lengkap dan otomatis menandai berita sebagai telah dibaca.

---

## 🛠️ Arsitektur & Teknologi

### Arsitektur
Model-View-ViewModel (**MVVM**) — pemisahan concerns agar logika bisnis (Flow/Coroutines) terpisah dari tampilan.

```
+---------------------------------------------------------------+
|  VIEW        NewsScreen.kt  (Jetpack Compose)                 |
|              collectAsState() -> menggambar UI                |
+-------------------------------+-------------------------------+
                                |  StateFlow (read-only)
+-------------------------------v-------------------------------+
|  VIEWMODEL   NewsViewModel.kt                                  |
|                flow {}     -> producer berita     (Fitur 1)   |
|                filter {}   -> filter kategori     (Fitur 2)   |
|                map {}      -> transformasi data  (Fitur 3)   |
|                StateFlow   -> jumlah berita baca (Fitur 4)   |
|                suspend+IO  -> detail async        (Fitur 5)   |
+-------------------------------+-------------------------------+
                                |
+-------------------------------v-------------------------------+
|  MODEL       News.kt   (News, FormattedNews, Dataset)         |
+---------------------------------------------------------------+
```

### Tumpukan Teknologi

| Komponen | Versi / Keterangan |
|---|---|
| Bahasa | Kotlin 2.2.10 |
| Android Gradle Plugin | 9.4.1 |
| Gradle Wrapper | 9.6.0 |
| JDK (Toolchain) | 25 |
| Min SDK | 24 (Android 7.0+) |
| Target / Compile SDK | 37 |
| UI Framework | Jetpack Compose (Material 3) |
| Compose BOM | 2026.02.01 |
| Async / Reactive | Kotlin Coroutines 1.10.2 (`Dispatchers.IO`, `Dispatchers.Main`, `viewModelScope`, `lifecycleScope`) & Kotlin Flow (`Flow`, `filter`, `map`, `StateFlow`, `collectAsState`) |
| Arsitektur | MVVM |
| Unit Testing | JUnit 4 + `kotlinx-coroutines-test` |
| Build & Deploy | Gradle, adb, USB Debugging, scrcpy |

---

## 📂 Struktur Direktori Proyek

```
PAM-Tugas2/
├── app/
│   ├── build.gradle.kts              # Konfigurasi modul: SDK, Compose, dependency
│   └── src/
│       ├── main/
│       │   ├── java/com/example/pamtugas2/
│       │   │   ├── MainActivity.kt        # Entry point: setContent + collector Logcat
│       │   │   ├── News.kt                # Model data (News, FormattedNews, NewsDetailState, dataset)
│       │   │   ├── NewsViewModel.kt       # ★ Ototanggal: Flow, filter, map, StateFlow, async
│       │   │   ├── NewsScreen.kt          # ★ Tampilan Jetpack Compose
│       │   │   └── ui/theme/
│       │   │       ├── Color.kt           # Palet warna
│       │   │       ├── Theme.kt           # Definisi tema aplikasi
│       │   │       └── Type.kt            # Tipografi
│       │   ├── res/                       # Ikon launcher, strings, themes
│       │   └── AndroidManifest.xml
│       └── test/
│           └── java/com/example/pamtugas2/
│               ├── NewsViewModelTest.kt   # 5 unit test fitur tugas
│               └── ExampleUnitTest.kt
├── gradle/
│   ├── libs.versions.toml                # Version Catalog (satu tempat versi)
│   └── wrapper/                           # Gradle Wrapper
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── install-usb.ps1                        # Skrip otomatis: test → build → install → jalankan → logcat
└── README.md
```

---

## 💻 Cara Menjalankan Proyek

### Prasyarat
- **Android Studio** (ter terbaru) sudah terinstall
- **JDK 17+** (proyek ini memakai toolchain JDK 25)
- **Android SDK Platform-Tools** (termasuk `adb`)
- Perangkat Android **API 24+** atau **Android Emulator**

---

### 1️⃣ Menjalankan Melalui Android Studio

1. Buka aplikasi **Android Studio**.
2. Pilih **File > Open**, lalu arahkan ke folder proyek ini.
3. Tunggu hingga proses **Gradle Sync** selesai (bar bawah menunjukkan progress).
4. Hubungkan perangkat:
   - **HP fisik** — aktifkan USB Debugging (lihat bagian 2️⃣), **atau**
   - **Emulator** — buka **Device Manager** (`Alt+A` → `Device Manager`) lalu klik ▶ pada AVD.
5. Pastikan perangkat terpilih pada toolbar bagian atas.
6. Klik tombol **Run 'app'** (ikon segitiga hijau ▶ atau **Shift + F10**).
7. Aplikasi akan otomatis ter-*build*, ter-*install*, dan berjalan di perangkat.

---

### 2️⃣ Menjalankan di Perangkat Fisik via USB Debugging

#### a) Aktifkan USB Debugging di HP

```
Settings > About phone  >  tap 7× pada "Build number"
Settings > Developer options  >  USB debugging = ON
```

Colokkan kabel USB, lalu di layar HP:
- ⚠️ **Ganti mode USB ke "File Transfer (MTP)"** — bukan "Charging Only", jika tidak adb tidak terdeteksi.
- Tekan **Allow USB debugging** ketika dialog muncul (centang *Always allow*).

#### b) Cek perangkat terdeteksi

```bash
adb devices
```

Output harus menunjukkan status **`device`** (bukan `unauthorized`):

```
List of devices attached
115413742T003183    device
```

#### c) Build, install, jalankan, dan lihat logcat

```bash
# 1) Build + install APK debug ke HP
.\gradlew.bat :app:installDebug

# 2) Jalankan aplikasi
adb shell am start -n com.example.pamtugas2/.MainActivity

# 3) Lihat bukti alur data (semua fitur). Tekan Ctrl+C untuk berhenti.
adb logcat -c
adb logcat -s NewsApp:D *:S
```

> **Catatan:** jika `adb` tidak dikenali di PowerShell, tambahkan Platform-Tools ke variabel `PATH`:
> ```powershell
> $env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
> ```

#### d) Atau pakai skrip otomatis (paling praktis)

Sudah tersedia `install-usb.ps1` yang menjalankan semuanya berurutan:
**cek perangkat → unit test → build → install → jalankan → stream logcat.**

```powershell
# Jangan lupa set Execution Policy agar skrip boleh dijalankan:
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

.\install-usb.ps1                 # jalankan semua langkah
.\install-usb.ps1 -SkipTests       # lewati unit test (lebih cepat)
.\install-usb.ps1 -Uninstall       # hapus aplikasi dari HP
```

---

### 3️⃣ Menampilkan Layar HP di Laptop (scrcpy)

Agar aplikasi bisa didemokan langsung di laptop atau proyektor **tanpa emulator** (emulator memerlukan unduhan *system image* sekitar 1-2 GB), pakai **[scrcpy](https://github.com/Genymobile/scrcpy)** - program open-source yang memirror dan memberi kendali penuh atas layar Android melalui USB.

```powershell
# 1) Install scrcpy (sekali saja)
winget install --id Genymobile.scrcpy --exact

# 2) Jalankan mirroring
scrcpy

# Untuk device tertentu + judul window:
scrcpy -s 115413742T003183 --window-title "News Feed Simulator" --stay-awake
```

Yang bisa dilakukan di jendela scrcpy:

| Aksi | Cara |
|---|---|
| Geser / scroll daftar berita | Seret mouse, atau scroll wheel |
| Buka dialog detail async | Klik kiri pada kartu berita |
| Pilih kategori (fitur filter) | Klik chip `Teknologi` / `Ekonomi` / `Sukan` / `Semasa` |
| Jeda / lanjutkan Flow | Klik tombol `Jeda` / `Lanjut` |
| Bersihkan daftar | Klik tombol `Hapus` |
| Kembali ke HP | `Ctrl+Shift+Z` |
| Layar penuh | `Alt+F11` |

> **Kelebihan:** kualitas HD, latency rendah, memakai GPU HP sehingga laptop tetap ringan, dan **tidak butuh emulator sama sekali**.

---

### 4️⃣ Menjalankan Unit Test (Verifikasi 5 Fitur)

```bash
.\gradlew.bat :app:testDebugUnitTest
```

Laporan hasil uji (HTML) dapat dibuka di:
`app/build/reports/tests/testDebugUnitTest/index.html`

#### Hasil Pengujian

| # | Nama Test | Fitur yang Diuji | Status |
|---|---|---|---|
| 1 | `testRawNewsStreamEmitsNews` | Fitur 1 — Flow menyiarkan berita valid | ✅ PASS |
| 2 | `testCategoryFilterOnlyReturnsMatchingCategory` | Fitur 2 — filter hanya mengembalikan kategori terpilih | ✅ PASS |
| 3 | `testDataTransformationToFormattedNews` | Fitur 3 — judul berlabel `[KATEGORI]`, waktu ada "WIB", preview tidak kosong | ✅ PASS |
| 4 | `testStateFlowReadCountTracksReadArticles` | Fitur 4 — jumlah dibaca naik, tidak double count | ✅ PASS |
| 5 | `testFetchNewsDetailAsync` | Fitur 5 — detail berita termuat dan memuat data benar | ✅ PASS |
| 6 | `addition_isCorrect` | Sanity check boilerplate | ✅ PASS |

**Total: 6 test, 0 failure, 0 error.**

> Untuk menguji `Flow` tanpa *hang*, test memakai `MainDispatcherRule` (mengganti `Dispatchers.Main` dengan `StandardTestDispatcher`) dan `runTest { }`.

---

### 5️⃣ Membangun Berkas APK Debug

```bash
.\gradlew.bat :app:assembleDebug
```

Berkas APK dihasilkan di:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📋 Verifikasi Aliran Data pada Logcat

Selain tampilannya di layar, aplikasi mencetak alur simulasi otomatis ke **Logcat** dengan tag **`NewsApp`** — ini dipakai sebagai **bukti kelima fitur berjalan benar**.

### Contoh Output Nyata

Diuji pada **Infinix X6853, Android 16** via USB Debugging:

```log
I NewsApp : JUMLAH DIBACA: 0 berita
I NewsApp : Memulakan suapan berita untuk kategori Teknologi...
I NewsApp : BERITA MASUK: [TEKNOLOGI] Peluncuran Chipset Snapdragon Generasi Terbaru (17:54:55 WIB)
I NewsApp : Pengguna klik berita ID 2. Memuat turun detail...
I NewsApp : BUTIRAN LENGKAP:
I NewsApp : Detail Artikel #2:
I NewsApp : Judul   : Peluncuran Chipset Snapdragon Generasi Terbaru
I NewsApp : Kategori: Teknologi
I NewsApp : Penulis : Budi Santoso
I NewsApp : Waktu   : Sabtu, 26 September 2026 WIB
I NewsApp : Konten Berita Lengkap:
I NewsApp : Qualcomm resmi mengumumkan chipset generasi terbaru dengan efisiensi daya hingga 30% ...
I NewsApp : JUMLAH DIBACA: 1 berita
```

### Pemetaan Log ke Fitur

| Baris Log | Waktu | Membuktikan Fitur |
|---|---|---|
| `BERITA MASUK: ...` tiap **2 detik** | 17:54:55 | **Fitur 1** — `Flow` menyiarkan berita baru secara berkala |
| Hanya kategori `TEKNOLOGI` yang muncul | — | **Fitur 2** — `filter` menyaring berdasarkan kategori |
| Judul berformat `[TEKNOLOGI] ...` | — | **Fitur 3** — `map` mentransformasi data ke format tampilan |
| `JUMLAH DIBACA: 0` → `JUMLAH DIBACA: 1` | — | **Fitur 4** — `StateFlow` melacak jumlah berita dibaca |
| `Pengguna klik` → `BUTIRAN LENGKAP` **1.022 detik** | 17:54:59.204 → 17:55:00.226 | **Fitur 5** — `Coroutines` async di `Dispatchers.IO` (simulasi latency 1000 ms) |

### Perhatikan Selisih Waktu

```
17:54:59.204  Pengguna klik berita ID 2. Memuat turun detail...
      ↓ menunggu 1.022 detik (async, tidak memblokir UI)
17:55:00.226  BUTIRAN LENGKAP: ...
```

Selisih **1.022 detik** sesuai simulasi `delay(1000L)` di `withContext(Dispatchers.IO)` — dan selama itu UI **tetap responsif** (bukan `runBlocking`).

---

## 🐛 Catatan Troubleshooting: Level Log `INFO` vs `DEBUG`

> **Ini adalah temuan nyata saat pengujian di perangkat, bukan teori.**

Awalnya aplikasi menggunakan `Log.d()` (level **DEBUG**). Saat dijalankan di HP Infinix X6853 (Android 16), **logcat kosong total** — meskipun aplikasi berjalan normal di layar.

**Penyebabnya:** HP tersebut menyetel properti sistem secara global:

```
[persist.log.tag]: [I]
```

Artinya **seluruh log level `DEBUG` diblokir oleh sistem** dan tidak pernah sampai ke logcat. Ini bukan Rare — banyak HP produksi (Infinix/Transsion, Xiaomi, dan sebagian OEM lainnya) melakukan hal yang sama.

**Solusi yang diterapkan:**

1. [`MainActivity.kt`](app/src/main/java/com/example/pamtugas2/MainActivity.kt) memakai **`Log.i("NewsApp", ...)`** (level `INFO`) yang selalu lolos di perangkat mana pun.
2. `install-usb.ps1` menjalankan `setprop log.tag.NewsApp D` sebagai pengaman tambahan.

> Perintah `adb logcat -s NewsApp:D *:S` **tetap sama** dan tetap menampilkan semua log.

---

## ❓ Troubleshooting Umum

| Gejala | Penyebab | Solusi |
|---|---|---|
| `adb devices` kosong | Kabel USB mode "Charging Only" | Ganti mode USB ke **File Transfer (MTP)** |
| Status `unauthorized` | Dialog RSA fingerprint belum disetujui | Cabut-colok kabel, tap **Allow** di HP. Jika tetap: `Developer options > Revoke USB debugging authorizations` |
| Status `offline` | Koneksi USB tidak stabil | Ganti port USB / kabel data, lalu `adb kill-server && adb start-server` |
| `error: no devices/emulators found` | Server adb mati | `adb kill-server` lalu `adb start-server` |
| `adb` tidak dikenali | Platform-Tools belum di PATH | `$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Versi app lama dengan signature berbeda | `adb uninstall com.example.pamtugas2` lalu install ulang |
| Logcat kosong padahal app jalan | OEM memblokir log `DEBUG` | `adb shell setprop log.tag.NewsApp D` (lihat bagian di atas) |
| `.\install-usb.ps1` diblokir PowerShell | Execution Policy | `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` |
| Build gagal `SDK location not found` | `local.properties` hilang | Buka proyek sekali di Android Studio, atau tulis manual `sdk.dir=...` |
| `Windows can't find adb` / driver tidak ada | Driver ADB belum terinstall | Unduh *Universal ADB Driver*, install, lalu `adb devices` lagi |

---

## 📤 Pengunggahan ke GitHub & Submit ke LMS

### Langkah 1 — Pastikan Git tersedia
```bash
git --version
```

### Langkah 2 — Simpan semua perubahan
```bash
git add .
git commit -m "feat: News Feed Simulator Tugas 2 PAM ITERA (Flow, Coroutines, StateFlow)"
```

> File `local.properties`, folder `build/`, dan `.idea/` **sudah diabaikan** oleh `.gitignore` sehingga tidak ikut ter-*upload*.

### Langkah 3 — Buat repositori baru di GitHub
1. Buka <https://github.com/new>
2. **Repository name**: `PAM-Tugas2`
3. Pilih **Private** (atau **Public** jika diminta pengajar)
4. ⚠️ **Jangan** centang "Add a README file", `.gitignore`, atau `license` — semuanya sudah ada
5. Klik **Create repository**

### Langkah 4 — Hubungkan & unggah ke GitHub
```bash
git branch -M main
git remote add origin https://github.com/<USERNAME-ANDA>/PAM-Tugas2.git
git push -u origin main
```

> Saat *push*, GitHub meminta `Username` dan `Personal access token`.
> Gunakan **Personal access token**, **bukan** password akun.
> Token dibuat di: **Settings > Developer settings > Personal access tokens > Tokens (classic)** dengan akses `repo`.

### Langkah 5 — Verifikasi & submit tautan ke LMS
```bash
# Harus menampilkan "nothing to commit, working tree clean"
git status

# Cek branch utama sudah terhubung ke GitHub
git branch -vv
```

Buka halaman repositori di GitHub, pastikan seluruh file proyek terlihat, lalu salin tautannya:
```
https://github.com/<USERNAME-ANDA>/PAM-Tugas2
```

Tempelkan tautan tersebut pada kolom **submission LMS ITERA**.

---

## 📄 Lisensi & Catatan

Proyek ini dibuat untuk keperluan **pembelajaran dan tugas akademik** pada mata kuliah Pengembangan Aplikasi Mobile (ITERA). Seluruh data berita di dalam aplikasi bersifat **fiktif/simulasi** dan tidak merujuk berita nyata.

---

<div align="center">

**Tugas Praktikum 2 — Pengembangan Aplikasi Mobile**
Institut Teknologi Sumatera (ITERA)
Topik: **Coroutines & Flow** ❤

</div>
