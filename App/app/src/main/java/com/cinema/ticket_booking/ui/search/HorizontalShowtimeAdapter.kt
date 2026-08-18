package com.cinema.ticket_booking.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.ShowtimeResponse

class HorizontalShowtimeAdapter(
    private val showtimes: List<ShowtimeResponse>,
    private val listener: (ShowtimeResponse) -> Unit
) : RecyclerView.Adapter<HorizontalShowtimeAdapter.ShowtimeChipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowtimeChipViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_showtime, parent, false)
        return ShowtimeChipViewHolder(v)
    }

    override fun onBindViewHolder(holder: ShowtimeChipViewHolder, position: Int) {
        val s = showtimes[position]
        holder.tvTime.text = formatTime(s.startTime)
        holder.itemView.setOnClickListener { listener(s) }
    }

    override fun getItemCount(): Int = showtimes.size

    private fun formatTime(s: String?): String? {
        if (s == null || !s.contains("T")) return s
        return try {
            s.split("T")[1].substring(0, 5)
        } catch (e: Exception) {
            s
        }
    }

    class ShowtimeChipViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }
}
