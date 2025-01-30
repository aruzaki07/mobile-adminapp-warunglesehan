package com.example.myresto.pesanan

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myresto.databinding.ActivityDetailPesananBinding
import com.example.myresto.model.DaftarMenu
import com.example.myresto.model.Menu
import com.example.myresto.model.MenuItem
import com.example.myresto.model.Pesanan
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject

class DetailPesananActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailPesananBinding
    private lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailPesananBinding.inflate(layoutInflater)
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
        if (diproses){
            binding.status.text = "Status : Pesanan Sedang di Proses"
        }else{
            binding.status.text = "Status : Pesanan Sudah di Meja"
        }




        if (daftarMenu != null) {
            fetchMenuDetails(daftarMenu)
        }else{
            binding.status.text = "GakJelas"
        }

        binding.SelesaiBut.setOnClickListener {
            updateStatus(id_pesanan.toString())
        }

        binding.iconback.setOnClickListener {
            finish()
        }

        binding.cancelButton.setOnClickListener {
            deletePesanan(id_pesanan.toString())
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

    private fun updateStatus(id: String?) {
        if (id != null) {
            showLoading(true)

            // Ambil dokumen pesanan
            firestore.collection("pesanan").document(id).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // Ambil data daftar_item yang ada
                        val data = documentSnapshot.toObject(Pesanan::class.java)
                        val updatedDaftarItem = data?.daftar_item?.map { item ->
                            // Menandai setiap item dalam daftar_item sebagai sudah sampai meja
                            item.copy(dimeja = true)
                        }

                        // Update status diproses menjadi false dan daftar_item
                        firestore.collection("pesanan").document(id)
                            .update(
                                "diproses", false,
                                "daftar_item", updatedDaftarItem
                            )
                            .addOnSuccessListener {
                                binding.status.text = "Status : Pesanan Sudah di Meja"
                                showLoading(false)
                            }
                            .addOnFailureListener { exception ->
                                binding.status.text = "Error updating status: ${exception.message} id nya = ${id}"
                                showLoading(false)
                            }
                    } else {
                        binding.status.text = "Pesanan tidak ditemukan"
                        showLoading(false)
                    }
                }
                .addOnFailureListener { exception ->
                    binding.status.text = "Error fetching data: ${exception.message}"
                    showLoading(false)
                }
        } else {
            binding.status.text = "ID pesanan tidak ditemukan"
        }
    }

    private fun deletePesanan(idPesanan:String?){
        showLoading(true)
        // Mengambil dokumen pesanan berdasarkan ID pesanan
        if (idPesanan != null) {
            firestore.collection("pesanan").document(idPesanan).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val pesanan = documentSnapshot.toObject(Pesanan::class.java)

                        // Mengecek apakah pesanan ada
                        pesanan?.let {
                            val daftarItem = it.daftar_item

                            // Mengembalikan stok menu untuk setiap item yang dibatalkan
                            if (daftarItem != null) {
                                for (item in daftarItem) {
                                    if (item.id_menu != null) {
                                        firestore.collection("menus").document(item.id_menu).get()
                                            .addOnSuccessListener { menuDocument ->
                                                val menu = menuDocument.toObject(Menu::class.java)
                                                menu?.let {
                                                    val updatedStock = it.stok?.plus(item.jumlah!!)

                                                    // Update stok di Firestore
                                                    it.id?.let { it1 ->
                                                        firestore.collection("menus").document(it1)
                                                            .update("stok", updatedStock)
                                                            .addOnSuccessListener {
                                                                // Setelah stok berhasil dikembalikan, hapus pesanan
                                                                firestore.collection("pesanan").document(idPesanan)
                                                                    .delete()
                                                                    .addOnSuccessListener {
                                                                        binding.status.text = "Pesanan berhasil dibatalkan dan stok dikembalikan"
                                                                        showLoading(false)
                                                                        finish() // Menutup activity setelah pembatalan
                                                                    }
                                                                    .addOnFailureListener { exception ->
                                                                        binding.status.text = "Gagal menghapus pesanan: ${exception.message}"
                                                                        showLoading(false)
                                                                    }
                                                            }
                                                            .addOnFailureListener { exception ->
                                                                binding.status.text = "Gagal mengupdate stok: ${exception.message}"
                                                                showLoading(false)
                                                            }
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { exception ->
                                                binding.status.text = "Gagal mengambil data menu: ${exception.message}"
                                                showLoading(false)
                                            }
                                    }
                                }
                            }
                        }
                    } else {
                        binding.status.text = "Pesanan tidak ditemukan"
                        showLoading(false)
                    }
                }
                .addOnFailureListener { exception ->
                    binding.status.text = "Error fetching pesanan: ${exception.message}"
                    showLoading(false)
                }
        } else {
            binding.status.text = "ID pesanan tidak ditemukan"
            showLoading(false)
        }
    }

}
