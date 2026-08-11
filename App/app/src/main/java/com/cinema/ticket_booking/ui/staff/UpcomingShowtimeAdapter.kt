package com.cinema.ticket_booking.ui.staff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.UpcomingShowtimeResponse
import java.util.ArrayList

class UpcomingShowtimeAdapter : RecyclerView.Adapter<UpcomingShowtimeAdapter.ViewHolder>() {

    private var items: List<UpcomingShowtimeResponse> = ArrayList()

    fun submitList(list: List<UpcomingShowtimeResponse>?) {
        this.items = list ?: ArrayList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_upcoming_showtime, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvMovieTitle.text = item.movieTitle
        holder.tvScreenName.text = "🎬 " + item.screenName

        // Parse startTime "HH:mm" from ISO string
        var timeDisplay = "--:--"
        val startTime = item.startTime
        if (startTime != null && startTime.length >= 16) {
            timeDisplay = startTime.substring(11, 16) // "HH:mm"
        }
        holder.tvStartTime.text = timeDisplay

        // Badge urgency
        if (item.minutesUntilStart <= 0) {
            holder.tvUrgency.text = "Bắt đầu!"
            holder.tvUrgency.setTextColor(0xFFEF4444.toInt())
        } else if ("SOON" == item.urgency) {
            holder.tvUrgency.text = "${item.minutesUntilStart} phút"
            holder.tvUrgency.setTextColor(0xFFEF4444.toInt()) // Red khi gấp
        } else {
            holder.tvUrgency.text = "${item.minutesUntilStart} phút"
            holder.tvUrgency.setTextColor(0xFFF5C518.toInt()) // Gold khi còn thời gian
        }

        // Load poster
        val moviePosterUrl = item.moviePosterUrl
        if (!moviePosterUrl.isNullOrEmpty()) {
            Glide.with(holder.ivPoster.context)
                .load(moviePosterUrl)
                .placeholder(R.color.surface)
                .centerCrop()
                .into(holder.ivPoster)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPoster: ImageView = itemView.findViewById(R.id.ivPoster)
        val tvMovieTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)
        val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
        val tvScreenName: TextView = itemView.findViewById(R.id.tvScreenName)
        val tvUrgency: TextView = itemView.findViewById(R.id.tvUrgency)
    }
}
