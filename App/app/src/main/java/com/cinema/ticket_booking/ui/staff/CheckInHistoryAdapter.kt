package com.cinema.ticket_booking.ui.staff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.CheckInHistoryItemResponse
import java.util.ArrayList

class CheckInHistoryAdapter : RecyclerView.Adapter<CheckInHistoryAdapter.ViewHolder>() {

    private var items: List<CheckInHistoryItemResponse> = ArrayList()

    fun submitList(list: List<CheckInHistoryItemResponse>?) {
        this.items = list ?: ArrayList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_check_in_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Movie title
        holder.tvMovieTitle.text = item.movieTitle ?: "Không xác định"

        // Cinema name
        holder.tvCinemaName.text = item.cinemaName ?: "Hệ thống Nova"

        // Screen + Seats
        var screenSeats = ""
        if (item.screenName != null) screenSeats += item.screenName
        if (!item.seatsChecked.isNullOrEmpty()) {
            screenSeats += " • " + item.seatsChecked
        }
        holder.tvScreenAndSeats.text = if (screenSeats.isEmpty()) "-" else screenSeats

        // Booking code
        holder.tvBookingCode.text = if (item.bookingCode != null) "#" + item.bookingCode else ""

        // Customer name
        holder.tvCustomerName.text = item.customerName ?: "Khách vãng lai"

        // Time
        var timeDisplay = "--:--"
        val scannedAt = item.scannedAt
        if (scannedAt != null && scannedAt.length >= 16) {
            timeDisplay = scannedAt.substring(11, 16)
        }
        holder.tvScannedAt.text = timeDisplay

        // Status badge
        if (item.success) {
            holder.tvStatusBadge.text = "✓ Thành công"
            holder.tvStatusBadge.setTextColor(0xFF22C55E.toInt())
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_success)
            holder.tvFailReason.visibility = View.GONE
        } else {
            holder.tvStatusBadge.text = "✗ Thất bại"
            holder.tvStatusBadge.setTextColor(0xFFEF4444.toInt())
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_fail)
            val failReason = item.failReason
            if (!failReason.isNullOrEmpty()) {
                holder.tvFailReason.visibility = View.VISIBLE
                holder.tvFailReason.text = "↳ $failReason"
            } else {
                holder.tvFailReason.visibility = View.GONE
            }
        }

        // Movie poster
        val moviePosterUrl = item.moviePosterUrl
        if (!moviePosterUrl.isNullOrEmpty()) {
            Glide.with(holder.ivMoviePoster.context)
                .load(moviePosterUrl)
                .placeholder(R.color.surface)
                .centerCrop()
                .into(holder.ivMoviePoster)
        } else {
            holder.ivMoviePoster.setImageResource(R.color.surface)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivMoviePoster: ImageView = itemView.findViewById(R.id.ivMoviePoster)
        val tvMovieTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)
        val tvCinemaName: TextView = itemView.findViewById(R.id.tvCinemaName)
        val tvScreenAndSeats: TextView = itemView.findViewById(R.id.tvScreenAndSeats)
        val tvBookingCode: TextView = itemView.findViewById(R.id.tvBookingCode)
        val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        val tvScannedAt: TextView = itemView.findViewById(R.id.tvScannedAt)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvFailReason: TextView = itemView.findViewById(R.id.tvFailReason)
    }
}
