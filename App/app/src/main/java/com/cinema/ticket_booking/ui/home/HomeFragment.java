package com.cinema.ticket_booking.ui.home;

import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.data.model.response.MovieSummary;
import com.cinema.ticket_booking.data.model.response.VoucherSyncResponse;
import com.cinema.ticket_booking.data.model.response.PromotionResponse;
import androidx.viewpager2.widget.ViewPager2;
import java.util.List;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentHomeBinding;
import dagger.hilt.android.AndroidEntryPoint;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private Runnable bannerRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding.rvNowShowing.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvComingSoon.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        viewModel.getNowShowing().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null && resource.data.getContent() != null
                            && !resource.data.getContent().isEmpty()) {
                        List<MovieSummary> movies = resource.data.getContent();
                        MovieAdapter adapter = new MovieAdapter(movies,
                                movieId -> navigateToDetail(view, movieId));
                        binding.rvNowShowing.setAdapter(adapter);

                        // Setup Hero Section with first movie from backend
                        MovieSummary firstMovie = movies.get(0);
                        binding.tvHeroTitle.setText(firstMovie.getTitle().toUpperCase());
                        Glide.with(requireContext())
                                .load(firstMovie.getPosterUrl())
                                .placeholder(R.drawable.placeholder_hero)
                                .error(R.drawable.placeholder_hero)
                                .centerCrop()
                                .into(binding.ivHero);
                        binding.btnBookHero.setOnClickListener(v -> navigateToDetail(view, firstMovie.getId()));
                    } else {
                        // No movies from backend — show empty state
                        binding.tvHeroTitle.setText("No Movies Available");
                        binding.btnBookHero.setVisibility(View.GONE);
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvHeroTitle.setText("Unable to load movies");
                    binding.btnBookHero.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getComingSoon().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                MovieAdapter adapter = new MovieAdapter(resource.data.getContent(),
                        movieId -> navigateToDetail(view, movieId));
                binding.rvComingSoon.setAdapter(adapter);
            }
        });

        viewModel.getActivePromotions().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null && !resource.data.isEmpty()) {
                binding.cvPromoContainer.setVisibility(View.VISIBLE);
                
                PromotionAdapter promotionAdapter = new PromotionAdapter(resource.data, promotion -> {
                    // Optional: Handle click, e.g., open a dialog or a link
                });
                binding.vpPromotions.setAdapter(promotionAdapter);
                
                // Auto-scroll logic
                setupAutoScrollForBanners(resource.data.size());
                
            } else {
                binding.cvPromoContainer.setVisibility(View.GONE);
                if (bannerRunnable != null) {
                    bannerHandler.removeCallbacks(bannerRunnable);
                }
            }
        });

        // SwipeRefresh
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadHomeData();
            binding.swipeRefresh.setRefreshing(false);
        });

        // Chatbot FAB
        // Search Debounce Logic
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString().trim();
                if (query.isEmpty()) {
                    binding.rvSearchResults.setVisibility(View.GONE);
                    viewModel.clearSearch();
                } else if (query.length() >= 2) {
                    searchRunnable = () -> viewModel.searchMovies(query);
                    searchHandler.postDelayed(searchRunnable, 500); // 500ms Debounce
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || resource.status == com.cinema.ticket_booking.util.Resource.Status.LOADING) {
                return;
            }
            if (resource.isSuccess() && resource.data != null && !resource.data.getContent().isEmpty()) {
                binding.rvSearchResults.setVisibility(View.VISIBLE);
                binding.rvSearchResults.setAdapter(new MovieAdapter(resource.data.getContent(),
                        movieId -> navigateToDetail(view, movieId)));
            } else {
                binding.rvSearchResults.setVisibility(View.GONE);
            }
        });
    }

    private void setupAutoScrollForBanners(int size) {
        if (size <= 1) return; // No need to scroll if only 1 banner

        bannerRunnable = () -> {
            int currentItem = binding.vpPromotions.getCurrentItem();
            int nextItem = (currentItem + 1) % size;
            binding.vpPromotions.setCurrentItem(nextItem, true);
        };

        bannerHandler.postDelayed(bannerRunnable, 4000);

        binding.vpPromotions.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bannerHandler.removeCallbacks(bannerRunnable);
                bannerHandler.postDelayed(bannerRunnable, 4000);
            }
        });
    }

    private void navigateToDetail(View view, String movieId) {
        Bundle args = new Bundle();
        args.putString("movieId", movieId);
        Navigation.findNavController(view).navigate(R.id.action_home_to_movieDetail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
        binding = null;
    }
}
