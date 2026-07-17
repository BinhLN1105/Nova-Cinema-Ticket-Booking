package com.cinema.ticket_booking.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.databinding.FragmentCancelConfirmBinding
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CancelConfirmFragment : Fragment() {

    private var _binding: FragmentCancelConfirmBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BookingDetailViewModel
    private var token: String? = null
    private var bookingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            token = it.getString("token")
            bookingId = it.getString("bookingId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCancelConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[BookingDetailViewModel::class.java]

        if (token == null || bookingId == null) {
            showError("Thông tin xác nhận không hợp lệ")
            return
        }

        confirmCancellation()

        binding.btnBackToDetail.setOnClickListener { v ->
            val args = Bundle().apply {
                putString("bookingId", bookingId)
            }
            Navigation.findNavController(v).navigate(R.id.bookingDetailFragment, args)
        }
    }

    private fun confirmCancellation() {
        val t = token ?: return
        val bId = bookingId ?: return
        viewModel.cancelConfirm(t, bId).observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    showSuccess()
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    showError(resource.message ?: "Lỗi xác nhận hủy vé")
                }
            }
        }
    }

    private fun showSuccess() {
        binding.ivStatus.setImageResource(R.drawable.ic_check)
        binding.tvTitle.text = "Hủy vé thành công!"
        binding.tvMessage.text = "Yêu cầu hủy vé của bạn đã được thực hiện. Điểm CinePoint sẽ được hoàn trả vào ví của bạn."
        binding.btnBackToDetail.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.ivStatus.setImageResource(R.drawable.ic_close)
        binding.tvTitle.text = "Hủy vé thất bại"
        binding.tvMessage.text = message
        binding.btnBackToDetail.visibility = View.VISIBLE
        binding.btnBackToDetail.text = "Quay lại"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
