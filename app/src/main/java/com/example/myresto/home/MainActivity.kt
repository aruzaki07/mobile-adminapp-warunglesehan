package com.example.myresto.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myresto.R
import com.example.myresto.databinding.ActivityMainBinding
import com.example.myresto.historyPenjualan.HistoryPenjualanActivity
import com.example.myresto.menu.MyListMenuActivity
import com.example.myresto.menunggu_pembayaran.MenungguPembayaranActivity
import com.example.myresto.pesanan.PesananActivity
import com.example.myresto.service.PesananListenerService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications permission rejected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Mulai service untuk mendengarkan pesanan baru
        val serviceIntent = Intent(this, PesananListenerService::class.java)
        startForegroundService(serviceIntent)

        binding.daftarmenubutton.setOnClickListener {
            val intent = Intent(this@MainActivity, MyListMenuActivity::class.java)
            startActivity(intent)
        }

        binding.daftarpesananbutton.setOnClickListener {
            val intent = Intent(this@MainActivity, PesananActivity::class.java)
            startActivity(intent)
        }

        binding.menunggupembayaranbutton.setOnClickListener {
            val intent = Intent(this@MainActivity, MenungguPembayaranActivity::class.java)
            startActivity(intent)
        }

        binding.historypenjualanbutton.setOnClickListener {
            val intent = Intent(this@MainActivity,HistoryPenjualanActivity::class.java)
            startActivity(intent)
        }

    }
}