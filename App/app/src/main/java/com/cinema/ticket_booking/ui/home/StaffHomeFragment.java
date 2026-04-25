package com.cinema.ticket_booking.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentStaffHomeBinding;
import com.cinema.ticket_booking.ui.MainViewModel;
import com.cinema.ticket_booking.ui.staff.UpcomingShowtimeAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffHomeFragment extends Fragment {

    private FragmentStaffHomeBinding binding;
    private StaffHomeViewModel viewModel;
    private MainViewModel mainViewModel;
    private UpcomingShowtimeAdapter upcomingAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStaffHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StaffHomeViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupUpcomingRecyclerView();
        setupDate();
        setupNavigation();
        observeViewModel();

        binding.btnRefresh.setOnClickListener(v -> viewModel.refresh());
    }

    private void setupNavigation() {
        // ── 1. Suất chiếu hôm nay → CinemaDetailFragment ──────────────────────────
        // Dùng flag để tránh navigate nhiều lần nếu user click liên tục
        binding.cardShowtimes.setOnClickListener(v -> {
            // Disable ngay để chặn double-click
            binding.cardShowtimes.setEnabled(false);

            var cached = mainViewModel.getUserProfile().getValue();
            if (cached != null && cached.isSuccess() && cached.data != null) {
                navigateToCinemaDetail(cached.data.getCinemaId(), cached.data.getCinemaName());
                binding.cardShowtimes.setEnabled(true);
            } else {
                // Chưa có data → load rồi chờ 1 lần
                mainViewModel.loadUserProfile();
                mainViewModel.getUserProfile().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<>() {
                    @Override
                    public void onChanged(com.cinema.ticket_booking.util.Resource<com.cinema.ticket_booking.data.model.response.UserResponse> resource) {
                        if (resource == null) return;
                        if (resource.isSuccess() && resource.data != null) {
                            mainViewModel.getUserProfile().removeObserver(this);
                            navigateToCinemaDetail(resource.data.getCinemaId(), resource.data.getCinemaName());
                            if (binding != null) binding.cardShowtimes.setEnabled(true);
                        } else if (resource.isError()) {
                            mainViewModel.getUserProfile().removeObserver(this);
                            Toast.makeText(requireContext(), "Không thể tải thông tin rạp", Toast.LENGTH_SHORT).show();
                            if (binding != null) binding.cardShowtimes.setEnabled(true);
                        }
                    }
                });
            }
        });

        // ── 2. Vé hôm nay → History tab TODAY ────────────────────────────────────
        // Dùng BottomNav.setSelectedItemId để NavigationUI tự xử lý backstack,
        // tránh xung đột khi user muốn quay lại Dashboard từ menu.
        binding.cardCheckedToday.setOnClickListener(v -> {
            mainViewModel.requestHistoryTab("TODAY");
            View bnvToday = requireActivity().findViewById(R.id.bottomNav);
            if (bnvToday instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                ((com.google.android.material.bottomnavigation.BottomNavigationView) bnvToday)
                        .setSelectedItemId(R.id.checkInHistoryFragment);
            }
        });

        // ── 3. Vé tháng này → History tab THIS_MONTH ─────────────────────────────
        binding.cardCheckedMonth.setOnClickListener(v -> {
            mainViewModel.requestHistoryTab("THIS_MONTH");
            View bnvView = requireActivity().findViewById(R.id.bottomNav);
            if (bnvView instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                ((com.google.android.material.bottomnavigation.BottomNavigationView) bnvView)
                        .setSelectedItemId(R.id.checkInHistoryFragment);
            }
        });
    }

    private void navigateToCinemaDetail(String cinemaId, String cinemaName) {
        if (!isAdded() || binding == null) return;
        if (cinemaId == null || cinemaId.isEmpty()) {
            Toast.makeText(requireContext(), "Nhân viên chưa được phân công rạp!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bundle args = new Bundle();
            args.putString("cinemaId", cinemaId);
            args.putString("cinemaName", cinemaName);
            androidx.navigation.Navigation
                    .findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.action_staffHome_to_cinemaDetail, args);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Lỗi điều hướng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupUpcomingRecyclerView() {
        upcomingAdapter = new UpcomingShowtimeAdapter();
        binding.rvUpcomingShowtimes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUpcomingShowtimes.setAdapter(upcomingAdapter);
        binding.rvUpcomingShowtimes.setNestedScrollingEnabled(false);
    }

    private void setupDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        binding.tvDate.setText(sdf.format(new Date()));
    }

    private void observeViewModel() {
        viewModel.stats.observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                animateNumber(binding.tvShowtimesValue, stats.totalShowtimesToday);
                animateNumber(binding.tvCheckedTodayValue, stats.ticketsCheckedToday);
                animateNumber(binding.tvCheckedMonthValue, stats.ticketsCheckedThisMonth);
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                binding.tvShowtimesValue.setText("--");
                binding.tvCheckedTodayValue.setText("--");
                binding.tvCheckedMonthValue.setText("--");
            }
        });

        viewModel.upcomingShowtimes.observe(getViewLifecycleOwner(), list -> {
            boolean hasItems = list != null && !list.isEmpty();
            binding.tvUpcomingSubtitle.setVisibility(hasItems ? View.VISIBLE : View.GONE);
            if (!hasItems) {
                binding.tvUpcomingEmpty.setVisibility(View.VISIBLE);
                binding.rvUpcomingShowtimes.setVisibility(View.GONE);
            } else {
                binding.tvUpcomingEmpty.setVisibility(View.GONE);
                binding.rvUpcomingShowtimes.setVisibility(View.VISIBLE);
                upcomingAdapter.submitList(list);
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        mainViewModel.getUserProfile().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.isSuccess() && resource.data != null) {
                binding.tvWelcome.setText("Xin chào, " + resource.data.getFullName() + "! 👋");
                if (resource.data.getCinemaName() != null && !resource.data.getCinemaName().isEmpty()) {
                    binding.tvCinemaName.setText("Rạp: " + resource.data.getCinemaName());
                } else {
                    binding.tvCinemaName.setText("Rạp: Chưa phân công");
                }
            }
        });
    }

    private void animateNumber(android.widget.TextView tv, long newValue) {
        long currentValue;
        try {
            String current = tv.getText().toString().trim();
            currentValue = "--".equals(current) ? -1 : Long.parseLong(current);
        } catch (NumberFormatException e) {
            currentValue = -1;
        }

        if (currentValue != newValue) {
            tv.animate().alpha(0.3f).setDuration(150).withEndAction(() -> {
                tv.setText(String.valueOf(newValue));
                tv.animate().alpha(1f).setDuration(200).start();
            }).start();
        } else {
            tv.setText(String.valueOf(newValue));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
