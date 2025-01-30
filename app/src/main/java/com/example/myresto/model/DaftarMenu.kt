package com.example.myresto.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DaftarMenu(
    val id_menu: String? = null,
    val jumlah: Int? = null,
    val imgurl: String? = null,
    val nama: String? = null,
    val harga: Int? = null
):Parcelable
