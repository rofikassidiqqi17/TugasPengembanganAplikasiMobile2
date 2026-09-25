package com.example.pamtugas2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule untuk menyediakan CoroutineDispatcher Main saat unit test JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: NewsViewModel

    @Before
    fun setUp() {
        viewModel = NewsViewModel()
        // Hentikan infinite stream bawaan agar scheduler unit test tidak menggantung
        viewModel.pauseStream()
    }

    @After
    fun tearDown() {
        viewModel.pauseStream()
    }

    /**
     * Uji FITUR 1: Flow yang mensimulasikan data berita baru
     */
    @Test
    fun testRawNewsStreamEmitsNews() = runTest {
        val stream = viewModel.getRawNewsStream()
        val firstNews = stream.first()

        assertNotNull("Berita pertama tidak boleh null", firstNews)
        assertTrue("ID berita harus valid (> 0)", firstNews.id > 0)
        assertFalse("Judul berita tidak boleh kosong", firstNews.title.isBlank())
    }

    /**
     * Uji FITUR 2: Filter berita berdasarkan kategori tertentu
     */
    @Test
    fun testCategoryFilterOnlyReturnsMatchingCategory() = runTest {
        val categoryTarget = "Teknologi"
        val newsList = viewModel.getProcessedNews(categoryTarget).take(2).toList()

        assertEquals("Harus mengumpulkan 2 berita kategori Teknologi", 2, newsList.size)
        newsList.forEach { formattedNews ->
            assertEquals(
                "Semua berita harus memiliki kategori $categoryTarget",
                categoryTarget.lowercase(),
                formattedNews.category.lowercase()
            )
        }
    }

    /**
     * Uji FITUR 3: Transform data mentah ke format siap tampil
     */
    @Test
    fun testDataTransformationToFormattedNews() = runTest {
        val newsItem = viewModel.getProcessedNews("Semua").first()

        // Memastikan transformasi judul menambahkan tag [KATEGORI]
        assertTrue(
            "Judul harus diawali dengan [${newsItem.category.uppercase()}]",
            newsItem.formattedTitle.startsWith("[${newsItem.category.uppercase()}]")
        )

        // Memastikan waktu diformat (mengandung kata 'WIB')
        assertTrue(
            "Waktu berita harus terformat dengan zona WIB",
            newsItem.formattedTime.contains("WIB")
        )

        // Memastikan preview ringkasan tidak kosong
        assertFalse("Preview content tidak boleh kosong", newsItem.previewContent.isBlank())
    }

    /**
     * Uji FITUR 4: StateFlow untuk menyimpan jumlah berita yang sudah dibaca
     */
    @Test
    fun testStateFlowReadCountTracksReadArticles() {
        assertEquals("Jumlah awal berita dibaca harus 0", 0, viewModel.readCount.value)

        // Tandai berita 1 sebagai dibaca
        viewModel.markAsRead(1)
        assertEquals("Jumlah dibaca harus menjadi 1", 1, viewModel.readCount.value)

        // Tandai berita 1 lagi (tidak boleh double count)
        viewModel.markAsRead(1)
        assertEquals("Jumlah dibaca tetap 1 (mencegah duplikasi)", 1, viewModel.readCount.value)

        // Tandai berita 2 sebagai dibaca
        viewModel.markAsRead(2)
        assertEquals("Jumlah dibaca harus menjadi 2", 2, viewModel.readCount.value)
    }

    /**
     * Uji FITUR 5: Coroutines untuk mengambil detail berita secara async
     */
    @Test
    fun testFetchNewsDetailAsync() = runTest {
        val newsId = 2
        val detailContent = viewModel.fetchNewsDetailAsync(newsId)

        assertNotNull("Konten detail tidak boleh null", detailContent)
        assertTrue("Konten harus memuat ID berita", detailContent.contains("#$newsId"))
        assertTrue("Konten harus memuat label detail artikel", detailContent.contains("Detail Artikel"))
    }
}
