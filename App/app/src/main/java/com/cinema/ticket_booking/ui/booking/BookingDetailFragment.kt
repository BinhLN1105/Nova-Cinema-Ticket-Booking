package com.cinema.ticket_booking.ui.booking

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.BookingResponse
import com.cinema.ticket_booking.databinding.FragmentBookingDetailBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import com.cinema.ticket_booking.util.TicketCacheManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BookingDetailFragment : Fragment() {

    private var _binding: FragmentBookingDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BookingDetailViewModel
    private var isCodeVisible = false
    private var rawBookingCode = ""

    @Inject
    lateinit var ticketCacheManager: TicketCacheManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[BookingDetailViewModel::class.java]

        val bookingId = arguments?.getString("bookingId")
        val paymentSuccess = arguments?.getBoolean("paymentSuccess", false) ?: false
        if (bookingId != null) {
            viewModel.loadBooking(bookingId)
        }

        // Nếu từ thanh toán thành công -> nút back chuyển sang tab Tickets
        if (paymentSuccess) {
            binding.btnBack.setOnClickListener { navigateToTicketsTab() }
            binding.btnBackTop.setOnClickListener { navigateToTicketsTab() }
        } else {
            binding.btnBack.setOnClickListener { Navigation.findNavController(view).popBackStack() }
            binding.btnBackTop.setOnClickListener { Navigation.findNavController(view).popBackStack() }
        }

        binding.ivToggleCode.setOnClickListener {
            isCodeVisible = !isCodeVisible
            updateBookingCodeDisplay()
        }

        binding.btnShare.setOnClickListener {
            if (rawBookingCode.isEmpty()) return@setOnClickListener
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Thông tin vé xem phim")
                val shareMessage = "Mình đã đặt vé xem phim *${binding.tvMovieTitle.text}*\n" +
                        "Tại: ${binding.tvCinema.text}\n" +
                        "Lúc: ${binding.tvShowtime.text}\n" +
                        "Mã đặt vé: $rawBookingCode"
                putExtra(Intent.EXTRA_TEXT, shareMessage)
            }
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ vé"))
        }

        viewModel.booking.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    resource.data?.let {
                        renderBooking(it, false)
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    if (resource.data != null) {
                        renderBooking(resource.data, true)
                        if (!resource.message.isNullOrEmpty()) {
                            SnackbarHelper.showSuccess(binding.root, resource.message)
                        }
                    } else {
                        SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải chi tiết vé")
                    }
                }
            }
        }
    }

    private fun renderBooking(b: BookingResponse, isOffline: Boolean) {
        rawBookingCode = b.bookingCode ?: ""
        updateBookingCodeDisplay()
        binding.tvMovieTitle.text = b.movieTitle
        binding.tvCinema.text = "CineNoir ${b.cinemaName ?: ""}"
        binding.tvScreen.text = b.screenName
        binding.tvFormat.text = formatScreenType(b.screenType)

        // Format showtime
        val rawTime = b.startTime
        if (rawTime != null) {
            try {
                val sdfIn = if (rawTime.contains("T")) {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                } else {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                }
                val d = sdfIn.parse(rawTime)
                if (d != null) {
                    val sdfOut = SimpleDateFormat("h:mm a - dd/MM/yyyy", Locale.getDefault())
                    binding.tvShowtime.text = sdfOut.format(d)
                }
            } catch (e: Exception) {
                binding.tvShowtime.text = rawTime
            }
        } else {
            binding.tvShowtime.text = ""
        }

        // Format status
        val statusText = when (b.status ?: "") {
            "PAID" -> "ĐÃ THANH TOÁN"
            "PENDING" -> "CHỜ THANH TOÁN"
            "CANCELLED" -> "ĐÃ HỦY"
            "EXPIRED" -> "HẾT HẠN"
            else -> b.status ?: ""
        }

        binding.tvStatus.text = statusText

        binding.tvTotal.text = String.format("Tổng thanh toán: %,.0fđ", b.totalAmount)

        // Danh sách ghế
        val seatsList = b.seats
        if (seatsList != null) {
            val seats = StringBuilder()
            for (i in seatsList.indices) {
                val s = seatsList[i]
                seats.append(s.rowLabel).append(s.colNumber)
                if (i < seatsList.size - 1) {
                    seats.append(", ")
                }
            }
            binding.tvSeats.text = seats.toString().trim()
        }

        // Danh sách bắp nước
        val combos = b.combos
        if (!combos.isNullOrEmpty()) {
            val combosStr = StringBuilder()
            for (k in combos.indices) {
                val cb = combos[k]
                combosStr.append(cb.quantity).append("x ").append(cb.comboName)
                if (k < combos.size - 1) {
                    combosStr.append("\n")
                }
            }
            binding.tvCombos.text = combosStr.toString().trim()
            binding.layoutCombos.visibility = View.VISIBLE
        } else {
            binding.layoutCombos.visibility = View.GONE
        }

        // Poster
        if (context != null) {
            Glide.with(this).load(b.moviePosterUrl)
                .placeholder(R.drawable.ic_movie_placeholder).into(binding.ivPoster)
        }

        // QR Code
        val qr = b.qrCode
        if (!qr.isNullOrEmpty()) {
            generateQrCode(qr)
        } else {
            binding.ivQrCode.visibility = View.GONE
        }

        // Trạng thái màu
        val statusColor = when (b.status ?: "") {
            "PAID" -> R.color.seat_available
            "PENDING" -> R.color.tertiary
            "CANCELLED", "EXPIRED" -> R.color.error
            else -> R.color.on_surface_variant
        }
        binding.tvStatus.setTextColor(resources.getColor(statusColor, null))

        if ("PENDING" == b.status && !isOffline) {
            binding.btnShare.visibility = View.GONE
            binding.btnPay.visibility = View.VISIBLE
            binding.btnPay.setOnClickListener {
                val args = Bundle().apply {
                    putString("bookingId", b.id)
                }
                if (view != null) {
                    Navigation.findNavController(requireView()).navigate(R.id.action_bookingDetail_to_payment, args)
                }
            }
        } else {
            binding.btnShare.visibility = View.VISIBLE
            binding.btnPay.visibility = View.GONE
        }
    }

    private fun updateBookingCodeDisplay() {
        if (_binding == null) return
        if (isCodeVisible) {
            binding.tvBookingCode.text = rawBookingCode
            binding.ivToggleCode.setImageResource(R.drawable.ic_eye)
            binding.tvBookingCode.letterSpacing = 0.3f
        } else {
            binding.tvBookingCode.text = "••••••"
            binding.ivToggleCode.setImageResource(R.drawable.ic_eye_off)
            binding.tvBookingCode.letterSpacing = 0.8f
        }
    }

    private fun generateQrCode(content: String) {
        try {
            val encoder = BarcodeEncoder()
            val bitmap = encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 400, 400)
            binding.ivQrCode.setImageBitmap(bitmap)
            binding.ivQrCode.visibility = View.VISIBLE
        } catch (e: Exception) {
            binding.ivQrCode.visibility = View.GONE
        }
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

    private fun navigateToTicketsTab() {
        try {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
            if (bottomNav != null) {
                // Pop toàn bộ back stack về root trước
                Navigation.findNavController(requireView()).popBackStack(R.id.homeFragment, false)
                // Chuyển sang tab Tickets
                bottomNav.selectedItemId = R.id.bookingHistoryFragment
            }
        } catch (e: Exception) {
            // Fallback: quay về home
            Navigation.findNavController(requireView()).popBackStack(R.id.homeFragment, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
