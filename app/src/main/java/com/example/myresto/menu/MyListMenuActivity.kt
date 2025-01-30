package com.example.myresto.menu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myresto.databinding.ActivityMyListMenuBinding
import com.example.myresto.model.Menu
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyListMenuActivity : AppCompatActivity(), FirebaseMenuAdapter.OnMenuItemClickListener {
    private lateinit var binding: ActivityMyListMenuBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: FirebaseMenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyListMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = Firebase.firestore

        val menuRef = db.collection("menus")

        val manager = LinearLayoutManager(this)
        binding.rvMenus.layoutManager = manager

        val options = FirestoreRecyclerOptions.Builder<Menu>()
            .setQuery(menuRef.orderBy("timestamp", Query.Direction.DESCENDING), Menu::class.java)
            .build()

        adapter = FirebaseMenuAdapter(options, this)
        binding.rvMenus.adapter = adapter

        binding.iconback.setOnClickListener {
            finish()
        }
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this@MyListMenuActivity, AddMenuActivity::class.java)
            startActivity(intent)
        }
    }

    public override fun onResume() {
        super.onResume()
        showLoading(true)
        db.collection("menus")
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
        binding.progressBar .visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onMenuItemClick(menu:Menu){
        // Handle the click event here
        // You can start a new activity or show a dialog with menu details
        val intent = Intent(this, UpdateMenuActivity::class.java)
        intent.putExtra("ID", menu.id)
        intent.putExtra("NAMA", menu.nama)
        intent.putExtra("HARGA", menu.harga.toString())
        intent.putExtra("JENIS", menu.jenis)
        intent.putExtra("STOK", menu.stok.toString())
        intent.putExtra("IMAGE", menu.img_uri)
        startActivity(intent)
    }


}