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
        //
        // CATATAN PENTING:
        // Log memakai level INFO (bukan DEBUG) dengan sengaja. Beberapa HP
        // produksi -- terutama Infinix/Transsion, Xiaomi, dan beberapa OEM
        // lainnya -- menyetel secara global "persist.log.tag=I" sehingga SELURUH
        // log level DEBUG diblokir oleh sistem dan tidak pernah muncul di logcat.
        // Memakai INFO membuat bukti alur data (Flow/filter/map/StateFlow/async)
        // selalu terlihat di perangkat mana pun tanpa perlu setprop manual.
        //
        // Perintah logcat di README tetap sama: adb logcat -s NewsApp:D *:S
        //
        // Tag dipakai: "NewsApp"
        // ================================================================

        // 1. Mengamati perubahan jumlah berita yang dibaca (StateFlow)
        lifecycleScope.launch {
            viewModel.readCount.collect { count ->
                Log.i("NewsApp", "JUMLAH DIBACA: $count berita")
            }
        }

        // 2. Mengambil aliran berita berdasarkan kategori Teknologi (Flow + filter + map)
        lifecycleScope.launch {
            Log.i("NewsApp", "Memulakan suapan berita untuk kategori Teknologi...")
            viewModel.getProcessedNews("Teknologi").collect { formattedNews ->
                Log.i("NewsApp", "BERITA MASUK: $formattedNews")
            }
        }

        // 3. Simulasi aksi klik berita oleh pengguna setelah 5 detik (Coroutines async)
        lifecycleScope.launch {
            delay(5000)
            Log.i("NewsApp", "Pengguna klik berita ID 2. Memuat turun detail...")

            // Memanggil suspend function secara langsung
            val detailContent = viewModel.fetchNewsDetailAsync(2)

            Log.i("NewsApp", "BUTIRAN LENGKAP:\n$detailContent")

            viewModel.markAsRead(2)
        }
    }
}