package com.cinema.ticket_booking.ui.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.data.model.response.NotificationResponse
import com.cinema.ticket_booking.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val items: MutableList<NotificationResponse>
) : RecyclerView.Adapter<NotificationAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(pos: Int) {
        items.removeAt(pos)
        notifyItemRemoved(pos)
    }

    fun getItem(pos: Int): NotificationResponse {
        return items[pos]
    }

    inner class VH(val b: ItemNotificationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(n: NotificationResponse) {
            b.tvTitle.text = n.title
            b.tvBody.text = n.body
            b.tvTime.text = formatDate(n.sentAt)
            b.root.alpha = if (n.isRead) 0.6f else 1f
        }

        private fun formatDate(rawDate: String?): String {
            if (rawDate == null) return ""
            return try {
                var cleanDate = rawDate
                if (rawDate.contains(".")) {
                    cleanDate = rawDate.substring(0, rawDate.indexOf("."))
                }

                val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = sdfIn.parse(cleanDate) ?: return rawDate

                val sdfOut = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                sdfOut.format(date)
            } catch (e: Exception) {
                rawDate
            }
        }
    }
}
