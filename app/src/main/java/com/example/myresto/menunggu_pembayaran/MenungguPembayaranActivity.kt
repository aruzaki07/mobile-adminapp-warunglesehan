package com.example.myresto.menunggu_pembayaran

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myresto.R
import com.example.myresto.databinding.ActivityMenungguPembayaranBinding
import com.example.myresto.model.Pesanan
import com.example.myresto.pesanan.DetailPesananActivity
import com.example.myresto.pesanan.FirebasePesananAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MenungguPembayaranActivity : AppCompatActivity(), FirebasePesananAdapter.OnPesananItemClickListener {
    private lateinit var binding:ActivityMenungguPembayaranBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: FirebasePesananAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenungguPembayaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Firebase.firestore

        val pesananRef = db.collection("pesanan")//tabel refrensi

        val manager = LinearLayoutManager(this)
        binding.rvMenunggupembayaran.layoutManager = manager

        val options = FirestoreRecyclerOptions.Builder<Pesanan>()
            .setQuery(pesananRef
                .whereEqualTo("diproses", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                , Pesanan::class.java)
            .build()

        adapter = FirebasePesananAdapter(options, this)
        binding.rvMenunggupembayaran.adapter = adapter
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
        val intent = Intent(this, DetailMenungguPembayaranActivity::class.java)
        intent.putExtra("ID", pesanan.id_pesanan)
        intent.putExtra("NAMA", pesanan.nama_pemesan)
        intent.putExtra("TOTAL_HARGA", pesanan.total_harga)
        intent.putExtra("NO_MEJA", pesanan.no_meja)
        intent.putExtra("DIPROSES", pesanan.diproses)
        val daftarItem = arrayListOf(pesanan.daftar_item)
        // Pastikan daftar_item adalah ArrayList<MenuItem>
        intent.putParcelableArrayListExtra("DAFTAR_MENU", ArrayList(pesanan.daftar_item))
        startActivity(intent)
    }
}