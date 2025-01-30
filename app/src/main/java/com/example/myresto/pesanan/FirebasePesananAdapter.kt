package com.example.myresto.pesanan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myresto.R
import com.example.myresto.databinding.ItemMenuBinding
import com.example.myresto.databinding.ItemPesananBinding
import com.example.myresto.menu.FirebaseMenuAdapter
import com.example.myresto.model.Menu
import com.example.myresto.model.Pesanan
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.text.NumberFormat
import java.util.Locale

class FirebasePesananAdapter(
    options: FirestoreRecyclerOptions<Pesanan>,
    private val listener: FirebasePesananAdapter.OnPesananItemClickListener
): FirestoreRecyclerAdapter<Pesanan, FirebasePesananAdapter.PesananViewHolder>(options) {

    interface OnPesananItemClickListener {
        fun onMenuItemClick(pesanan: Pesanan )
    }

    inner class PesananViewHolder(private val binding: ItemPesananBinding) : RecyclerView.ViewHolder(binding.root) {

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
            binding.jumlah.text ="${item.daftar_item?.size.toString()} Menu"
            binding.noMeja.text = "No Meja : ${item.no_meja}"
            binding.totalHarga.text = "Total : ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.total_harga)}"
//            binding.nama.text = item.nama
//            binding.harga.text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.harga)
//            binding.stok.text = "Stok: ${item.stok.toString()}"
//
//            Glide.with(itemView)
//                .load(item.img_uri)
//                .apply(RequestOptions().transform(RoundedCorners(70)))
//                .into(binding.imageMenu)


        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PesananViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_pesanan, parent, false)
        val binding = ItemPesananBinding.bind(view)
        return  PesananViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PesananViewHolder, position: Int, model: Pesanan) {
        holder.bind(getItem(position))
    }
}