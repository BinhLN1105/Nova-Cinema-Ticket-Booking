package com.cinema.ticket_booking.ui.booking

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.BookingSummary
import com.cinema.ticket_booking.databinding.ItemBookingBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingHistoryAdapter(
    private val items: List<BookingSummary>,
    private val listener: Listener
) : RecyclerView.Adapter<BookingHistoryAdapter.VH>() {

    interface Listener {
        fun onClick(bookingId: String)
        fun onPayClick(bookingId: String)
        fun onReviewClick(s: BookingSummary)
        fun onCancelClick(s: BookingSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(val b: ItemBookingBinding) : RecyclerView.ViewHolder(b.root) {
        private var countDownTimer: CountDownTimer? = null

        fun bind(s: BookingSummary) {
            val bookingId = s.id ?: ""
            countDownTimer?.cancel()
            b.tvMovieTitle.text = s.movieTitle
            b.tvCinema.text = "CineNoir ${s.cinemaName ?: ""}"
            b.tvFormat.text = formatScreenType(s.screenType)

            // Format time
            val rawTime = s.startTime
            if (rawTime != null) {
                try {
                    val sdfIn = if (rawTime.contains("T")) {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    } else {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    }
                    val d = sdfIn.parse(rawTime)
                    if (d != null) {
                        val sdfOut = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
                        b.tvShowtime.text = sdfOut.format(d)
                    }
                } catch (e: Exception) {
                    b.tvShowtime.text = rawTime
                }
            } else {
                b.tvShowtime.text = "N/A"
            }

            // Format Seat
            val screen = s.screenName ?: ""
            val seats = s.seats ?: ""

            if (seats.isEmpty() && screen.isEmpty()) {
                b.tvSeat.text = "Mã vé: ${s.bookingCode ?: "N/A"}"
            } else {
                b.tvSeat.text = "$screen - $seats"
            }

            Glide.with(b.ivPoster.context).load(s.moviePosterUrl)
                .placeholder(R.drawable.ic_movie_placeholder).into(b.ivPoster)

            // Display Status Tag
            val status = s.status?.uppercase(Locale.getDefault()) ?: "UNKNOWN"
            b.tvStatus.text = formatStatusText(status)
            b.tvStatus.backgroundTintList = ColorStateList.valueOf(getStatusColor(b.root.context, status))

            if ("PENDING".equals(s.status, ignoreCase = true)) {
                b.btnReview.visibility = View.GONE

                // Kiểm tra vé PENDING đã quá hạn thanh toán chưa
                var isExpired = false
                if (s.expiresAt != null) {
                    try {
                        val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val expDate = sdfIn.parse(s.expiresAt)
                        if (expDate != null && expDate.time <= System.currentTimeMillis()) {
                            isExpired = true
                        }
                    } catch (ignored: Exception) {
                    }
                }

                if (isExpired) {
                    // Vé PENDING đã quá hạn: hiển thị nút "ĐÃ HẾT HẠN" bị vô hiệu hoá
                    b.tvCountdown.visibility = View.VISIBLE
                    b.tvCountdown.text = "Đã quá hạn"
                    b.btnViewTicket.text = "ĐÃ HẾT HẠN"
                    b.btnViewTicket.setIconResource(0)
                    b.btnViewTicket.isEnabled = false
                    b.btnViewTicket.alpha = 0.6f
                    b.btnViewTicket.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                    b.btnViewTicket.setOnClickListener(null)
                    // Cập nhật status tag
                    b.tvStatus.text = "HẾT HẠN"
                    b.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                } else {
                    // Vé PENDING còn hạn: hiển thị nút thanh toán + countdown
                    b.btnViewTicket.text = "THANH TOÁN"
                    b.btnViewTicket.setIconResource(0)
                    b.btnViewTicket.isEnabled = true
                    b.btnViewTicket.alpha = 1.0f
                    b.btnViewTicket.backgroundTintList = ColorStateList.valueOf(b.root.context.getColor(R.color.error))
                    b.btnViewTicket.setOnClickListener { listener.onPayClick(bookingId) }

                    // Countdown Logic
                    if (s.expiresAt != null) {
                        try {
                            val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            val expDate = sdfIn.parse(s.expiresAt)
                            if (expDate != null) {
                                val diffInMillis = expDate.time - System.currentTimeMillis()
                                if (diffInMillis > 0) {
                                    b.tvCountdown.visibility = View.VISIBLE
                                    countDownTimer = object : CountDownTimer(diffInMillis, 1000) {
                                        override fun onTick(millisUntilFinished: Long) {
                                            val m = (millisUntilFinished / 1000) / 60
                                            val sec = (millisUntilFinished / 1000) % 60
                                            b.tvCountdown.text = String.format(Locale.getDefault(), "Hủy sau: %02d:%02d", m, sec)
                                        }

                                        override fun onFinish() {
                                            b.tvCountdown.text = "Đã quá hạn"
                                            // Khi countdown chạy xong, disable nút thanh toán luôn
                                            b.btnViewTicket.text = "ĐÃ HẾT HẠN"
                                            b.btnViewTicket.isEnabled = false
                                            b.btnViewTicket.alpha = 0.6f
                                            b.btnViewTicket.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                                            b.btnViewTicket.setOnClickListener(null)
                                            b.tvStatus.text = "HẾT HẠN"
                                            b.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                                        }
                                    }.start()
                                } else {
                                    b.tvCountdown.visibility = View.VISIBLE
                                    b.tvCountdown.text = "Đã quá hạn"
                                }
                            }
                        } catch (ignored: Exception) {
                        }
                    } else {
                        b.tvCountdown.visibility = View.GONE
                    }
                }

            } else if ("CANCELLED".equals(s.status, ignoreCase = true)) {
                // Vé đã huỷ: nút Hủy vé (btnReview) chuyển thành "ĐÃ HỦY VÉ", xám đi và vô hiệu hóa
                b.tvCountdown.visibility = View.GONE

                b.btnViewTicket.text = "VÉ ĐIỆN TỬ"
                b.btnViewTicket.setIconResource(R.drawable.ic_qr_code)
                b.btnViewTicket.isEnabled = true
                b.btnViewTicket.alpha = 1.0f
                b.btnViewTicket.backgroundTintList = ColorStateList.valueOf(b.root.context.getColor(R.color.primary))
                b.btnViewTicket.setOnClickListener { listener.onClick(bookingId) }

                b.btnReview.visibility = View.VISIBLE
                b.btnReview.text = "ĐÃ HỦY VÉ"
                b.btnReview.setIconResource(0)
                b.btnReview.isEnabled = false
                b.btnReview.alpha = 0.6f
                b.btnReview.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
                b.btnReview.setTextColor(Color.parseColor("#757575"))
                b.btnReview.strokeWidth = 0 // Bỏ border
                b.btnReview.setOnClickListener(null)
            } else {
                b.tvCountdown.visibility = View.GONE
                b.btnViewTicket.text = "VÉ ĐIỆN TỬ"
                b.btnViewTicket.setIconResource(R.drawable.ic_qr_code)
                b.btnViewTicket.isEnabled = true
                b.btnViewTicket.alpha = 1.0f
                b.btnViewTicket.backgroundTintList = ColorStateList.valueOf(b.root.context.getColor(R.color.primary))
                b.btnViewTicket.setOnClickListener { listener.onClick(bookingId) }

                // Show Review button if status is CHECKED_IN or PAID+Past
                var isPast = false
                val startTime = s.startTime
                if (startTime != null) {
                    try {
                        val sdfIn = if (startTime.contains("T")) {
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        } else {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        }
                        val startDate = sdfIn.parse(startTime)
                        if (startDate != null && startDate.time < System.currentTimeMillis()) {
                            isPast = true
                        }
                    } catch (ignored: Exception) {
                    }
                }

                if ("CHECKED_IN".equals(s.status, ignoreCase = true) ||
                    ("PAID".equals(s.status, ignoreCase = true) && isPast)
                ) {
                    b.btnReview.visibility = View.VISIBLE
                    b.btnReview.text = "ĐÁNH GIÁ"
                    b.btnReview.setIconResource(0)
                    b.btnReview.isEnabled = true
                    b.btnReview.alpha = 1.0f
                    b.btnReview.setTextColor(b.root.context.getColor(R.color.on_surface))
                    b.btnReview.backgroundTintList = ColorStateList.valueOf(b.root.context.getColor(R.color.primary))
                    b.btnReview.setOnClickListener { listener.onReviewClick(s) }
                } else if ("PAID".equals(s.status, ignoreCase = true)) {
                    // Show Cancel button for PAID upcoming tickets
                    b.btnReview.visibility = View.VISIBLE
                    b.btnReview.text = "HỦY VÉ"
                    b.btnReview.setIconResource(0)
                    b.btnReview.isEnabled = true
                    b.btnReview.alpha = 1.0f
                    b.btnReview.setTextColor(b.root.context.getColor(R.color.on_surface))
                    b.btnReview.backgroundTintList = ColorStateList.valueOf(b.root.context.getColor(R.color.error))
                    b.btnReview.setOnClickListener { listener.onCancelClick(s) }
                } else {
                    b.btnReview.visibility = View.GONE
                }
            }
            b.root.setOnClickListener { listener.onClick(bookingId) }
        }

        private fun formatScreenType(rawType: String?): String {
            if (rawType == null) return "2D"
            return when (rawType.uppercase(Locale.getDefault())) {
                "STANDARD" -> "2D"
                "THREE_D" -> "3D"
                "IMAX" -> "IMAX"
                "FOUR_DX" -> "4DX"
                else -> rawType
            }
        }

        private fun formatStatusText(status: String): String {
            return when (status) {
                "PAID" -> "ĐÃ TT"
                "PENDING" -> "CHỜ TT"
                "CHECKED_IN" -> "ĐÃ QUÉT"
                "EXPIRED" -> "HẾT HẠN"
                "CANCELLED" -> "ĐÃ HỦY"
                else -> status
            }
        }

        private fun getStatusColor(ctx: Context, status: String): Int {
            return when (status) {
                "PAID" -> Color.parseColor("#4CAF50") // Green
                "PENDING" -> Color.parseColor("#FF9800") // Orange
                "CHECKED_IN" -> Color.parseColor("#2196F3") // Blue
                "EXPIRED" -> Color.parseColor("#9E9E9E") // Grey
                "CANCELLED" -> Color.parseColor("#F44336") // Red
                else -> Color.parseColor("#9E9E9E")
            }
        }
    }
}
