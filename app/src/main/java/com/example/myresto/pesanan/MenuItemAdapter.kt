package com.example.myresto.pesanan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myresto.databinding.ItemDetailMenuBinding
import com.example.myresto.databinding.ItemMenuBinding
import com.example.myresto.model.DaftarMenu
import com.example.myresto.model.MenuItem
import java.text.NumberFormat
import java.util.Locale

class MenuItemAdapter(private val menuItems: List<DaftarMenu>) : RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder>() {



    inner class MenuItemViewHolder(private val binding: ItemDetailMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(menuItem: DaftarMenu) {
            binding.nama.text = menuItem.nama
            binding.jumlah.text = "x ${menuItem.jumlah.toString()}"
            binding.harga.text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(menuItem.harga)
            Glide.with(itemView)
                .load(menuItem.imgurl)
                .apply(RequestOptions().transform(RoundedCorners(70)))
                .into(binding.imageMenu)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDetailMenuBinding.inflate(inflater, parent, false)
        return MenuItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size
}