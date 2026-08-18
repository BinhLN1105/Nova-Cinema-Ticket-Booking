package com.cinema.ticket_booking.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.PromotionResponse

class PromotionAdapter(
    private val promotions: List<PromotionResponse>,
    private val listener: (PromotionResponse) -> Unit
) : RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_promotion_banner, parent, false)
        return PromotionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromotionViewHolder, position: Int) {
        val promotion = promotions[position]
        Glide.with(holder.itemView.context)
            .load(promotion.imageUrl)
            .centerCrop()
            .placeholder(R.drawable.placeholder_hero)
            .into(holder.ivBanner)

        holder.itemView.setOnClickListener {
            listener(promotion)
        }
    }

    override fun getItemCount(): Int = promotions.size

    class PromotionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivBanner: ImageView = itemView.findViewById(R.id.ivBannerImage)
    }
}
