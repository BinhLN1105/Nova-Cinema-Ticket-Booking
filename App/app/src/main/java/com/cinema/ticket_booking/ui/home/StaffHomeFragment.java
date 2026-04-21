package com.cinema.ticket_booking.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cinema.ticket_booking.databinding.FragmentStaffHomeBinding;
import com.cinema.ticket_booking.util.SnackbarHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StaffHomeFragment extends Fragment {

    private FragmentStaffHomeBinding binding;
    private StaffHomeViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStaffHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StaffHomeViewModel.class);

        // Hiển thị ngày hôm nay
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        binding.tvDate.setText(today.format(formatter));

        // Quan sát dữ liệu thống kê
        viewModel.stats.observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                binding.tvShowtimesValue.setText(String.valueOf(stats.totalShowtimesToday));
                binding.tvCheckedTodayValue.setText(String.valueOf(stats.ticketsCheckedToday));
                binding.tvCheckedMonthValue.setText(String.valueOf(stats.ticketsCheckedThisMonth));
            }
        });

        // Quan sát trạng thái loading
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && binding.pbShowtimes != null) {
                binding.pbShowtimes.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            if (isLoading != null && isLoading) {
                binding.tvShowtimesValue.setText("--");
                binding.tvCheckedTodayValue.setText("--");
                binding.tvCheckedMonthValue.setText("--");
            }
        });

        // Quan sát lỗi
        viewModel.error.observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                SnackbarHelper.showError(binding.getRoot(), errorMsg);
            }
        });

        // Nút làm mới
        binding.btnRefresh.setOnClickListener(v -> {
            viewModel.loadStats();
            SnackbarHelper.showSuccess(binding.getRoot(), "Đang cập nhật...");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
