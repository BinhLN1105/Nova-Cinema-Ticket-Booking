package com.cinema.ticket_booking.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.ComboResponse
import com.cinema.ticket_booking.databinding.ItemComboBinding

class ComboAdapter(
    private val items: List<ComboResponse>,
    private val onAdd: (String) -> Unit,
    private val onRemove: (String) -> Unit,
    private val qtys: Map<String, Int>
) : RecyclerView.Adapter<ComboAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemComboBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(val b: ItemComboBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: ComboResponse) {
            b.tvComboName.text = c.name
            b.tvComboDesc.text = c.description
            b.tvComboPrice.text = String.format("%,.0fđ", c.price)
            Glide.with(b.ivCombo.context)
                .load(c.imageUrl)
                .placeholder(R.drawable.ic_movie_placeholder)
                .into(b.ivCombo)
            val id = c.id ?: ""
            val qty = qtys[id] ?: 0
            b.tvQty.text = qty.toString()
            b.btnMinus.setOnClickListener {
                onRemove(id)
                notifyItemChanged(adapterPosition)
            }
            b.btnPlus.setOnClickListener {
                onAdd(id)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}
