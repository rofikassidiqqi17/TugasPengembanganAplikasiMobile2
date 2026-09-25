package com.example.pamtugas2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.pamtugas2.ui.theme.PAMTugas2Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // =====================================================================
        // TAMPILAN APLIKASI (Jetpack Compose UI)
        // =====================================================================
        setContent {
            PAMTugas2Theme {
                NewsAppScreen(viewModel = viewModel)
            }
        }

        // =====================================================================
        // LOGIK SIMULASI LOGCAT (Sesuai Spesifikasi Alur Praktikum)
        // =====================================================================

        // 1. Mengamati perubahan jumlah berita yang dibaca (StateFlow)
        lifecycleScope.launch {
            viewModel.readCount.collect { count ->
                Log.d("NewsApp", "JUMLAH DIBACA: $count berita")
            }
        }

        // 2. Mengambil aliran berita berdasarkan kategori Teknologi (Flow + filter + map)
        lifecycleScope.launch {
            Log.d("NewsApp", "Memulakan suapan berita untuk kategori Teknologi...")
            viewModel.getProcessedNews("Teknologi").collect { formattedNews ->
                Log.d("NewsApp", "BERITA MASUK: $formattedNews")
            }
        }

        // 3. Simulasi aksi klik berita oleh pengguna setelah 5 detik (Coroutines async)
        lifecycleScope.launch {
            delay(5000)
            Log.d("NewsApp", "Pengguna klik berita ID 2. Memuat turun detail...")

            // Memanggil suspend function secara langsung
            val detailContent = viewModel.fetchNewsDetailAsync(2)

            Log.d("NewsApp", "BUTIRAN LENGKAP:\n$detailContent")

            viewModel.markAsRead(2)
        }
    }
}