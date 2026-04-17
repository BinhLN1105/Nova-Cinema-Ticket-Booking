package com.cinema.ticket_booking.ui.movie;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import com.cinema.ticket_booking.util.SnackbarHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.request.ReviewRequest;
import com.cinema.ticket_booking.databinding.LayoutWriteReviewBottomSheetBinding;
import com.cinema.ticket_booking.ui.booking.BookingHistoryViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WriteReviewBottomSheet extends BottomSheetDialogFragment {

    private LayoutWriteReviewBottomSheetBinding binding;
    private MovieDetailViewModel viewModel;
    private String movieId;
    private String movieTitle;
    private String bookingId;

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

        // Pre-check review status (Nova Strategy: One Review Per Movie)
        setLoading(true);
        viewModel.checkReviewEligibility(movieId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                setLoading(false);
                var data = resource.data;
                if (data.isAlreadyReviewed() && data.getExistingReview() != null) {
                    binding.ratingBar.setRating(data.getExistingReview().getRating());
                    binding.etComment.setText(data.getExistingReview().getComment());
                    binding.btnSubmit.setText("CẬP NHẬT ĐÁNH GIÁ");
                }
                
                // If bookingId was not passed (from Profile/Notification), use the eligible one from backend
                if (bookingId == null || bookingId.isEmpty()) {
                    bookingId = data.getBookingId();
                }
                
                if (bookingId == null && !data.isAlreadyReviewed()) {
                    setLoading(false);
                    SnackbarHelper.showError(binding.getRoot(), "Không tìm thấy vé hợp lệ để đánh giá!");
                    binding.btnSubmit.setEnabled(false);
                }
            } else if (resource.isError()) {
                setLoading(false);
                SnackbarHelper.showError(binding.getRoot(), resource.message);
                binding.btnSubmit.setEnabled(false);
            }
        });

        binding.btnSubmit.setOnClickListener(v -> {
            String comment = binding.etComment.getText().toString().trim();
            int rating = (int) binding.ratingBar.getRating();

            if (comment.isEmpty()) {
                binding.etComment.setError("Vui lòng nhập cảm nhận");
                return;
            }

            setLoading(true);
            ReviewRequest request = new ReviewRequest(movieId, bookingId, rating, comment);

            viewModel.createReview(request).observe(getViewLifecycleOwner(), resReview -> {
                if (resReview.isSuccess()) {
                    SnackbarHelper.showInfo(binding.getRoot(), "Đánh giá thành công!");
                    try {
                        var historyVm = new ViewModelProvider(requireActivity()).get(com.cinema.ticket_booking.ui.booking.BookingHistoryViewModel.class);
                        historyVm.refresh();
                    } catch (Exception ignored) {}
                    dismiss();
                } else if (resReview.isError()) {
                    setLoading(false);
                    SnackbarHelper.showError(binding.getRoot(), resReview.message);
                }
            });
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnSubmit.setEnabled(!isLoading);
        binding.btnSubmit.setText(isLoading ? "" : "GỬI ĐÁNH GIÁ");
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

