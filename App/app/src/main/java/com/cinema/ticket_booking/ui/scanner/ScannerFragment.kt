package com.cinema.ticket_booking.ui.scanner

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.data.model.response.CheckInResponse
import com.cinema.ticket_booking.databinding.FragmentScannerBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ScannerViewModel

    @Inject
    lateinit var tokenManager: TokenManager

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            SnackbarHelper.showSuccess(binding.root, "Đã huỷ quét")
        } else {
            val qrCode = result.contents
            if (qrCode.startsWith("http") || qrCode.startsWith("novaticket://")) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrCode))
                    startActivity(intent)
                } catch (e: Exception) {
                    SnackbarHelper.showError(binding.root, "Mã QR không hợp lệ hoặc không được hỗ trợ")
                }
            } else {
                binding.progressBar.visibility = View.VISIBLE
                viewModel.checkInTicket(qrCode)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ScannerViewModel::class.java]

        val isStaff = "STAFF" == tokenManager.userRole || "ADMIN" == tokenManager.userRole
        if (!isStaff) {
            binding.tvScannerTitle.text = "Quét QR Tiện Ích"
            binding.tvScannerSubtitle.text = "Quét mã để xem thông tin phim, ưu đãi"
            binding.btnLogout.text = "Quay lại"
            binding.btnLogout.setTextColor(resources.getColor(R.color.primary, null))
            binding.btnManualInput.visibility = View.GONE
            binding.btnLogout.setOnClickListener {
                if (!Navigation.findNavController(view).popBackStack()) {
                    Navigation.findNavController(view).navigate(R.id.homeFragment)
                }
            }
        } else {
            binding.tvScannerTitle.text = "Soát Vé Phim"
            binding.tvScannerSubtitle.text = "Dành cho nhân viên kiểm soát vé tại rạp"
            binding.btnLogout.text = "Quay lại Dashboard"
            binding.btnManualInput.visibility = View.VISIBLE
            binding.btnLogout.setOnClickListener {
                if (!Navigation.findNavController(view).popBackStack()) {
                    Navigation.findNavController(view).navigate(R.id.staffHomeFragment)
                }
            }
        }

        binding.btnScan.setOnClickListener { startScan() }
        binding.btnManualInput.setOnClickListener { showManualInputDialog() }
        binding.btnCloseSuccess.setOnClickListener { resetUiToMain() }
        binding.btnCloseError.setOnClickListener { resetUiToMain() }

        viewModel.checkInResult.observe(viewLifecycleOwner) { resource ->
            if (resource.status == Resource.Status.LOADING) {
                binding.progressBar.visibility = View.VISIBLE
            } else if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                binding.progressBar.visibility = View.GONE
                showSuccessOverlay(resource.data)
            } else if (resource.status == Resource.Status.ERROR) {
                binding.progressBar.visibility = View.GONE
                showErrorOverlay(resource.message)
            }
        }
    }

    private fun showManualInputDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Nhập mã vé thủ công")
        builder.setMessage("Nhập mã đặt vé (ví dụ: BK2026...) nạp vào hệ thống để kiểm tra.")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Kiểm tra") { _, _ ->
            val code = input.text.toString().trim()
            if (code.isNotEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
                viewModel.checkInTicket(code)
            } else {
                SnackbarHelper.showError(binding.root, "Vui lòng nhập mã vé")
            }
        }
        builder.setNegativeButton("Hủy") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun startScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Hướng camera vào mã QR vé.\nBấm Tăng Âm Lượng để bật Flash.")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(false)
        options.setOrientationLocked(false)
        options.setCaptureActivity(PortraitCaptureActivity::class.java)
        barcodeLauncher.launch(options)
    }

    private fun showSuccessOverlay(data: CheckInResponse) {
        binding.layoutMain.visibility = View.GONE
        binding.overlayError.visibility = View.GONE
        binding.overlaySuccess.visibility = View.VISIBLE

        binding.tvSuccessCode.text = "Mã: " + data.bookingCode
        binding.tvSuccessMovie.text = data.movieTitle
        binding.tvSuccessCinema.text = "${data.cinemaName} - ${data.screenName}"
        binding.tvSuccessTime.text = "Thời gian: " + (data.startTime?.replace("T", " ") ?: "")

        val seats = data.seats
        if (!seats.isNullOrEmpty()) {
            val seatStr = seats.joinToString(", ") { s -> "${s.rowLabel}${s.colNumber}" }
            binding.tvSuccessSeats.text = "Ghế: $seatStr"
        } else {
            binding.tvSuccessSeats.text = "Ghế: Theo vé combo"
        }

        binding.tvSuccessCustomer.text = "Khách: " + (data.customerName ?: "Khách vãng lai")
        binding.tvSuccessEmail.text = data.customerEmail ?: ""
        val phone = data.customerPhone
        if (!phone.isNullOrEmpty()) {
            binding.tvSuccessPhone.visibility = View.VISIBLE
            binding.tvSuccessPhone.text = "SĐT: $phone"
        } else {
            binding.tvSuccessPhone.visibility = View.GONE
        }
    }

    private fun showErrorOverlay(message: String?) {
        binding.layoutMain.visibility = View.GONE
        binding.overlaySuccess.visibility = View.GONE
        binding.overlayError.visibility = View.VISIBLE

        binding.tvErrorMessage.text = message ?: "Có lỗi xảy ra"

        // Phát âm thanh báo động khẩn cấp
        try {
            var notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val r = RingtoneManager.getRingtone(requireContext(), notification)
            r?.play()

            // Xóa tiếng sau 2 giây để tránh ồn ảo
            binding.overlayError.postDelayed({
                if (r != null && r.isPlaying) r.stop()
            }, 2000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resetUiToMain() {
        binding.overlaySuccess.visibility = View.GONE
        binding.overlayError.visibility = View.GONE
        binding.layoutMain.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
