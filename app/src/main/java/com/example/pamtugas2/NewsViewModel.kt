package com.example.pamtugas2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class NewsViewModel : ViewModel() {

    // =========================================================================
    // FITUR 4: StateFlow untuk menyimpan jumlah berita yang sudah dibaca
    // =========================================================================
    private val _readCount = MutableStateFlow(0)
    val readCount: StateFlow<Int> = _readCount.asStateFlow()

    // Melacak set ID berita yang sudah dibaca agar tidak double count jika dibuka berulang
    private val _readNewsIds = MutableStateFlow<Set<Int>>(emptySet())
    val readNewsIds: StateFlow<Set<Int>> = _readNewsIds.asStateFlow()

    // =========================================================================
    // STATE UNTUK UI FEED BERITA
    // =========================================================================
    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _newsFeedList = MutableStateFlow<List<FormattedNews>>(emptyList())
    val newsFeedList: StateFlow<List<FormattedNews>> = _newsFeedList.asStateFlow()

    private val _isStreaming = MutableStateFlow(true)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _detailState = MutableStateFlow(NewsDetailState())
    val detailState: StateFlow<NewsDetailState> = _detailState.asStateFlow()

    private var streamJob: Job? = null

    /**
     * Penomoran berita bersifat global dan monoton (tidak pernah di-reset).
     * Jika counter di-reset setiap stream dimulai ulang, ID berita akan terulang
     * sehingga berita yang sudah dibaca kembali ditandai "telah dibaca".
     */
    private val newsIdCounter = AtomicInteger(0)

    init {
        // Otomatis memulai suapan berita saat ViewModel diinisialisasi
        startStream()
    }

    // =========================================================================
    // FITUR 1: Flow yang mensimulasikan data berita baru setiap 2 detik
    // =========================================================================
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
            delay(2000L) // Jeda 2 detik simulasi berita masuk
        }
    }

    // =========================================================================
    // FITUR 2 & FITUR 3: Filter berdasarkan kategori dan Transform format data
    // =========================================================================
    /**
     * Mengambil aliran berita yang telah difilter berdasarkan kategori (Fitur 2)
     * dan ditransformasikan ke format siap tampil (Fitur 3).
     */
    fun getProcessedNews(category: String): Flow<FormattedNews> {
        return getRawNewsStream()
            // FITUR 2: Filter berita berdasarkan kategori tertentu
            .filter { news ->
                if (category.equals("Semua", ignoreCase = true)) {
                    true
                } else {
                    news.category.equals(category, ignoreCase = true)
                }
            }
            // FITUR 3: Transform data menjadi format yang ditampilkan
            .map { news ->
                transformToFormattedNews(news)
            }
    }

    /**
     * Transformasi data objek mentah News menjadi FormattedNews untuk tampilan UI.
     */
    private fun transformToFormattedNews(news: News): FormattedNews {
        val locale = Locale.forLanguageTag("id-ID")
        val publishDate = Date(news.timestamp)
        val timeFormatter = SimpleDateFormat("HH:mm:ss", locale)
        val dateFormatter = SimpleDateFormat("EEEE, dd MMMM yyyy", locale)
        val isRead = _readNewsIds.value.contains(news.id)

        return FormattedNews(
            id = news.id,
            originalTitle = news.title,
            formattedTitle = "[${news.category.uppercase(locale)}] ${news.title}",
            category = news.category,
            previewContent = if (news.content.length > 80) news.content.take(80) + "..." else news.content,
            formattedTime = "${timeFormatter.format(publishDate)} WIB",
            author = news.author,
            publishedDate = dateFormatter.format(publishDate),
            fullContent = news.content,
            isRead = isRead
        )
    }

    // =========================================================================
    // MANAJEMEN ALIRAN FEED (UI Collection)
    // =========================================================================
    fun startStream() {
        streamJob?.cancel()
        _isStreaming.value = true
        streamJob = viewModelScope.launch {
            getProcessedNews(_selectedCategory.value).collect { newItem ->
                val itemWithStatus = newItem.copy(isRead = _readNewsIds.value.contains(newItem.id))
                // Menambahkan berita terbaru di bagian atas list (maksimal 50 berita)
                _newsFeedList.value = listOf(itemWithStatus) + _newsFeedList.value.take(49)
            }
        }
    }

    fun pauseStream() {
        _isStreaming.value = false
        streamJob?.cancel()
    }

    fun toggleStream() {
        if (_isStreaming.value) {
            pauseStream()
        } else {
            startStream()
        }
    }

    fun setCategory(category: String) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            // Reset feed list untuk kategori baru dan mulai aliran baru
            _newsFeedList.value = emptyList()
            if (_isStreaming.value) {
                startStream()
            }
        }
    }

    fun clearFeed() {
        _newsFeedList.value = emptyList()
    }

    // =========================================================================
    // FITUR 5: Coroutines untuk mengambil detail berita secara async
    // =========================================================================
    /**
     * Mengambil detail artikel berita secara asynchronous menggunakan Coroutines Dispatchers.IO.
     * Mensimulasikan jeda latensi jaringan/database selama 1 detik.
     */
    suspend fun fetchNewsDetailAsync(newsId: Int): String = withContext(Dispatchers.IO) {
        delay(1000L) // Simulasi network delay async 1 detik

        // Prioritas: ambil item yang benar-benar tampil di feed agar isi artikel
        // selalu konsisten dengan judul yang diklik pengguna.
        val displayed = _newsFeedList.value.firstOrNull { it.id == newsId }

        // Fallback bila berita tidak ada di feed (mis. dipanggil langsung dari logcat).
        val fallback = realNewsData[Math.floorMod(newsId - 1, realNewsData.size)]

        val title = displayed?.originalTitle ?: fallback.title
        val category = displayed?.category ?: fallback.category
        val author = displayed?.author ?: fallback.author
        val body = displayed?.fullContent ?: fallback.content
        val publishedDate = displayed?.publishedDate ?: SimpleDateFormat(
            "EEEE, dd MMMM yyyy",
            Locale.forLanguageTag("id-ID")
        ).format(Date(System.currentTimeMillis()))

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

    /**
     * Membuka dialog detail berita: memicu Coroutines async dan memperbarui StateFlow readCount.
     */
    fun openNewsDetail(news: FormattedNews) {
        viewModelScope.launch {
            _detailState.value = NewsDetailState(
                isOpen = true,
                isLoading = true,
                newsId = news.id,
                title = news.originalTitle,
                category = news.category,
                author = news.author,
                detailContent = ""
            )

            // Mengambil detail artikel secara async via suspend function
            val fullContent = fetchNewsDetailAsync(news.id)

            _detailState.value = _detailState.value.copy(
                isLoading = false,
                detailContent = fullContent
            )

            // Tandai berita sebagai sudah dibaca (Fitur 4: StateFlow)
            markAsRead(news.id)
        }
    }

    fun closeNewsDetail() {
        _detailState.value = _detailState.value.copy(isOpen = false)
    }

    // =========================================================================
    // FITUR 4: StateFlow Update
    // =========================================================================
    /**
     * Menambah jumlah berita yang telah dibaca pada StateFlow.
     */
    fun markAsRead(newsId: Int? = null) {
        if (newsId != null) {
            if (!_readNewsIds.value.contains(newsId)) {
                _readNewsIds.value = _readNewsIds.value + newsId
                _readCount.value = _readNewsIds.value.size

                // Perbarui status isRead pada list feed yang sedang aktif
                _newsFeedList.value = _newsFeedList.value.map { item ->
                    if (item.id == newsId) item.copy(isRead = true) else item
                }
            }
        } else {
            // Inkremen langsung bila dipanggil tanpa ID
            _readCount.value += 1
        }
    }
}