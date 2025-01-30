package com.example.myresto.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PesananListenerService : Service() {
    private lateinit var firestore: FirebaseFirestore
    private var pesananListener: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()

        // Membuat FirebaseFirestore instance di sini
        firestore = FirebaseFirestore.getInstance()

        // Segera mulai foreground service saat service dibuat
        startForegroundService()

        // Mulai mendengarkan pesanan baru
        listenForNewPesanan()
    }

    override fun onDestroy() {
        super.onDestroy()
        pesananListener?.remove() // Hapus listener saat service berhenti
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Fungsi untuk memulai foreground service dengan segera
    private fun startForegroundService() {
        val channelId = "pesanan_channel"
        val channelName = "Pesanan Baru"
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Membuat Notification Channel untuk API >= 26 (Android O)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(channel)
        }

        // Membuat notifikasi untuk service foreground
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Memantau Pesanan")
            .setContentText("Service sedang berjalan untuk memantau pesanan baru.")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()

        // Segera panggil startForeground setelah notification dibangun
        startForeground(1, notification)
    }

    // Mendengarkan pesanan baru di Firestore
    private fun listenForNewPesanan() {
        pesananListener = firestore.collection("pesanan")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                for (change in snapshots!!.documentChanges) {
                    // Jika ada pesanan baru, tampilkan notifikasi
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val data = change.document.data
                        val nama = data["nama_pemesan"] as String? ?: "Tidak diketahui"
                        val noMeja = data["no_meja"] as String? ?: "Tidak ada"

                        // Menampilkan notifikasi jika ada pesanan baru
                        showNotification("Pesanan Baru", "Pesanan dari $nama di meja $noMeja")
                    }
                }
            }
    }

    // Fungsi untuk menampilkan notifikasi
    private fun showNotification(title: String, message: String) {
        val channelId = "pesanan_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Notifikasi akan hilang ketika diklik
            .build()

        notificationManager?.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}