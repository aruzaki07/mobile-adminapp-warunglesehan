package com.example.myresto.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Menu(
    val id: String? = null,
    val nama: String? = null,
    val jenis: String? = null,
    val harga: Int? = null,
    val stok: Int? = null,
    val img_uri: String? = null,
    val timestamp: Long? = null
): Parcelable