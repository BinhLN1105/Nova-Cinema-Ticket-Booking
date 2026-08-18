package com.cinema.ticket_booking.ui.booking

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.BookingResponse
import com.cinema.ticket_booking.data.model.response.VoucherSummary
import com.cinema.ticket_booking.databinding.FragmentConfirmBookingBinding
import com.cinema.ticket_booking.ui.MainViewModel
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ConfirmBookingFragment : Fragment() {

    private var _binding: FragmentConfirmBookingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ConfirmBookingViewModel
    private lateinit var mainViewModel: MainViewModel
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ConfirmBookingViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // Hiển thị thông tin đã chọn
        binding.tvMovieTitle.text = SelectShowtimeViewModel.pendingMovieTitle
        binding.tvCinemaName.text = SelectShowtimeViewModel.pendingCinemaName
        binding.tvShowtimeTime.text = SelectShowtimeViewModel.pendingShowtimeTime
        binding.tvSeatCount.text = "${SelectSeatViewModel.pendingSeatIds.size} ghế"

        var cbCount = 0
        for (qty in SelectComboViewModel.pendingCombos.values) {
            cbCount += qty
        }
        binding.tvComboCount.text = "$cbCount phần"

        // Format and set date
        val dateStr = SelectShowtimeViewModel.pendingShowDate
        if (dateStr != null) {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                if (date != null) {
                    binding.tvShowDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                binding.tvShowDate.text = dateStr
            }
        }

        if (SelectShowtimeViewModel.pendingMoviePoster != null) {
            Glide.with(this)
                .load(SelectShowtimeViewModel.pendingMoviePoster)
                .placeholder(R.drawable.ic_movie_placeholder)
                .into(binding.ivPoster)
        }

        binding.btnBack.setOnClickListener {
            Navigation.findNavController(view).popBackStack()
        }

        // Voucher Station
        val showVoucherSheet = View.OnClickListener {
            val cartTotal = viewModel.quoteResult.value?.data?.subtotal ?: 0.0

            val sheet = VoucherSelectionBottomSheet.newInstance(viewModel.getMyVouchers(), cartTotal)
            sheet.setListener(object : VoucherSelectionBottomSheet.OnVoucherSelectedListener {
                override fun onVoucherSelected(voucher: VoucherSummary) {
                    viewModel.applyVoucherDirectly(voucher)
                }

                override fun onManualVoucherApplied(code: String) {
                    viewModel.validateVoucher(code)
                }
            })
            sheet.show(childFragmentManager, "VoucherSheet")
        }

        binding.cvVoucherStation.setOnClickListener(showVoucherSheet)
        binding.btnSelectVoucherAction.setOnClickListener(showVoucherSheet)

        binding.btnClearVoucher.setOnClickListener {
            viewModel.clearVoucher()
            SnackbarHelper.showSuccess(binding.root, "Đã gỡ mã ưu đãi")
        }

        // Payment methods select logic
        binding.cvWallet.setOnClickListener {
            binding.rbWallet.isChecked = true
            binding.rbVnpay.isChecked = false
            binding.rbMomo.isChecked = false
        }
        binding.cvVnpay.setOnClickListener {
            binding.rbVnpay.isChecked = true
            binding.rbWallet.isChecked = false
            binding.rbMomo.isChecked = false
        }
        binding.cvMomo.setOnClickListener {
            binding.rbMomo.isChecked = true
            binding.rbWallet.isChecked = false
            binding.rbVnpay.isChecked = false
        }
        binding.rbWallet.setOnClickListener {
            binding.rbWallet.isChecked = true
            binding.rbVnpay.isChecked = false
            binding.rbMomo.isChecked = false
        }
        binding.rbVnpay.setOnClickListener {
            binding.rbVnpay.isChecked = true
            binding.rbWallet.isChecked = false
            binding.rbMomo.isChecked = false
        }
        binding.rbMomo.setOnClickListener {
            binding.rbMomo.isChecked = true
            binding.rbWallet.isChecked = false
            binding.rbVnpay.isChecked = false
        }

        // Xác nhận đặt vé
        binding.btnConfirmBooking.setOnClickListener {
            if (binding.rbWallet.isChecked) {
                viewModel.confirmBookingAndPayWithWallet()
            } else {
                viewModel.confirmBooking()
            }
        }

        // ── Seed dữ liệu ban đầu từ Parcelable ─────
        arguments?.let {
            val initialQuote: BookingResponse? = it.getParcelable("initialQuote")
            if (initialQuote != null) {
                viewModel.setInitialQuote(initialQuote)
            } else {
                viewModel.refreshQuote() // Fallback
            }
        } ?: viewModel.refreshQuote()

        // Khởi tạo đếm ngược dựa trên expireTime từ Bundle
        arguments?.let {
            val expireTime = it.getLong("expireTime", 0)
            val remaining = expireTime - System.currentTimeMillis()
            if (remaining > 0) {
                startCountdown(remaining)
            } else {
                SnackbarHelper.showError(binding.root, "Hết thời gian giữ vé!")
                Navigation.findNavController(view).popBackStack(R.id.selectSeatFragment, false)
            }
        } ?: startCountdown(5 * 60 * 1000)

        setupObservers(view)
    }

    private fun setupObservers(view: View) {
        viewModel.voucher.observe(viewLifecycleOwner) { resource ->
            if (resource == null) return@observe
            if (resource.isSuccess && resource.data != null) {
                binding.tvVoucherInfo.text = "✓ ${resource.data.description}"
                binding.tvVoucherInfo.visibility = View.VISIBLE
                binding.btnClearVoucher.visibility = View.VISIBLE
                SnackbarHelper.showSuccess(binding.root, "Áp dụng mã giảm giá thành công!")
            } else if (resource.isError) {
                SnackbarHelper.showError(binding.root, resource.message ?: "Mã giảm giá không hợp lệ")
            }
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            if (resource != null && resource.isSuccess && resource.data != null) {
                binding.tvWalletBalance.text = String.format(
                    Locale.getDefault(),
                    "Số dư hiện tại: %,d CP (≈ %,.0f ₫)",
                    resource.data.cinePoints,
                    resource.data.cinePoints * 1000.0
                )
            }
        }

        viewModel.quoteResult.observe(viewLifecycleOwner) { resource ->
            if (resource != null && resource.isSuccess && resource.data != null) {
                updatePriceDetails(resource.data)
            }
        }

        viewModel.bookingResult.observe(viewLifecycleOwner) { resource ->
            if (resource == null) return@observe
            when (resource.status) {
                Resource.Status.LOADING -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnConfirmBooking.isEnabled = false
                }
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    resource.data?.let { data ->
                        val args = Bundle().apply {
                            putString("bookingId", data.id)
                        }

                        if ("PAID" == data.status) {
                            mainViewModel.refreshUserProfile()
                            Navigation.findNavController(view)
                                .navigate(R.id.action_confirmBooking_to_bookingDetail, args)
                        } else {
                            // Default flow: go to Payment (WebView)
                            Navigation.findNavController(view)
                                .navigate(R.id.action_confirmBooking_to_payment, args)
                        }
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnConfirmBooking.setEnabled(true)
                    SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tạo giao dịch đặt vé")
                }
            }
        }

        // Observer cho kết quả thanh toán ví (Hybrid Flow)
        viewModel.walletPaymentResult.observe(viewLifecycleOwner) { resource ->
            if (resource == null) return@observe
            when (resource.status) {
                Resource.Status.LOADING -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnConfirmBooking.isEnabled = false
                }
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    resource.data?.let { data ->
                        if (data.remainingAmount != null && data.remainingAmount > 0) {
                            // Hybrid Flow: Tiếp tục thanh toán phần còn lại qua VNPay
                            val args = Bundle().apply {
                                putString("bookingId", data.bookingId)
                            }
                            Navigation.findNavController(view)
                                .navigate(R.id.action_confirmBooking_to_payment, args)
                            Toast.makeText(
                                context,
                                "Vui lòng thanh toán số tiền còn lại qua VNPay",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // Full CinePoint Payment
                            mainViewModel.refreshUserProfile()
                            val args = Bundle().apply {
                                putString("bookingId", data.bookingId)
                            }
                            Navigation.findNavController(view)
                                .navigate(R.id.action_confirmBooking_to_bookingDetail, args)
                            Toast.makeText(context, "Thanh toán bằng CinePoint thành công!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnConfirmBooking.setEnabled(true)
                    SnackbarHelper.showError(binding.root, resource.message ?: "Thanh toán ví thất bại")
                }
            }
        }
    }

    private fun updatePriceDetails(quote: BookingResponse) {
        val locale = Locale.getDefault()

        // Tiền vé + Combo (Gốc)
        binding.tvSubtotal.setText(
            String.format(
                locale, "%,.0f ₫",
                quote.totalOriginalAmount ?: 0.0
            )
        )

        // Khuyến mãi hệ thống
        val promo = quote.promotionDiscountAmount ?: 0.0
        val voucherDiscount = quote.discountAmount ?: 0.0

        // Hiển thị KM hệ thống
        if (promo > 0) {
            binding.tvPromotion.visibility = View.VISIBLE
            binding.tvPromotion.text = String.format(
                locale, "🎁 %s: -%,.0f ₫",
                quote.appliedPromotionName ?: "Khuyến mãi", promo
            )
        } else {
            binding.tvPromotion.visibility = View.GONE
        }

        // Hiển thị Voucher
        if (voucherDiscount > 0) {
            binding.tvDiscount.visibility = View.VISIBLE
            binding.tvDiscount.text = String.format(locale, "🎫 Voucher: -%,.0f ₫", voucherDiscount)
        } else {
            binding.tvDiscount.visibility = View.GONE
        }

        // Cập nhật Voucher Station UI
        val applied = viewModel.appliedVoucher
        if (applied != null) {
            binding.tvVoucherStationTitle.text = "✓ Mã Tốt Nhất: ${applied.code}"
            binding.tvVoucherStationTitle.setTextColor(0xFF4CAF50.toInt()) // Green
            binding.tvVoucherStationDesc.text = applied.description
            binding.btnClearVoucher.visibility = View.VISIBLE
        } else {
            binding.tvVoucherStationTitle.text = "Chưa chọn mã giảm giá"
            binding.tvVoucherStationTitle.setTextColor(resources.getColor(R.color.on_surface_variant, null))
            binding.tvVoucherStationDesc.text = "Nhấn để chọn hoặc nhập mã..."
            binding.btnClearVoucher.visibility = View.GONE
        }

        // Hiển thị cảnh báo voucher (nếu có)
        if (quote.warningMessage != null) {
            binding.tvVoucherInfo.text = "⚠️ ${quote.warningMessage}"
            binding.tvVoucherInfo.visibility = View.VISIBLE
        } else {
            binding.tvVoucherInfo.visibility = View.GONE
        }

        // Tổng cộng
        binding.tvTotal.setText(
            String.format(
                locale, "%,.0f ₫",
                quote.totalAmount ?: 0.0
            )
        )

        // CinePoint (Hybrid Flow Status)
        val ptDiscount = quote.pointDiscount
        if (ptDiscount != null && ptDiscount > 0) {
            binding.tvWalletBalance.text = String.format(
                locale, "Sử dụng %,d CP: -%,.0f ₫ (Còn lại: %,.0f ₫)",
                quote.pointsUsed, ptDiscount, quote.remainingAmount
            )
        }
    }

    private fun startCountdown(duration: Long) {
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                if (context != null && _binding != null) {
                    SnackbarHelper.showError(binding.root, "Hết thời gian giữ vé")
                    Navigation.findNavController(requireView()).popBackStack()
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
    }
}
