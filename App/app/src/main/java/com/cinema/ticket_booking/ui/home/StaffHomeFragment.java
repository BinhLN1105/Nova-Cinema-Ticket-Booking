package com.cinema.ticket_booking.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cinema.ticket_booking.databinding.FragmentStaffHomeBinding;
import com.cinema.ticket_booking.ui.staff.UpcomingShowtimeAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffHomeFragment extends Fragment {

    private FragmentStaffHomeBinding binding;
    private StaffHomeViewModel viewModel;
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

        setupUpcomingRecyclerView();
        setupDate();
        observeViewModel();

        binding.btnRefresh.setOnClickListener(v -> viewModel.refresh());
    }

    private void setupUpcomingRecyclerView() {
        upcomingAdapter = new UpcomingShowtimeAdapter();
        binding.rvUpcomingShowtimes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUpcomingShowtimes.setAdapter(upcomingAdapter);
        binding.rvUpcomingShowtimes.setNestedScrollingEnabled(false);
    }

    private void setupDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        String today = sdf.format(new Date());
        binding.tvDate.setText(today);
    }

    private void observeViewModel() {
        // Stats — badge tự nhảy khi polling cập nhật
        viewModel.stats.observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                animateNumber(binding.tvShowtimesValue, stats.totalShowtimesToday);
                animateNumber(binding.tvCheckedTodayValue, stats.ticketsCheckedToday);
                animateNumber(binding.tvCheckedMonthValue, stats.ticketsCheckedThisMonth);
            }
        });

        // Loading indicator
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                binding.tvShowtimesValue.setText("--");
                binding.tvCheckedTodayValue.setText("--");
                binding.tvCheckedMonthValue.setText("--");
            }
        });

        // Upcoming showtimes
        viewModel.upcomingShowtimes.observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) {
                binding.tvUpcomingEmpty.setVisibility(View.VISIBLE);
                binding.rvUpcomingShowtimes.setVisibility(View.GONE);
            } else {
                binding.tvUpcomingEmpty.setVisibility(View.GONE);
                binding.rvUpcomingShowtimes.setVisibility(View.VISIBLE);
                upcomingAdapter.submitList(list);
            }
        });

        // Error state — hiện toast khi API lỗi
        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Hiệu ứng nhảy số đơn giản — đặt text trực tiếp nhưng với highlight ngắn
     * Để làm animation đếm số thực sự cần CountUp custom animator.
     */
    private void animateNumber(android.widget.TextView tv, long newValue) {
        long currentValue;
        try {
            String current = tv.getText().toString().trim();
            currentValue = "--".equals(current) ? -1 : Long.parseLong(current);
        } catch (NumberFormatException e) {
            currentValue = -1;
        }

        if (currentValue != newValue) {
            // Flash animation nhỏ khi số thay đổi
            tv.animate()
                    .alpha(0.3f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        tv.setText(String.valueOf(newValue));
                        tv.animate().alpha(1f).setDuration(200).start();
                    })
                    .start();
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
