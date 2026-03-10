package com.cinema.ticket_booking.ui.movie;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentMovieDetailBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MovieDetailFragment extends Fragment {

    private FragmentMovieDetailBinding binding;
    private MovieDetailViewModel viewModel;
    private String movieId;

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
        }

        setupObservers(view);
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        binding.btnBookTicket.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("movieId", movieId);
            Navigation.findNavController(view).navigate(R.id.action_movieDetail_to_selectShowtime, args);
        });
    }

    private void setupObservers(View view) {
        viewModel.getMovie().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data == null) return;
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
                        for (var g : m.getGenres()) sb.append(g.getName()).append("  ");
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
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
