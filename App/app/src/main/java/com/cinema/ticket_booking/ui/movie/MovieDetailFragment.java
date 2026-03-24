package com.cinema.ticket_booking.ui.movie;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.databinding.FragmentMovieDetailBinding;
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
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để viết đánh giá", Toast.LENGTH_SHORT).show();
                return;
            }
            binding.progressBar.setVisibility(View.VISIBLE);
            androidx.lifecycle.LiveData<com.cinema.ticket_booking.util.Resource<String>> liveData = viewModel.checkReviewEligibility(movieId);
            liveData.observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<>() {
                @Override
                public void onChanged(com.cinema.ticket_booking.util.Resource<String> resource) {
                    if (resource.status == com.cinema.ticket_booking.util.Resource.Status.LOADING) return;
                    binding.progressBar.setVisibility(View.GONE);
                    liveData.removeObserver(this);
                    if (resource.isSuccess() && resource.data != null) {
                        showWriteReviewDialog(resource.data);
                    } else {
                        Toast.makeText(requireContext(), "Bạn cần mua vé và xem phim trước khi đánh giá!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
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
                    binding.tvRating.setText(String.format("★ %.1f", m.getAvgRating()));
                    binding.tvReleaseDate.setText("Khởi chiếu: " + m.getReleaseDate());
                    if (m.getGenres() != null && !m.getGenres().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (var g : m.getGenres())
                            sb.append(g.getName()).append("  ");
                        binding.tvGenres.setText(sb.toString().trim());
                    }
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
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Reviews
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        viewModel.getReviews().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null && resource.data.getContent() != null) {
                if (resource.data.getContent().isEmpty()) {
                    binding.tvNoReviews.setVisibility(View.VISIBLE);
                    binding.rvReviews.setVisibility(View.GONE);
                } else {
                    binding.tvNoReviews.setVisibility(View.GONE);
                    binding.rvReviews.setVisibility(View.VISIBLE);
                    binding.rvReviews.setAdapter(new ReviewAdapter(resource.data.getContent()));
                }
            }
        });

        viewModel.getCreateReviewResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess()) {
                Toast.makeText(requireContext(), "Đã gửi đánh giá!", Toast.LENGTH_SHORT).show();
                viewModel.loadReviews(movieId);
            } else if (resource.status == com.cinema.ticket_booking.util.Resource.Status.ERROR) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWriteReviewDialog(String bookingId) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_write_review, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etComment = dialogView.findViewById(R.id.etComment);

        new AlertDialog.Builder(requireContext())
                .setTitle("Đánh giá phim")
                .setView(dialogView)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    int rating = (int) ratingBar.getRating();
                    String comment = etComment.getText().toString().trim();
                    if (rating == 0) {
                        Toast.makeText(requireContext(), "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.submitReview(movieId, bookingId, rating, comment);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
