package com.cinema.ticket_booking.ui.movie;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import com.cinema.ticket_booking.util.SnackbarHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.cinema.ticket_booking.data.model.request.ReviewRequest;
import com.cinema.ticket_booking.databinding.LayoutWriteReviewBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WriteReviewBottomSheet extends BottomSheetDialogFragment {

    private LayoutWriteReviewBottomSheetBinding binding;
    private MovieDetailViewModel viewModel;
    private String movieId;
    private String movieTitle;
    private String bookingId;
    private boolean isAlreadyReviewed = false;
    private String reviewId = null;
    // Cờ chặn submit nhiều lần liên tiếp
    private boolean isSubmitting = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            movieId = getArguments().getString("movieId");
            movieTitle = getArguments().getString("movieTitle");
            bookingId = getArguments().getString("bookingId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        binding = LayoutWriteReviewBottomSheetBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);
        viewModel = new ViewModelProvider(this).get(MovieDetailViewModel.class);

        binding.tvMovieName.setText(movieTitle);

        // Bấm vùng ngoài EditText → ẩn bàn phím
        binding.getRoot().setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });

        // Nút Done trên bàn phím → ẩn bàn phím
        binding.etComment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        // Pre-check trạng thái review của user với phim này
        setLoading(true);
        viewModel.checkReviewEligibility(movieId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                setLoading(false);
                var data = resource.data;
                if (data.isAlreadyReviewed() && data.getExistingReview() != null) {
                    isAlreadyReviewed = true;
                    reviewId = data.getExistingReview().getId().toString();
                    binding.ratingBar.setRating(data.getExistingReview().getRating());
                    binding.etComment.setText(data.getExistingReview().getComment());
                    binding.btnSubmit.setText("CẬP NHẬT ĐÁNH GIÁ");
                }
                // Nếu không truyền bookingId từ ngoài vào, dùng bookingId hợp lệ từ backend
                if (bookingId == null || bookingId.isEmpty()) {
                    bookingId = data.getBookingId();
                }
                binding.btnSubmit.setEnabled(true);
            } else if (resource.isError()) {
                // Vẫn cho phép thử submit, backend sẽ trả lỗi cụ thể
                setLoading(false);
                binding.btnSubmit.setEnabled(true);
            }
        });

        // Observer đặt bên ngoài onClick để không bị nhân lên mỗi lần bấm
        viewModel.getReviewResult().observe(getViewLifecycleOwner(), resReview -> {
            if (resReview == null)
                return;
            if (resReview.isSuccess()) {
                isSubmitting = false;
                Toast.makeText(requireContext(), isAlreadyReviewed ? "Cập nhật đánh giá thành công!" : "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                dismiss();
            } else if (resReview.isError()) {
                isSubmitting = false;
                setLoading(false);
                Toast.makeText(requireContext(), resReview.message, Toast.LENGTH_LONG).show();
            }
        });

        binding.btnSubmit.setOnClickListener(v -> {
            if (isSubmitting)
                return; // Chặn double-tap
            if (!isAlreadyReviewed && (bookingId == null || bookingId.isEmpty())) {
                Toast.makeText(requireContext(), "Bạn cần mua vé và xem phim này trước khi đánh giá!",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String comment = binding.etComment.getText().toString().trim();
            int rating = (int) binding.ratingBar.getRating();
            if (comment.isEmpty()) {
                binding.etComment.setError("Vui lòng nhập cảm nhận");
                return;
            }
            hideKeyboard();
            isSubmitting = true;
            setLoading(true);
            if (isAlreadyReviewed && reviewId != null) {
                viewModel.updateReview(reviewId, new ReviewRequest(movieId, null, rating, comment));
            } else {
                viewModel.submitReview(new ReviewRequest(movieId, bookingId, rating, comment));
            }
        });
    }

    private void hideKeyboard() {
        if (binding == null)
            return;
        View focused = binding.getRoot().findFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            focused.clearFocus();
        }
    }

    private void setLoading(boolean isLoading) {
        if (binding == null)
            return;
        binding.btnSubmit.setEnabled(!isLoading);
        String currentText = binding.btnSubmit.getText().toString();
        if (!isLoading && currentText.isEmpty()) {
            binding.btnSubmit.setText("GỬI ĐÁNH GIÁ");
        }
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
