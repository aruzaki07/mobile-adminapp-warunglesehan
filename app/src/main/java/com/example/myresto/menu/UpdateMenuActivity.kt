package com.example.myresto.menu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myresto.R
import com.example.myresto.databinding.ActivityUpdateMenuBinding
import com.example.myresto.model.Menu
import com.example.myresto.utils.getImageUri
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.Date

class UpdateMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateMenuBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var currentImageUri: Uri? = null
    private  lateinit var menu_id: String

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ){isGranted : Boolean ->
            if (isGranted){
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
            }else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun allPermissionGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.butDelete.setOnClickListener {
            delete()
        }


        if (!allPermissionGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }


        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        binding.cammeraButton.setOnClickListener {
            if (!allPermissionGranted()) {
                requestPermissionLauncher.launch(REQUIRED_PERMISSION)
            }else{
                startCamera()
            }
        }

        binding.iconback.setOnClickListener {
            finish()
        }

        var name = intent.getStringExtra("NAMA")
        var harga = intent.getStringExtra("HARGA")
        var jenis = intent.getStringExtra("JENIS")
        var stok  = intent.getStringExtra("STOK")
        var img_url = intent.getStringExtra("IMAGE")
        var id_menu = intent.getStringExtra("ID")

        menu_id = id_menu.toString()

        binding.inputName.setText(name)
        binding.inputHarga.setText(harga)
        binding.inputStok.setText(stok)

        Glide.with(binding.root)
            .load(img_url)
            .apply(RequestOptions().transform(RoundedCorners(70)))
            .into(binding.previewImageView)

        val inputJenis: Spinner = findViewById(R.id.inputJenis)
        val jenisArray = arrayOf("Pilih Jenis Menu", "Makanan", "Minuman") // Menambahkan hint

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, jenisArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inputJenis.adapter = adapter

        // Set default selection to the first item (hint)
        val jenisIndex = jenisArray.indexOf(jenis)
        inputJenis.setSelection(jenisIndex)


        binding.buttperbarui.setOnClickListener {
            if (binding.inputName.text.isEmpty() || binding.inputHarga.text.isEmpty() || binding.inputStok.text.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Keluar dari listener jika ada yang kosong
            }else if (binding.inputJenis.selectedItem == "Pilih Jenis Menu"){
                Toast.makeText(this, "Silakan pilih jenis menu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Keluar dari listener jika ada yang kosong
            }else if(currentImageUri == null && img_url == null){
                Toast.makeText(this, "Silakan masukan foto menu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Keluar dari listener jika ada yang kosong
            }else if (binding.inputHarga.text.toString().toIntOrNull() == null || binding.inputStok.text.toString().toIntOrNull() == null){
                Toast.makeText(this, "Silakan masukan harga dan stok dalam format angka", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Keluar dari listener jika ada yang kosong
            }

            showLoading(true)
            update(id_menu.toString(), img_url.toString())


        }


    }


    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.item_delete, menu)
        return true
    }



    private  fun delete(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi Penghapusan")
        builder.setMessage("Apakah Anda yakin ingin menghapus menu ini?")

        builder.setPositiveButton("Ya") { dialog, which ->
            showLoading(true)
            db = Firebase.firestore
            storage = Firebase.storage

            val menuRef = db.collection("menus")
            val img_menuRef = storage.getReference("menu_img")

            img_menuRef.child(menu_id).delete().addOnSuccessListener {
                menuRef.document(menu_id).delete()
                    .addOnSuccessListener {
                        showLoading(false)
                        Log.d("UpdateMenuActivity", "Menu updated successfully with ID: $menu_id")
                        Toast.makeText(this, "Menu Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {error ->
                        showLoading(false)
                        Log.d("UpdateMenuActivity", "Menu updated gagal with ID: $menu_id")
                        Toast.makeText(this, "Gagal Menghapus Menu" + error.message, Toast.LENGTH_SHORT).show()
                    }
            }
                .addOnFailureListener {error ->
                    showLoading(false)
                    Log.d("UpdateMenuActivity", "Menu updated gagal with ID: $menu_id")
                    Toast.makeText(this, "Gagal Menghapus Gambar Menu" + error.message, Toast.LENGTH_SHORT).show()
                }



        }

        builder.setNegativeButton("Tidak") { dialog, which ->
            // Jika pengguna membatalkan, tutup dialog
            dialog.dismiss()
        }
        builder.show()
    }

    private fun update(id_menu: String, img_url:String){
        db = Firebase.firestore
        storage = Firebase.storage

        val menuRef = db.collection("menus")
        val img_menuRef = storage.getReference("menu_img")

        if (currentImageUri != null){
            currentImageUri?.let {
                img_menuRef.child(id_menu).putFile(it)
                    .addOnSuccessListener { task ->
                        task.metadata!!.reference!!.downloadUrl
                            .addOnSuccessListener { url ->
                                val imgUrl = url.toString()


                                val menu_baru = Menu(
                                    id  =   id_menu,
                                    nama =  binding.inputName.text.toString(),
                                    jenis = binding.inputJenis.selectedItem.toString(),
                                    harga = binding.inputHarga.text.toString().toInt(),
                                    stok = binding.inputStok.text.toString().toInt(),
                                    img_uri = imgUrl,
                                    timestamp = Date().time
                                )

                                menuRef.document(id_menu).set(menu_baru)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Menu Berhasil Diperbarui", Toast.LENGTH_SHORT).show()
                                        // Lanjutkan ke aktivitas berikutnya
                                        finish()
                                    }
                                    .addOnFailureListener { error ->
                                        Toast.makeText(this, "Menu Gagal Diperbarui: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }

                            }
                    }
            }
        }else{
            val menu_baru = Menu(
                id = id_menu,
                nama =  binding.inputName.text.toString(),
                jenis = binding.inputJenis.selectedItem.toString(),
                harga = binding.inputHarga.text.toString().toInt(),
                stok = binding.inputStok.text.toString().toInt(),
                img_uri = img_url,
                timestamp = Date().time
            )

            menuRef.document(id_menu).set(menu_baru)
                .addOnSuccessListener {
                    Toast.makeText(this, "Menu Berhasil Diperbarui", Toast.LENGTH_SHORT).show()
                    // Lanjutkan ke aktivitas berikutnya
                    finish()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Menu Gagal Diperbarui: ${error.message}", Toast.LENGTH_SHORT).show()
                }

        }
    }


    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ){isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }


    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ){uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        }else {
            Log.d("Photo Picker", "No media selected")
        }
    }


    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            Glide.with(binding.root)
                .load(it)
                .apply(RequestOptions().transform(RoundedCorners(70)))
                .into(binding.previewImageView)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar .visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
}