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
import androidx.recyclerview.widget.RecyclerView;

import com.cinema.ticket_booking.databinding.FragmentCheckInHistoryBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CheckInHistoryFragment extends Fragment {

    private FragmentCheckInHistoryBinding binding;
    private CheckInHistoryViewModel viewModel;
    private CheckInHistoryAdapter adapter;

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
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new CheckInHistoryAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvHistory.setLayoutManager(layoutManager);
        binding.rvHistory.setAdapter(adapter);

        // Infinite scroll — load page tiếp theo khi gần cuối
        binding.rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.loadNextPage();
                }
            }
        });
    }

    private void observeViewModel() {
        viewModel.items.observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.rvHistory.setVisibility(View.GONE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.rvHistory.setVisibility(View.VISIBLE);
                adapter.submitList(items);
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(Boolean.TRUE.equals(isLoading)
                    && (viewModel.items.getValue() == null || viewModel.items.getValue().isEmpty())
                    ? View.VISIBLE : View.GONE);
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.tvError.setVisibility(View.VISIBLE);
                binding.tvError.setText(error);
            } else {
                binding.tvError.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
