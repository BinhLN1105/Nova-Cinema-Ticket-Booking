package com.cinema.ticket_booking.ui.home;

import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.data.model.response.MovieSummary;
import com.cinema.ticket_booking.data.model.response.VoucherSyncResponse;
import com.cinema.ticket_booking.data.model.response.PromotionResponse;
import com.cinema.ticket_booking.util.SnackbarHelper;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import java.util.List;

import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.cinema.ticket_booking.data.model.response.CinemaResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.cinema.ticket_booking.util.Resource;
import com.cinema.ticket_booking.data.model.response.PageResponse;
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

        binding.rvMovies.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // Mặc định khi vào app sẽ hiển thị danh sách phim Đang chiếu
        observeMovies(true);

        // Lắng nghe sự kiện chuyển Tab giữa "ĐANG CHIẾU" và "SẮP CHIẾU"
        binding.tabLayoutMovies
                .addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                        // tab.getPosition() == 0 là Tab Đang chiếu, 1 là Tab Sắp chiếu
                        observeMovies(tab.getPosition() == 0);
                    }

                    @Override
                    public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {
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

        viewModel.getActivePromotions().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null && !resource.data.isEmpty()) {
                binding.cvPromoContainer.setVisibility(View.VISIBLE);
                binding.tabLayoutPromo.setVisibility(View.VISIBLE);

                PromotionAdapter promotionAdapter = new PromotionAdapter(resource.data, promotion -> {
                    // Xử lý khi người dùng bấm vào banner khuyến mãi (nếu cần)
                });
                binding.vpPromotions.setAdapter(promotionAdapter);

                // Kết nối TabLayout với ViewPager2 để hiển thị các dấu chấm (Dot Indicator)
                new TabLayoutMediator(binding.tabLayoutPromo, binding.vpPromotions,
                        (tab, position) -> {
                        }).attach();

                // Thiết lập tự động chuyển banner sau mỗi 4 giây
                setupAutoScrollForBanners(resource.data.size());

            } else {
                binding.cvPromoContainer.setVisibility(View.GONE);
                binding.tabLayoutPromo.setVisibility(View.GONE);
                if (bannerRunnable != null) {
                    bannerHandler.removeCallbacks(bannerRunnable);
                }
            }
        });

        // Xử lý làm mới dữ liệu khi kéo từ trên xuống
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadHomeData();
            binding.swipeRefresh.setRefreshing(false);
        });

        // Điều hướng từ các nút trên Header (Thanh tiêu đề)
        binding.btnNotification
                .setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.notificationFragment));

        binding.ivUserAvatar.setOnClickListener(v -> {
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = requireActivity()
                    .findViewById(R.id.bottomNav);
            if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
                bottomNav.setSelectedItemId(R.id.profileFragment);
            } else {
                Navigation.findNavController(v).navigate(R.id.profileFragment);
            }
        });

        binding.btnLocation.setOnClickListener(
                v -> SnackbarHelper.showSuccess(binding.getRoot(), "Bộ lọc địa điểm sẽ sớm ra mắt!"));

        // Xử lý các sự kiện bấm nhanh (Quick Booking Flow)
        binding.btnQuickMovie.setOnClickListener(v -> showMovieBottomSheet());
        binding.btnQuickCinema.setOnClickListener(v -> showCinemaBottomSheet());
        binding.btnQuickDate.setOnClickListener(v -> showDateBottomSheet());

        viewModel.getQuickSelectedMovie().observe(getViewLifecycleOwner(), movie -> {
            if (movie != null)
                binding.tvQuickMovie.setText(movie.getTitle());
            else
                binding.tvQuickMovie.setText("Phim");
        });

        viewModel.getQuickSelectedCinema().observe(getViewLifecycleOwner(), cinema -> {
            if (cinema != null)
                binding.tvQuickCinema.setText(cinema.getName());
            else
                binding.tvQuickCinema.setText("Rạp");
        });

        viewModel.getQuickSelectedDate().observe(getViewLifecycleOwner(), date -> {
            if (date != null)
                binding.tvQuickDate.setText(date);
            else
                binding.tvQuickDate.setText("Ngày");
        });

        binding.btnQuickSubmit.setOnClickListener(v -> {
            MovieSummary movie = viewModel.getQuickSelectedMovie().getValue();
            CinemaResponse cinema = viewModel.getQuickSelectedCinema().getValue();
            String date = viewModel.getQuickSelectedDate().getValue();

            if (movie == null || cinema == null || date == null) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng chọn đủ Phim, Rạp và Ngày!");
                return;
            }

            Bundle args = new Bundle();
            args.putString("movieId", movie.getId());
            Navigation.findNavController(v).navigate(R.id.selectShowtimeFragment, args);
        });

        // Xử lý logic tìm kiếm (Search) với Debounce để giảm tải cho Server
        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.viewSearchDim.setVisibility(View.VISIBLE);
            } else if (binding.etSearch.getText().toString().isEmpty()) {
                binding.viewSearchDim.setVisibility(View.GONE);
                binding.rvSearchResults.setVisibility(View.GONE);
            }
        });

        // Đóng vùng tìm kiếm khi bấm vào vùng mờ (Dim Background)
        binding.viewSearchDim.setOnClickListener(v -> {
            binding.etSearch.clearFocus();
            binding.viewSearchDim.setVisibility(View.GONE);
            binding.rvSearchResults.setVisibility(View.GONE);
            // Ẩn bàn phím ảo
            View view1 = requireActivity().getCurrentFocus();
            if (view1 != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
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
        if (size <= 1)
            return;
        if (heroRunnable != null) {
            heroHandler.removeCallbacks(heroRunnable);
        }
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
        if (size <= 1)
            return; // No need to scroll if only 1 banner

        if (bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }

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

    private void observeMovies(boolean nowShowing) {
        if (nowShowing) {
            viewModel.getNowShowing().observe(getViewLifecycleOwner(), resource -> {
                if (resource.isSuccess() && resource.data != null) {
                    binding.rvMovies.setAdapter(new MovieAdapter(resource.data.getContent(),
                            movieId -> navigateToDetail(requireView(), movieId)));
                }
            });
        } else {
            viewModel.getComingSoon().observe(getViewLifecycleOwner(), resource -> {
                if (resource.isSuccess() && resource.data != null) {
                    binding.rvMovies.setAdapter(new MovieAdapter(resource.data.getContent(),
                            movieId -> navigateToDetail(requireView(), movieId)));
                }
            });
        }
    }

    private void navigateToDetail(View view, String movieId) {
        Bundle args = new Bundle();
        args.putString("movieId", movieId);
        Navigation.findNavController(view).navigate(R.id.movieDetailFragment, args);
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

    private void showMovieBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        ListView listView = new ListView(requireContext());

        Resource<PageResponse<MovieSummary>> resource = viewModel.getNowShowing().getValue();
        if (resource == null || !resource.isSuccess() || resource.data == null) {
            SnackbarHelper.showError(binding.getRoot(), "Không thể tải danh sách phim");
            return;
        }

        List<MovieSummary> movies = resource.data.getContent();
        List<String> movieTitles = new ArrayList<>();
        for (MovieSummary m : movies)
            movieTitles.add(m.getTitle());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1,
                movieTitles);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            viewModel.getQuickSelectedMovie().setValue(movies.get(position));
            // Reset cascading selections
            viewModel.getQuickSelectedCinema().setValue(null);
            viewModel.getQuickSelectedDate().setValue(null);
            dialog.dismiss();
            showCinemaBottomSheet(); // Auto cascade
        });

        dialog.setContentView(listView);
        dialog.show();
    }

    private void showCinemaBottomSheet() {
        if (viewModel.getQuickSelectedMovie().getValue() == null) {
            SnackbarHelper.showError(binding.getRoot(), "Vui lòng chọn Phim trước!");
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        ListView listView = new ListView(requireContext());

        viewModel.getCinemas("").observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                List<CinemaResponse> cinemas = resource.data;
                List<String> cinemaNames = new ArrayList<>();
                for (CinemaResponse c : cinemas)
                    cinemaNames.add(c.getName());

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1,
                        cinemaNames);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    viewModel.getQuickSelectedCinema().setValue(cinemas.get(position));
                    viewModel.getQuickSelectedDate().setValue(null);
                    dialog.dismiss();
                    showDateBottomSheet(); // Auto cascade
                });

                dialog.setContentView(listView);
                if (!dialog.isShowing())
                    dialog.show();
            }
        });
    }

    private void showDateBottomSheet() {
        if (viewModel.getQuickSelectedCinema().getValue() == null) {
            SnackbarHelper.showError(binding.getRoot(), "Vui lòng chọn Rạp trước!");
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        ListView listView = new ListView(requireContext());

        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            dates.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, dates);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            viewModel.getQuickSelectedDate().setValue(dates.get(position));
            dialog.dismiss();
        });

        dialog.setContentView(listView);
        dialog.show();
    }
}
