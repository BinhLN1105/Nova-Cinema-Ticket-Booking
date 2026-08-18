package com.cinema.ticket_booking.ui.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.ShowtimeResponse

class ShowtimeAdapter(
    private val items: List<ShowtimeResponse>,
    private val listener: (ShowtimeResponse) -> Unit
) : RecyclerView.Adapter<ShowtimeAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_showtime, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvTime: TextView = v.findViewById(R.id.tvTime)
        private val tvType: TextView = v.findViewById(R.id.tvType)
        private val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        private val tvSeats: TextView = v.findViewById(R.id.tvSeats)

        fun bind(s: ShowtimeResponse) {
            var time = s.startTime
            if (time != null && time.length >= 16) {
                time = time.substring(11, 16)
            }
            tvTime.text = time
            tvType.text = s.screenType
            tvType.visibility = View.VISIBLE
            tvPrice.text = String.format("%,.0fđ", s.basePrice)
            tvPrice.visibility = View.VISIBLE
            tvSeats.text = "${s.availableSeats} ghế trống"
            tvSeats.visibility = View.VISIBLE
            itemView.alpha = if ("CANCELLED" == s.status) 0.4f else 1.0f
            itemView.isEnabled = "CANCELLED" != s.status
            itemView.setOnClickListener { listener(s) }
        }
    }
}
