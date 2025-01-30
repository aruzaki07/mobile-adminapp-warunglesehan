package com.example.myresto.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myresto.R
import com.example.myresto.databinding.ItemMenuBinding
import com.example.myresto.model.Menu
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.text.NumberFormat
import java.util.Locale

class FirebaseMenuAdapter(
    options: FirestoreRecyclerOptions<Menu>,
    private val listener: OnMenuItemClickListener
):FirestoreRecyclerAdapter<Menu, FirebaseMenuAdapter.MenuViewHolder>(options) {

    interface OnMenuItemClickListener {
        fun onMenuItemClick(menu: Menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_menu, parent, false)
        val binding = ItemMenuBinding.bind(view)
        return  MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int, model: Menu) {
//        val reversedPosition = itemCount - 1 - position
        holder.bind(getItem(position))
    }

    inner class MenuViewHolder(private val binding: ItemMenuBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMenuItemClick(getItem(position))
                }
            }
        }

        fun bind(item: Menu){
            binding.nama.text = item.nama
            binding.harga.text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.harga)
            binding.stok.text = "Stok: ${item.stok.toString()}"

            Glide.with(itemView)
                .load(item.img_uri)
                .apply(RequestOptions().transform(RoundedCorners(70)))
                .into(binding.imageMenu)


        }

    }
}