package com.cinema.ticket_booking.ui.home;

import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.data.model.response.MovieSummary;
import com.cinema.ticket_booking.data.model.response.VoucherSyncResponse;
import com.cinema.ticket_booking.data.model.response.PromotionResponse;
import com.google.android.material.tabs.TabLayoutMediator;
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
    private final Handler heroHandler = new Handler(Looper.getMainLooper());
    private Runnable heroRunnable;

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
                    }
                }
                case ERROR -> binding.progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getFeaturedMovies().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null && !resource.data.isEmpty()) {
                List<MovieSummary> movies = resource.data;
                HeroAdapter heroAdapter = new HeroAdapter(movies);
                binding.vpHero.setAdapter(heroAdapter);

                // Initial setup for the first movie
                updateHeroOverlay(movies.get(0));

                binding.vpHero.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        updateHeroOverlay(movies.get(position));
                        // Restart auto-scroll timer
                        if (heroRunnable != null) {
                            heroHandler.removeCallbacks(heroRunnable);
                        }
                        setupAutoScrollForHero(movies.size());
                    }
                });

                setupAutoScrollForHero(movies.size());
            } else if (resource.isError()) {
                binding.tvHeroTitle.setText("Unable to load featured movies");
                binding.btnBookHero.setVisibility(View.GONE);
            }
        });

        viewModel.getPopupPromotion().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                // Show Promotion Popup if not dismissed today (Galaxy Cinema Style)
                if (PromoDialogFragment.shouldShow(requireContext())) {
                    PromoDialogFragment.newInstance(resource.data)
                        .show(getParentFragmentManager(), "PromoDialog");
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
                binding.tabLayoutPromo.setVisibility(View.VISIBLE);
                
                PromotionAdapter promotionAdapter = new PromotionAdapter(resource.data, promotion -> {
                    // Optional: Handle click
                });
                binding.vpPromotions.setAdapter(promotionAdapter);
                
                // Initialize Dot Indicators
                new TabLayoutMediator(binding.tabLayoutPromo, binding.vpPromotions, 
                    (tab, position) -> {}).attach();
                
                // Auto-scroll logic
                setupAutoScrollForBanners(resource.data.size());
                
            } else {
                binding.cvPromoContainer.setVisibility(View.GONE);
                binding.tabLayoutPromo.setVisibility(View.GONE);
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
        // Search Overlay Logic
        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.viewSearchDim.setVisibility(View.VISIBLE);
            } else if (binding.etSearch.getText().toString().isEmpty()) {
                binding.viewSearchDim.setVisibility(View.GONE);
                binding.rvSearchResults.setVisibility(View.GONE);
            }
        });

        binding.viewSearchDim.setOnClickListener(v -> {
            binding.etSearch.clearFocus();
            binding.viewSearchDim.setVisibility(View.GONE);
            binding.rvSearchResults.setVisibility(View.GONE);
            // Hide keyboard
            View view1 = requireActivity().getCurrentFocus();
            if (view1 != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view1.getWindowToken(), 0);
            }
        });

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
                    if (!binding.etSearch.hasFocus()) {
                        binding.viewSearchDim.setVisibility(View.GONE);
                    }
                    viewModel.clearSearch();
                } else if (query.length() >= 2) {
                    binding.viewSearchDim.setVisibility(View.VISIBLE);
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

        // Popup logic moved to viewModel.getPopupPromotion() observer
    }

    private void setupAutoScrollForHero(int size) {
        if (size <= 1) return;
        heroRunnable = () -> {
            int current = binding.vpHero.getCurrentItem();
            int next = (current + 1) % size;
            binding.vpHero.setCurrentItem(next, true);
        };
        heroHandler.postDelayed(heroRunnable, 5000); // 5s interval for Hero
    }

    private void updateHeroOverlay(MovieSummary movie) {
        binding.tvHeroTitle.setText(movie.getTitle().toUpperCase());
        binding.btnBookHero.setVisibility(View.VISIBLE);
        binding.btnBookHero.setOnClickListener(v -> navigateToDetail(requireView(), movie.getId()));
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
        if (heroRunnable != null) {
            heroHandler.removeCallbacks(heroRunnable);
        }
        binding = null;
    }
}
