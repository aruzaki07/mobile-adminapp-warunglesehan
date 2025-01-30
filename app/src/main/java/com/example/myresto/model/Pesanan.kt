package com.example.myresto.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Date


@Parcelize
data class MenuItem(
    val id_menu: String? = null,
    val jumlah: Int? = null,
    val dimeja: Boolean? = null
):Parcelable

data class Pesanan(
    val id_pesanan: String? = null,
    val daftar_item: List<MenuItem>? = null,
    val diproses: Boolean? = null,
    val nama_pemesan: String? = null,
    val no_meja: String? = null,
    val total_harga: Int? = null,
    val timestamp: Long? = null
) {
}