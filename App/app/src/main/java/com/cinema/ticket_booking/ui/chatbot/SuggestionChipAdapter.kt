package com.cinema.ticket_booking.ui.chatbot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.R

data class SuggestionPrompt(
    val icon: String,
    val label: String,
    val queryText: String
)

class SuggestionChipAdapter(
    private val prompts: List<SuggestionPrompt>,
    private val onPromptClick: (SuggestionPrompt) -> Unit
) : RecyclerView.Adapter<SuggestionChipAdapter.ViewHolder>() {

    companion object {
        fun getDefaultPrompts(): List<SuggestionPrompt> {
            return listOf(
                SuggestionPrompt("🎫", "Đặt vé nhanh", "Đặt vé"),
                SuggestionPrompt("⏰", "Đặt nhắc lịch", "Đặt nhắc lịch"),
                SuggestionPrompt("👤", "CinePoint & Vé", "Lịch sử đặt vé và điểm CinePoint"),
                SuggestionPrompt("🎬", "Phim đang chiếu", "Phim đang chiếu"),
                SuggestionPrompt("🎁", "Ưu đãi voucher", "Voucher khuyến mãi"),
                SuggestionPrompt("❓", "Chính sách hoàn vé", "Chính sách hoàn vé")
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(prompts[position])
    }

    override fun getItemCount(): Int = prompts.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tvIcon)
        private val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)

        fun bind(item: SuggestionPrompt) {
            tvIcon.text = item.icon
            tvLabel.text = item.label
            itemView.setOnClickListener { onPromptClick(item) }
        }
    }
}
