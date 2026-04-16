package com.cinema.ticket_booking.ui.movie;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import com.cinema.ticket_booking.util.SnackbarHelper;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.data.model.response.ReviewResponse;
import com.cinema.ticket_booking.databinding.FragmentMovieDetailBinding;
import com.cinema.ticket_booking.ui.booking.SelectShowtimeViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class MovieDetailFragment extends Fragment {

    private FragmentMovieDetailBinding binding;
    private MovieDetailViewModel viewModel;
    private String movieId;

    @Inject
    TokenManager tokenManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMovieDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MovieDetailViewModel.class);

        if (getArguments() != null) {
            movieId = getArguments().getString("movieId");
            viewModel.loadMovie(movieId);
            viewModel.loadReviews(movieId);
        }

        setupObservers(view);
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        binding.btnBookTicket.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("movieId", movieId);
            Navigation.findNavController(view).navigate(R.id.action_movieDetail_to_selectShowtime, args);
        });
        binding.btnWriteReview.setOnClickListener(v -> {
            if (!tokenManager.isLoggedIn()) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng đăng nhập để viết đánh giá");
                return;
            }
            viewModel.checkReviewEligibility(movieId).observe(getViewLifecycleOwner(), resource -> {
                if (resource.status == com.cinema.ticket_booking.util.Resource.Status.LOADING) return;
                binding.progressBar.setVisibility(View.GONE);
                if (resource.isSuccess() && resource.data != null) {
                    var data = resource.data;
                    Bundle args = new Bundle();
                    args.putString("movieId", movieId);
                    args.putString("movieTitle", binding.tvTitle.getText().toString());
                    args.putString("bookingId", data.getBookingId());
                    Navigation.findNavController(requireView()).navigate(R.id.action_movieDetail_to_writeReview, args);
                } else if (resource.isError()) {
                    SnackbarHelper.showError(binding.getRoot(), resource.message != null ? resource.message : "Bạn cần mua vé và xem phim trước khi đánh giá!");
                }
            });
        });

        binding.btnViewAllReviews.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("movieId", movieId);
            Navigation.findNavController(v).navigate(R.id.action_movieDetail_to_reviewList, args);
        });
    }

    private void setupObservers(View view) {
        viewModel.getMovie().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data == null)
                        return;
                    var m = resource.data;
                    binding.tvTitle.setText(m.getTitle());
                    binding.tvDescription.setText(m.getDescription());
                    binding.tvDirector.setText("Đạo diễn: " + m.getDirector());
                    binding.tvCast.setText("Diễn viên: " + m.getCast());
                    binding.tvDuration.setText(m.getDuration() + " phút");
                    binding.tvRated.setText(m.getRated());
                    binding.tvRating.setText(String.format("%.1f", m.getAvgRating()));
                    binding.tvReleaseDate.setText("Khởi chiếu: " + m.getReleaseDate());
                    if (m.getGenres() != null && !m.getGenres().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (var g : m.getGenres())
                            sb.append(g.getName()).append("  ");
                        binding.tvGenres.setText(sb.toString().trim());
                    }
                    SelectShowtimeViewModel.pendingMoviePoster = m.getPosterUrl();
                    Glide.with(this).load(m.getPosterUrl())
                            .placeholder(R.drawable.ic_movie_placeholder)
                            .into(binding.ivPoster);
                    binding.btnTrailer.setVisibility(
                            m.getTrailerUrl() != null && !m.getTrailerUrl().isEmpty() ? View.VISIBLE : View.GONE);
                    binding.btnTrailer.setOnClickListener(v -> {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(m.getTrailerUrl())));
                    });
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                }
            }
        });

        // Reviews
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        viewModel.getReviews().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                java.util.List<ReviewResponse> list = resource.data.getContent();
                if (list == null || list.isEmpty()) {
                    binding.tvNoReviews.setVisibility(View.VISIBLE);
                    binding.rvReviews.setVisibility(View.GONE);
                    binding.btnViewAllReviews.setVisibility(View.GONE);
                } else {
                    binding.tvNoReviews.setVisibility(View.GONE);
                    binding.rvReviews.setVisibility(View.VISIBLE);
                    binding.rvReviews.setAdapter(new ReviewAdapter(list));
                    binding.btnViewAllReviews.setVisibility(resource.data.getTotalElements() > 3 ? View.VISIBLE : View.GONE);
                }
            }
        });

        viewModel.getCreateReviewResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess()) {
                SnackbarHelper.showSuccess(binding.getRoot(), "Đã gửi đánh giá!");
                viewModel.loadReviews(movieId);
            } else if (resource.status == com.cinema.ticket_booking.util.Resource.Status.ERROR) {
                SnackbarHelper.showError(binding.getRoot(), resource.message);
            }
        });
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

