package com.example.myresto.model

data class HistoryPenjualan(
    val id_penjualan: String? = null,
    val daftar_item: List<MenuItem>? = null,
    val nama_pemesan: String? = null,
    val no_meja: String? = null,
    val total_harga: Int? = null,
    val timestamp: Long? = null
)
