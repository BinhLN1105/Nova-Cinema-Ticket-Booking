package com.cinema.ticket_booking.ui.movie

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.cinema.ticket_booking.data.model.request.ReviewRequest
import com.cinema.ticket_booking.databinding.LayoutWriteReviewBottomSheetBinding
import com.cinema.ticket_booking.util.Resource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.ViewModelProvider

@AndroidEntryPoint
class WriteReviewBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutWriteReviewBottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MovieDetailViewModel
    private var movieId: String? = null
    private var movieTitle: String? = null
    private var bookingId: String? = null
    private var isAlreadyReviewed = false
    private var reviewId: String? = null
    // Cờ chặn submit nhiều lần liên tiếp
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            movieId = it.getString("movieId")
            movieTitle = it.getString("movieTitle")
            bookingId = it.getString("bookingId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutWriteReviewBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[MovieDetailViewModel::class.java]

        binding.tvMovieName.text = movieTitle

        // Bấm vùng ngoài EditText → ẩn bàn phím
        binding.getRoot().setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }

        // Nút Done trên bàn phím → ẩn bàn phím
        binding.etComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Pre-check trạng thái review của user với phim này
        setLoading(true)
        movieId?.let { id ->
            viewModel.checkReviewEligibility(id).observe(viewLifecycleOwner) { resource ->
                if (resource.isSuccess && resource.data != null) {
                    setLoading(false)
                    val data = resource.data
                    if (data.alreadyReviewed && data.existingReview != null) {
                        isAlreadyReviewed = true
                        reviewId = data.existingReview.id.toString()
                        binding.ratingBar.rating = data.existingReview.rating.toFloat()
                        binding.etComment.setText(data.existingReview.comment)
                        binding.btnSubmit.text = "CẬP NHẬT ĐÁNH GIÁ"
                    }
                    // Nếu không truyền bookingId từ ngoài vào, dùng bookingId hợp lệ từ backend
                    if (bookingId.isNullOrEmpty()) {
                        bookingId = data.bookingId
                    }
                    binding.btnSubmit.isEnabled = true
                } else if (resource.isError) {
                    // Vẫn cho phép thử submit, backend sẽ trả lỗi cụ thể
                    setLoading(false)
                    binding.btnSubmit.isEnabled = true
                }
            }
        }

        // Observer đặt bên ngoài onClick để không bị nhân lên mỗi lần bấm
        viewModel.getReviewResult().observe(viewLifecycleOwner) { resReview ->
            if (resReview == null) return@observe
            if (resReview.isSuccess) {
                isSubmitting = false
                Toast.makeText(
                    requireContext(),
                    if (isAlreadyReviewed) "Cập nhật đánh giá thành công!" else "Đánh giá thành công!",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            } else if (resReview.isError) {
                isSubmitting = false
                setLoading(false)
                Toast.makeText(requireContext(), resReview.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnSubmit.setOnClickListener {
            if (isSubmitting) return@setOnClickListener // Chặn double-tap
            if (!isAlreadyReviewed && bookingId.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Bạn cần mua vé và xem phim này trước khi đánh giá!",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val comment = binding.etComment.text.toString().trim()
            val rating = binding.ratingBar.rating.toInt()
            if (comment.isEmpty()) {
                binding.etComment.error = "Vui lòng nhập cảm nhận"
                return@setOnClickListener
            }
            hideKeyboard()
            isSubmitting = true
            setLoading(true)
            if (isAlreadyReviewed && reviewId != null) {
                viewModel.updateReview(reviewId!!, ReviewRequest(movieId, null, rating, comment))
            } else {
                viewModel.submitReview(ReviewRequest(movieId, bookingId, rating, comment))
            }
        }
    }

    private fun hideKeyboard() {
        if (_binding == null) return
        val focused = binding.getRoot().findFocus()
        if (focused != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(focused.windowToken, 0)
            focused.clearFocus()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.btnSubmit.isEnabled = !isLoading
        val currentText = binding.btnSubmit.text.toString()
        if (!isLoading && currentText.isEmpty()) {
            binding.btnSubmit.text = "GỬI ĐÁNH GIÁ"
        }
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
