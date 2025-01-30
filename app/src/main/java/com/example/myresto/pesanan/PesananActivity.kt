package com.example.myresto.pesanan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myresto.databinding.ActivityPesananBinding
import com.example.myresto.model.Pesanan
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PesananActivity : AppCompatActivity(), FirebasePesananAdapter.OnPesananItemClickListener{
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: FirebasePesananAdapter
    private lateinit var binding: ActivityPesananBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPesananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Firebase.firestore

        val pesananRef = db.collection("pesanan")//tabel refrensi

        val manager = LinearLayoutManager(this)
        binding.rvPesanan.layoutManager = manager

        val options = FirestoreRecyclerOptions.Builder<Pesanan>()
            .setQuery(pesananRef
                .whereEqualTo("diproses", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                , Pesanan::class.java)
            .build()

        adapter = FirebasePesananAdapter(options, this)
        binding.rvPesanan.adapter = adapter

        binding.iconback.setOnClickListener {
            finish()
        }

    }


    public override fun onResume() {
        super.onResume()
        db.collection("pesanan")
            .addSnapshotListener { snapshots, e ->
                showLoading(false)
                if (e != null) {
                    Log.w("MyListMenuActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // Anda bisa memperbarui adapter di sini jika menggunakan FirestoreRecyclerAdapter
                    adapter.notifyDataSetChanged()
                }
            }
        adapter.startListening()

    }

    public override fun onPause() {
        adapter.stopListening()
        super.onPause()
    }



    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }


    override fun onMenuItemClick(pesanan: Pesanan) {
        // Filter daftar_item yang dimeja = false
        val daftarItemYangBelumSampaiMeja = pesanan.daftar_item?.filter { it.dimeja == false }

        // Cek apakah ada item yang belum sampai meja
        if (daftarItemYangBelumSampaiMeja != null) {
            if (daftarItemYangBelumSampaiMeja.isNotEmpty()) {
                val intent = Intent(this, DetailPesananActivity::class.java)
                intent.putExtra("ID", pesanan.id_pesanan)
                intent.putExtra("NAMA", pesanan.nama_pemesan)
                intent.putExtra("TOTAL_HARGA", pesanan.total_harga)
                intent.putExtra("NO_MEJA", pesanan.no_meja)
                intent.putExtra("DIPROSES", pesanan.diproses)

                // Kirim hanya daftar item yang dimeja = false
                intent.putParcelableArrayListExtra("DAFTAR_MENU", ArrayList(daftarItemYangBelumSampaiMeja))

                startActivity(intent)
            } else {
                // Jika tidak ada item yang dimeja = false, mungkin bisa menunjukkan pesan bahwa semua sudah sampai meja
                showMessage("Semua pesanan sudah sampai meja!")
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}