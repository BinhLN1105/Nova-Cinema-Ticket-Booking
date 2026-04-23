package com.cinema.ticket_booking.ui.staff;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentCheckInHistoryBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CheckInHistoryFragment extends Fragment {

    private FragmentCheckInHistoryBinding binding;
    private CheckInHistoryViewModel viewModel;
    private CheckInHistoryAdapter adapter;

    private boolean isShowingToday = true; // Mặc định tab "Hôm nay"

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCheckInHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CheckInHistoryViewModel.class);

        setupRecyclerView();
        setupTabs();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new CheckInHistoryAdapter();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(adapter);
    }

    private void setupTabs() {
        // Mặc định: tab Hôm nay active
        setTabSelected(true);

        binding.tabToday.setOnClickListener(v -> {
            if (!isShowingToday) {
                isShowingToday = true;
                setTabSelected(true);
                showCurrentTab();
            }
        });

        binding.tabThisMonth.setOnClickListener(v -> {
            if (isShowingToday) {
                isShowingToday = false;
                setTabSelected(false);
                showCurrentTab();
            }
        });
    }

    private void setTabSelected(boolean todaySelected) {
        if (todaySelected) {
            binding.tabToday.setTextColor(0xFFF5C518);
            binding.tabToday.setBackgroundResource(R.drawable.bg_tab_selected);
            binding.tabThisMonth.setTextColor(0xFF6B7E94);
            binding.tabThisMonth.setBackgroundResource(R.drawable.bg_tab_unselected);
        } else {
            binding.tabThisMonth.setTextColor(0xFFF5C518);
            binding.tabThisMonth.setBackgroundResource(R.drawable.bg_tab_selected);
            binding.tabToday.setTextColor(0xFF6B7E94);
            binding.tabToday.setBackgroundResource(R.drawable.bg_tab_unselected);
        }
    }

    private void observeViewModel() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.tvError.setVisibility(View.VISIBLE);
                binding.tvError.setText(error);
                binding.tvError.postDelayed(() -> {
                    if (binding != null) binding.tvError.setVisibility(View.GONE);
                }, 3000);
            }
        });

        // Observe cả 2 tab, khi data thay đổi thì refresh list nếu tab đó đang active
        viewModel.todayItems.observe(getViewLifecycleOwner(), items -> {
            if (isShowingToday) showList(items);
        });

        viewModel.monthItems.observe(getViewLifecycleOwner(), items -> {
            if (!isShowingToday) showList(items);
        });
    }

    private void showCurrentTab() {
        if (isShowingToday) {
            showList(viewModel.todayItems.getValue());
        } else {
            showList(viewModel.monthItems.getValue());
        }
    }

    private void showList(java.util.List<com.cinema.ticket_booking.data.model.response.CheckInHistoryItemResponse> items) {
        if (items == null || items.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvHistory.setVisibility(View.GONE);
            binding.tvEmpty.setText(isShowingToday
                    ? "Chưa có vé nào được soát hôm nay"
                    : "Chưa có vé nào được soát trong tháng này");
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvHistory.setVisibility(View.VISIBLE);
            adapter.submitList(items);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
