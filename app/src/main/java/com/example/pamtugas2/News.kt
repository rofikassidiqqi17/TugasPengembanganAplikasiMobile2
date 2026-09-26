package com.example.pamtugas2

/**
 * Model data mentah untuk merepresentasikan sebuah artikel berita.
 */
data class News(
    val id: Int,
    val title: String,
    val category: String,
    val content: String,
    val author: String = "Redaksi ITERA News",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Model data presentasi setelah proses transformasi (Fitur 3: Transform data).
 */
data class FormattedNews(
    val id: Int,
    val originalTitle: String,
    val formattedTitle: String,     // Format: "[KATEGORI] Judul Berita"
    val category: String,
    val previewContent: String,    // Ringkasan singkat isi berita
    val formattedTime: String,     // Format jam WIB (misal: "14:30:15 WIB")
    val author: String,
    val publishedDate: String,     // Tanggal lengkap WIB (misal: "Jumat, 25 September 2026")
    val fullContent: String,       // Isi artikel lengkap (disimpan saat transform)
    val isRead: Boolean = false
) {
    override fun toString(): String {
        return "$formattedTitle ($formattedTime)"
    }
}

/**
 * State untuk dialog detail berita yang dimuat secara asinkron (Fitur 5: Coroutines async).
 */
data class NewsDetailState(
    val isOpen: Boolean = false,
    val isLoading: Boolean = false,
    val newsId: Int? = null,
    val title: String = "",
    val category: String = "",
    val author: String = "",
    val detailContent: String = ""
)

/**
 * Kumpulan data berita simulasi yang mencakup berbagai kategori.
 */
val realNewsData = listOf(
    News(
        id = 1,
        title = "Harga Emas Terus Meningkat di Pasaran Global",
        category = "Ekonomi",
        content = "Harga emas hari ini mencatat rekor tertinggi dalam sejarah susulan ketidaktentuan pasar keuangan global. Para investor cenderung mengalihkan modal mereka ke instrumen safe-haven guna menjaga stabilitas portofolio jangka panjang.",
        author = "Ahmad Pratama"
    ),
    News(
        id = 2,
        title = "Peluncuran Chipset Snapdragon Generasi Terbaru",
        category = "Teknologi",
        content = "Qualcomm resmi mengumumkan chipset generasi terbaru dengan efisiensi daya hingga 30% dan peningkatan performa komputasi AI on-device hingga dua kali lipat dibanding generasi pendahulunya.",
        author = "Budi Santoso"
    ),
    News(
        id = 3,
        title = "Timnas Sepak Bola Pastikan Tiket ke Putaran Final Piala Asia",
        category = "Sukan",
        content = "Kemenangan 2-0 di laga pamungkas babak kualifikasi memastikan slot otomatis tim nasional ke putaran final Piala Asia. Permainan disiplin di lini pertahanan menjadi kunci keberhasilan skuad garuda.",
        author = "Doni Setiawan"
    ),
    News(
        id = 4,
        title = "Peringatan Dini Cuaca Ekstrem Melanda Sejumlah Wilayah",
        category = "Semasa",
        content = "Badan Meteorologi merilis peringatan dini potensi hujan lebat disertai angin kencang untuk beberapa hari ke depan. Masyarakat diimbau waspada terhadap kemungkinan genangan air dan pohon tumbang.",
        author = "Rina Maharani"
    ),
    News(
        id = 5,
        title = "Integrasi AI Mempercepat Siklus Pengembangan Perangkat Lunak",
        category = "Teknologi",
        content = "Riset industri terbaru menemukan bahwa pemanfaatan AI coding assistant mampu memangkas waktu pembuatan kode boilerplate dan penulisan unit test hingga 50% tanpa menurunkan standar keamanan sistem.",
        author = "Fajar Ramadhan"
    ),
    News(
        id = 6,
        title = "Penguatan Nilai Tukar Rupiah Terus Berlanjut",
        category = "Ekonomi",
        content = "Arus modal asing yang stabil dan neraca perdagangan yang mencatatkan surplus positif memberikan sentimen positif bagi apresiasi nilai tukar rupiah di bursa valuta regional.",
        author = "Ahmad Pratama"
    ),
    News(
        id = 7,
        title = "Kontingen Mahasiswa Raih Prestasi di Kejuaraan Bulutangkis",
        category = "Sukan",
        content = "Perjuangan gigih atlet mahasiswa membuahkan medali emas ganda putra dalam turnamen antar universitas se-Asia Tenggara setelah melewati partai final sengit tiga set.",
        author = "Doni Setiawan"
    ),
    News(
        id = 8,
        title = "Percepatan Pembangunan Pembangkit Energi Surya Ramah Lingkungan",
        category = "Semasa",
        content = "Pemerintah bersama pemangku kepentingan mempercepat realisasi proyek panel surya skala gigawatt guna mendukung target emisi nol bersih dan pasokan listrik ramah lingkungan terjangkau.",
        author = "Siti Aisyah"
    ),
    News(
        id = 9,
        title = "Kotlin Multiplatform (KMP) Menjadi Standar Baru Aplikasi Mobile Modern",
        category = "Teknologi",
        content = "Adopsi Kotlin Multiplatform melonjak pesat di kalangan pengembang mobile karena efisiensi kode bersama antara platform Android dan iOS dengan native performance optimal.",
        author = "Budi Santoso"
    ),
    News(
        id = 10,
        title = "Sektor UMKM Digital Catat Pertumbuhan Transaksi 25%",
        category = "Ekonomi",
        content = "Pemanfaatan sistem pembayaran digital QRIS dan digital marketing mendorong pertumbuhan omset pelaku usaha mikro dan menengah secara nasional.",
        author = "Ahmad Pratama"
    )
)

/**
 * Daftar kategori yang tersedia untuk pemfilteran berita.
 */
val availableCategories = listOf("Semua", "Teknologi", "Ekonomi", "Sukan", "Semasa")