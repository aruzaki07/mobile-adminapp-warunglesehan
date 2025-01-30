package com.example.myresto.historyPenjualan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myresto.R
import com.example.myresto.databinding.ItemHistorypenjualanBinding
import com.example.myresto.model.Pesanan
import com.example.myresto.pesanan.FirebasePesananAdapter
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseHistoryPenjualanAdapter(
    options: FirestoreRecyclerOptions<Pesanan>,
    private val listener: FirebaseHistoryPenjualanAdapter.OnPesananItemClickListener
): FirestoreRecyclerAdapter<Pesanan, FirebaseHistoryPenjualanAdapter.HistoryPenjualanViewHolder>(options) {

    interface OnPesananItemClickListener {
        fun onMenuItemClick(pesanan: Pesanan )
    }

    inner class HistoryPenjualanViewHolder(private val binding: ItemHistorypenjualanBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMenuItemClick(getItem(position))
                }
            }
        }

        fun bind(item: Pesanan){
            binding.nama.text = "Nama Pemesan : ${item.nama_pemesan}"
            binding.jumlahmenu.text ="${item.daftar_item?.size.toString()} Menu"
            val timestamp = item.timestamp // pastikan ini dalam milidetik
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val date = timestamp?.let { Date(it) }
            val formattedDate = sdf.format(date)
            binding.waktu.text = "Waktu : ${formattedDate}"
            binding.totalHarga.text = "Total : ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.total_harga)}"


        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryPenjualanViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_historypenjualan, parent, false)
        val binding = ItemHistorypenjualanBinding.bind(view)
        return  HistoryPenjualanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryPenjualanViewHolder, position: Int, model: Pesanan) {
        holder.bind(getItem(position))
    }
}