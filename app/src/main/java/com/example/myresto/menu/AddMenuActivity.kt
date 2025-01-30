package com.example.myresto.menu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myresto.R
import com.example.myresto.databinding.ActivityAddMenuBinding
import com.example.myresto.model.Menu
import com.example.myresto.utils.getImageUri
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.Date
import java.util.UUID

class AddMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMenuBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var currentImageUri: Uri? = null

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
        binding = ActivityAddMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)


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

        val inputJenis: Spinner = findViewById(R.id.inputJenis)
        val jenisArray = arrayOf("Pilih Jenis Menu", "Makanan", "Minuman") // Menambahkan hint

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, jenisArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inputJenis.adapter = adapter

        // Set default selection to the first item (hint)
        inputJenis.setSelection(0)


        db = FirebaseFirestore.getInstance()
        storage = Firebase.storage

        val menuRef = db.collection("menus")//tabel refrensi
        val img_menuRef = storage.reference.child("menu_img")

        binding.iconback.setOnClickListener {
            finish()
        }


        binding.buttambahkan.setOnClickListener {
            // Validasi input
            if (binding.inputName.text.isEmpty() || binding.inputHarga.text.isEmpty() || binding.inputStok.text.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Keluar dari listener jika ada yang kosong
            }else if (binding.inputJenis.selectedItem == "Pilih Jenis Menu"){
                Toast.makeText(this, "Silakan pilih jenis menu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Keluar dari listener jika ada yang kosong
            }else if(currentImageUri == null){
                Toast.makeText(this, "Silakan masukan foto menu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Keluar dari listener jika ada yang kosong
            }else if (binding.inputHarga.text.toString().toIntOrNull() == null || binding.inputStok.text.toString().toIntOrNull() == null){
                Toast.makeText(this, "Silakan masukan harga dan stok dalam format angka", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Keluar dari listener jika ada yang kosong
            }

            showLoading(true)
            val menu_baru = Menu(
                "",
                binding.inputName.text.toString(),
                binding.inputJenis.selectedItem.toString(),
                binding.inputHarga.text.toString().toInt(),
                binding.inputStok.text.toString().toInt(),
                "",  // Initially set the image URL as empty
                Date().time
            )

            menuRef.add(menu_baru)
                .addOnSuccessListener { documentReference ->
                    // Get the generated document ID
                    val menuId = documentReference.id

                    // Now upload the image to Firebase Storage using the same menuId as the file name
                    currentImageUri?.let {
                        img_menuRef.child(menuId).putFile(it)
                            .addOnSuccessListener { task ->
                                // Once the image is uploaded successfully, retrieve its URL
                                task.metadata?.reference?.downloadUrl
                                    ?.addOnSuccessListener { url ->
                                        val imgUrl = url.toString()  // Get the image URL

                                        // Update the menu object with the image URL and the generated menu ID
                                        val updatedMenu = menu_baru.copy(id = menuId, img_uri = imgUrl)

                                        // Now update the document with the correct image URL and ID
                                        menuRef.document(menuId).set(updatedMenu)
                                            .addOnSuccessListener {
                                                // Successfully updated the menu document
                                                Toast.makeText(this, "Menu Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                                                finish()  // Optionally, you can navigate to another activity or finish the current one
                                            }
                                            .addOnFailureListener { error ->
                                                Toast.makeText(this, "Menu Gagal Disimpan: ${error.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    ?.addOnFailureListener { error ->
                                        // Failed to get the download URL of the image
                                        showLoading(false)
                                        Toast.makeText(this, "Gagal Mendapatkan URL Gambar: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { error ->
                                // Failed to upload the image
                                showLoading(false)
                                Toast.makeText(this, "Gagal Mengupload Gambar: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { error ->
                    // Failed to add the menu document to Firestore
                    showLoading(false)
                    Toast.makeText(this, "Gagal Menambahkan Menu: ${error.message}", Toast.LENGTH_SHORT).show()
                }


        }
    }


    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        } else {
            // Jika pengambilan gambar tidak berhasil, atur currentImageUri ke null
            currentImageUri = null
            Toast.makeText(this, "Pengambilan gambar dibatalkan", Toast.LENGTH_SHORT).show()
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