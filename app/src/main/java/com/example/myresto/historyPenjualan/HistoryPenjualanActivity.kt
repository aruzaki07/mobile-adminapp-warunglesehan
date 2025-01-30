    package com.example.myresto.historyPenjualan

    import android.app.DatePickerDialog
    import android.content.Intent
    import android.os.Build
    import android.os.Bundle
    import android.util.Log
    import android.view.View
    import android.widget.Toast
    import androidx.activity.enableEdgeToEdge
    import androidx.annotation.RequiresApi
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.ViewCompat
    import androidx.core.view.WindowInsetsCompat
    import androidx.recyclerview.widget.LinearLayoutManager
    import com.example.myresto.R
    import com.example.myresto.databinding.ActivityHistoryPenjualanBinding
    import com.example.myresto.model.HistoryPenjualan
    import com.example.myresto.model.Menu
    import com.example.myresto.model.Pesanan
    import com.example.myresto.pesanan.DetailPesananActivity
    import com.example.myresto.pesanan.FirebasePesananAdapter
    import com.firebase.ui.firestore.FirestoreRecyclerOptions
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.firestore.Query
    import com.google.firebase.firestore.ktx.firestore
    import com.google.firebase.ktx.Firebase
    import java.sql.Timestamp
    import java.text.NumberFormat
    import java.time.Instant
    import java.time.LocalDate
    import java.time.LocalDateTime
    import java.time.ZoneId
    import java.util.Date
    import java.util.Locale

    class HistoryPenjualanActivity : AppCompatActivity(), FirebaseHistoryPenjualanAdapter.OnPesananItemClickListener {
        private lateinit var binding: ActivityHistoryPenjualanBinding
        private lateinit var db: FirebaseFirestore
        private lateinit var adapter: FirebaseHistoryPenjualanAdapter
        private var tanggalMulai: LocalDate? = null
        private var tanggalAkhir: LocalDate? = null

        var totalRevenue = 0

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityHistoryPenjualanBinding.inflate(layoutInflater)
            setContentView(binding.root)



            binding.btnPilihTanggalMulai.setOnClickListener {
                showDatePicker { selectedDate ->
                    tanggalMulai = selectedDate
                    binding.tvTanggalMulai.text = "Mulai: ${selectedDate.dayOfMonth}/${selectedDate.monthValue}/${selectedDate.year}"
                }
            }

            binding.btnPilihTanggalAkhir.setOnClickListener {
                showDatePicker { selectedDate ->
                    tanggalAkhir = selectedDate
                    binding.tvTanggalAkhir.text = "Akhir: ${selectedDate.dayOfMonth}/${selectedDate.monthValue}/${selectedDate.year}"
                }
            }



            binding.btnFilter.setOnClickListener {
                if (tanggalMulai != null && tanggalAkhir != null) {
                    totalRevenue = 0

                    fetchTransactionDataFromFirestore(
                        tanggalMulai!!,
                        tanggalAkhir!!
                    )
                } else {
                    Toast.makeText(this, "Pilih tanggal mulai dan akhir terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
            }

            binding.iconback.setOnClickListener {
                finish()
            }



//            fetchTransactionDataFromFirestore()

        }


        @RequiresApi(Build.VERSION_CODES.O)
        private fun showDatePicker(onDateSelected: (LocalDate) -> Unit) {
            val currentDate = LocalDate.now()

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    onDateSelected(selectedDate)
                },
                currentDate.year,
                currentDate.monthValue - 1,
                currentDate.dayOfMonth
            ).show()
        }



        @RequiresApi(Build.VERSION_CODES.O)
        fun fetchTransactionDataFromFirestore(tanggalMulai: LocalDate, tanggalAkhir: LocalDate) {
            showLoading(true)


            val startTimestamp = tanggalMulai.atStartOfDay(ZoneId.of("Asia/Jakarta")).toInstant().toEpochMilli()
            val endTimestamp = tanggalAkhir.atTime(23, 59, 59).atZone(ZoneId.of("Asia/Jakarta")).toInstant().toEpochMilli()


            val db = FirebaseFirestore.getInstance()
            val menuMap = mutableMapOf<String, Menu>() // Map untuk mencocokkan id_menu dengan detail menu



            val historyRef = db.collection("history_penjualan")//tabel refrensi

            val manager = LinearLayoutManager(this)
            binding.rvHistoryPenjualan.layoutManager = manager

            val options = FirestoreRecyclerOptions.Builder<Pesanan>()
                .setQuery(historyRef
                    .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                    .whereLessThanOrEqualTo("timestamp", endTimestamp)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    , Pesanan::class.java)
                .build()

            adapter = FirebaseHistoryPenjualanAdapter(options, this)
            binding.rvHistoryPenjualan.adapter = adapter

            adapter.updateOptions(options) // Perbarui opsi adapter
            adapter.startListening() // Mulai mendengarkan data baru


            db.collection("menus")
                .get()
                .addOnSuccessListener { menuResult ->
                    for (menuDoc in menuResult) {
                        val menu = menuDoc.toObject(Menu::class.java)
                        menu.id?.let { menuMap[it] = menu }
                    }

                    // Query Firestore untuk mengambil transaksi bulan ini
                    db.collection("history_penjualan")
                        .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                        .whereLessThanOrEqualTo("timestamp", endTimestamp)
                        .get()
                        .addOnSuccessListener { result ->
                            val historyPenjualans = mutableListOf<HistoryPenjualan>()

                            // Menyimpan hasil transaksi yang diambil dari Firestore
                            for (document in result) {
                                val historyPenjualan =
                                    document.toObject(HistoryPenjualan::class.java)
                                historyPenjualan.total_harga?.let { harga ->
                                    totalRevenue += harga
                                }
                                historyPenjualans.add(historyPenjualan)
                            }

                            // Setelah data transaksi berhasil diambil, generate laporan hari dan jam ramai
                            generateDayAndHourReport(historyPenjualans, menuMap)
                            binding.uang.text = "+ ${
                                NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                                    .format(totalRevenue)
                            }"
                            showLoading(false)
                        }
                        .addOnFailureListener { exception ->
                            // Menangani error jika gagal mengambil data
                            showLoading(false)
                            Toast.makeText(
                                this,
                                "Error getting documents: $exception",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { exception ->
                    showLoading(false)
                    Toast.makeText(this, "Error getting menu: $exception", Toast.LENGTH_SHORT).show()
                }
        }


        @RequiresApi(Build.VERSION_CODES.O)
        fun getDayAndHourFromTimestamp(timestamp: Long): Pair<String, Int>? {
            // Mengonversi timestamp menjadi Instant
            val instant = Instant.ofEpochMilli(timestamp)
            // Mengonversi Instant menjadi LocalDateTime dengan zona waktu default
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Jakarta"))
            // Mendapatkan hari dalam format nama (MONDAY, TUESDAY, etc.)
            val dayOfWeek = dateTime.dayOfWeek.name
            // Mendapatkan jam dari waktu transaksi
            val hour = dateTime.hour

            return Pair(dayOfWeek, hour)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun generateDayAndHourReport(
            historyPenjualans: List<HistoryPenjualan>,
            menuMap: Map<String, Menu>
        ) {
            val dayHourCount = mutableMapOf<Pair<String, Int>, Int>()
            val foodCount = mutableMapOf<String, Int>()
            val drinkCount = mutableMapOf<String, Int>()
            var totalItemsSold = 0 // Variabel untuk menghitung total menu yang terjual

            historyPenjualans.forEach { penjualan ->
                penjualan.timestamp?.let {
                    // Mengambil pasangan (hari, jam) dari timestamp transaksi
                    val dayHour = getDayAndHourFromTimestamp(it)
                    dayHour?.let { dayHourPair ->
                        // Menghitung frekuensi (berapa banyak transaksi di hari dan jam tersebut)
                        dayHourCount[dayHourPair] = dayHourCount.getOrDefault(dayHourPair, 0) + 1
                    }
                }


                penjualan.daftar_item?.forEach { item ->
                    val menu = menuMap[item.id_menu]
                    menu?.let {
                        totalItemsSold += item.jumlah ?: 0
                        when (it.jenis) {
                            "Makanan" -> {
                                foodCount[it.nama ?: "-"] =
                                    foodCount.getOrDefault(it.nama ?: "", 0) + (item.jumlah ?: 0)
                            }
                            "Minuman" -> {
                                drinkCount[it.nama ?: "-"] =
                                    drinkCount.getOrDefault(it.nama ?: "", 0) + (item.jumlah ?: 0)
                            }
                        }
                    }
                }

            }


            // Menemukan waktu yang paling ramai
            val peakTime = dayHourCount.maxByOrNull { it.value }
            binding.hari.text = "Hari : ${peakTime?.key?.first ?: "-"}"
            binding.jam.text  = "Jam : ${peakTime?.key?.second ?: "-"}"
            binding.jumTransaksi.text = "Transaksi : ${peakTime?.value ?: "-"}"
            // Menampilkan jumlah menu yang terjual
            binding.menuterjuan.text = "$totalItemsSold item telah terjual"

            // Menemukan makanan dan minuman terlaris
            val bestFood = foodCount.maxByOrNull { it.value }
            val bestDrink = drinkCount.maxByOrNull { it.value }

            binding.makananterlaris.text = "Makanan Terlaris: ${bestFood?.key ?: "-"} (${bestFood?.value ?: 0} kali dipesan)"

            binding.minumanterlaris.text = "Minuman Terlaris: ${bestDrink?.key ?: "-"} (${bestDrink?.value ?: 0} kali dipesan)"
        }





        private fun showLoading(isLoading: Boolean) {
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }



        override fun onMenuItemClick(pesanan: Pesanan) {
    //        val intent = Intent(this, DetailPesananActivity::class.java)
    //        intent.putExtra("ID", pesanan.id_pesanan)
    //        intent.putExtra("NAMA", pesanan.nama_pemesan)
    //        intent.putExtra("TOTAL_HARGA", pesanan.total_harga)
    //        intent.putExtra("NO_MEJA", pesanan.no_meja)
    //        intent.putExtra("DIPROSES", pesanan.diproses)
    //        val daftarItem = arrayListOf(pesanan.daftar_item)
    //        // Pastikan daftar_item adalah ArrayList<MenuItem>
    //        intent.putParcelableArrayListExtra("DAFTAR_MENU", ArrayList(pesanan.daftar_item))
    //        startActivity(intent)
        }
    }