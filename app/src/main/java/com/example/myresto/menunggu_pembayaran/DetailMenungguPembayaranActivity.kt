package com.example.myresto.menunggu_pembayaran

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myresto.databinding.ActivityDetailMenungguPembayaranBinding
import com.example.myresto.menu.MyListMenuActivity
import com.example.myresto.model.DaftarMenu
import com.example.myresto.model.HistoryPenjualan
import com.example.myresto.model.Menu
import com.example.myresto.model.MenuItem
import com.example.myresto.pesanan.MenuItemAdapter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class DetailMenungguPembayaranActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailMenungguPembayaranBinding
    private lateinit var firestore: FirebaseFirestore
    private var totalHarga: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailMenungguPembayaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        var nama = intent.getStringExtra("NAMA")
        var total_harga = intent.getStringExtra("TOTAL_HARGA")
        var no_meja = intent.getStringExtra("NO_MEJA")
        var diproses  = intent.getBooleanExtra("DIPROSES", false)
        var id_pesanan = intent.getStringExtra("ID")
        val daftarMenu = intent.getParcelableArrayListExtra<MenuItem>("DAFTAR_MENU")

        binding.nama.text = "Nama Pemesan : ${nama}"
        binding.noMeja.text = "No Meja : ${no_meja}"
        if (totalHarga !== null){
            binding.totalHarga.text = "${totalHarga}"
        }else{
            binding.totalHarga.text = "Total harga Kosong"
        }


        if (daftarMenu != null) {
            fetchMenuDetails(daftarMenu)
        }else{
            binding.totalHarga.text = "GakJelas"
        }

        binding.KonfirmasiBut.setOnClickListener {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Konfirmasi Pesanan")
            builder.setMessage("Apakah Anda yakin ingin konfirmasi pembayaran ini, pastikan customer tersebut telah membayar?")

            builder.setPositiveButton("Ya") { dialog, _ ->
                // Jika pengguna mengonfirmasi, lanjutkan dengan konfirmasi pesanan
                konfirmasi(id_pesanan.toString())
                dialog.dismiss() // Tutup dialog
            }

            builder.setNegativeButton("Tidak") { dialog, _ ->
                // Jika pengguna menolak, tutup dialog
                dialog.dismiss()
            }

            // Tampilkan dialog
            val dialog = builder.create()
            dialog.show()
        }

        binding.iconback.setOnClickListener {
            finish()
        }
        binding.cancelButton.setOnClickListener {
            pesananDibatalkan(id_pesanan.toString())
        }
    }


    private fun fetchMenuDetails(daftarMenu: ArrayList<MenuItem>) {
        val menuList = mutableListOf<DaftarMenu>()
        val adapter = MenuItemAdapter(menuList)
        binding.rvDetailMenus.adapter = adapter
        binding.rvDetailMenus.layoutManager = LinearLayoutManager(this)

        showLoading(true)
        for (menuItem in daftarMenu) {
            firestore.collection("menus").document(menuItem.id_menu!!)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val menu = document.toObject(Menu::class.java)
                        menu?.let {
                            val daftarMenuItem = DaftarMenu(
                                id_menu = it.id,
                                jumlah = menuItem.jumlah,
                                imgurl = it.img_uri,
                                nama = it.nama,
                                harga = it.harga
                            )
                            menuList.add(daftarMenuItem)
                            adapter.notifyDataSetChanged()  // Beritahu adapter bahwa ada perubahan
                            totalHarga += daftarMenuItem.harga.toString().toInt() * daftarMenuItem.jumlah.toString().toInt()
                            binding.totalHarga.text = "Total :  ${NumberFormat.getCurrencyInstance(
                                Locale("id", "ID")
                            ).format(totalHarga)} "
                        }
                        showLoading(false)
                    }
                }
                .addOnFailureListener { exception ->
                    binding.nama.text = "Error fetching data: ${exception.message}"
                    showLoading(false)
                }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun konfirmasi(id:String){
        showLoading(true)
        val historyData = HistoryPenjualan(
            id_penjualan = id,
            daftar_item = intent.getParcelableArrayListExtra("DAFTAR_MENU"),
            nama_pemesan = intent.getStringExtra("NAMA"),
            no_meja = intent.getStringExtra("NO_MEJA"),
            total_harga = totalHarga,
            timestamp = Date().time
        )

        // Menyimpan data ke koleksi `history_penjualan` di Firestore
        firestore.collection("history_penjualan").add(historyData)
            .addOnSuccessListener {documentReference ->

                val newid = documentReference.id
                // Buat objek Menu baru dengan ID yang dihasilkan
                val updatedHistoryPenjualan = historyData.copy(id_penjualan  = newid)

                // Update dokumen dengan ID yang benar
                firestore.collection("history_penjualan").document(newid).set(updatedHistoryPenjualan)
                    .addOnSuccessListener {
                        // Lanjutkan ke aktivitas berikutnya
                        deletePesanan(id)
                    }
                    .addOnFailureListener { error ->
                        showLoading(false)
                        Toast.makeText(this, "Menu Gagal Disimpan: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                // Lakukan tindakan lain jika perlu, misalnya menutup aktivitas atau menampilkan pesan sukses
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                binding.nama.text = "Gagal menyimpan data ke history: ${exception.message}"
            }
    }

    private fun deletePesanan(id: String){
        // Hapus data pesanan dari Firestore setelah konfirmasi selesai
        firestore.collection("pesanan").document(id)
            .delete()
            .addOnSuccessListener {
                showLoading(false)
                // Kembali ke halaman sebelumnya setelah berhasil
                finish() // atau bisa menggunakan onBackPressed()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                // Masih bisa kembali ke halaman sebelumnya meskipun gagal
                finish() // atau bisa menggunakan onBackPressed()
            }
    }

    private fun pesananDibatalkan(id: String){
        // Show a confirmation dialog first
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Konfirmasi Pesanan Dibatalkan")
            .setMessage("Apakah Anda yakin ingin menghapus pesanan ini?")
            .setPositiveButton("Ya") { _, _ ->
                // If the user confirms, proceed with the deletion
                showLoading(true)
                firestore.collection("pesanan").document(id)
                    .delete()
                    .addOnSuccessListener {
                        showLoading(false)
                        // Kembali ke halaman sebelumnya setelah berhasil
                        finish() // atau bisa menggunakan onBackPressed()
                    }
                    .addOnFailureListener { exception ->
                        showLoading(false)
                        // Masih bisa kembali ke halaman sebelumnya meskipun gagal
                        finish() // atau bisa menggunakan onBackPressed()
                    }
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                // If the user cancels, dismiss the dialog
                dialog.dismiss()
            }
            .create()

        // Show the dialog
        alertDialog.show()
    }

}