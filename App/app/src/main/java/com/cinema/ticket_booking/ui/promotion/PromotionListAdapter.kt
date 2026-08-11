package com.cinema.ticket_booking.ui.promotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.PromotionResponse
import com.cinema.ticket_booking.databinding.ItemPromotionCardBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PromotionListAdapter(
    private val promotions: List<PromotionResponse>,
    private val listener: OnPromotionClickListener
) : RecyclerView.Adapter<PromotionListAdapter.ViewHolder>() {

    interface OnPromotionClickListener {
        fun onPromotionClick(promotion: PromotionResponse)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPromotionCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val promotion = promotions[position]
        holder.bind(promotion)
    }

    override fun getItemCount(): Int = promotions.size

    inner class ViewHolder(
        private val binding: ItemPromotionCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(promotion: PromotionResponse) {
            binding.tvPromoTitle.text = promotion.title
            binding.tvPromoDescription.text = promotion.description

            // Format date safely
            var formattedDate = "N/A"
            promotion.endDate?.let { endDate ->
                try {
                    val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val sdfOut = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = sdfIn.parse(endDate)
                    if (date != null) {
                        formattedDate = sdfOut.format(date)
                    }
                } catch (e: ParseException) {
                    // Fallback to substring if parsing fails
                    if (endDate.length >= 10) {
                        formattedDate = endDate.substring(0, 10)
                    }
                }
            }
            binding.tvPromoDate.text = "HSD: $formattedDate"

            Glide.with(itemView.context)
                .load(promotion.imageUrl)
                .placeholder(R.drawable.bg_placeholder_banner)
                .error(R.drawable.bg_placeholder_banner)
                .centerCrop()
                .into(binding.ivPromoBanner)

            itemView.setOnClickListener {
                listener.onPromotionClick(promotion)
            }

            binding.btnPromoDetail.setOnClickListener {
                listener.onPromotionClick(promotion)
            }
        }
    }
}
