package com.cinema.ticket_booking.ui.promotion;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentPromotionListBinding;
import com.cinema.ticket_booking.ui.home.HomeViewModel;
import com.cinema.ticket_booking.util.SnackbarHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PromotionListFragment extends Fragment {

    private FragmentPromotionListBinding binding;
    private HomeViewModel sharedViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPromotionListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Share the HomeViewModel from the Activity to avoid redundant API calls
        sharedViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        binding.rvPromotions.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Observe the active promotions already fetched by HomeFragment
        sharedViewModel.getActivePromotions().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            
            if (resource.isSuccess() && resource.data != null) {
                if (resource.data.isEmpty()) {
                    binding.rvPromotions.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.rvPromotions.setVisibility(View.VISIBLE);
                    binding.layoutEmpty.setVisibility(View.GONE);
                    
                    PromotionListAdapter adapter = new PromotionListAdapter(resource.data, promotion -> {
                        String url = promotion.getTargetUrl();
                        if (url != null && !url.trim().isEmpty()) {
                            try {
                                if (url.startsWith("/")) {
                                    if (url.startsWith("/movie") || url.startsWith("/movies")) {
                                        Navigation.findNavController(binding.getRoot()).navigate(R.id.homeFragment);
                                    } else if (url.startsWith("/cinema")) {
                                        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNav);
                                        if (bottomNav != null) {
                                            bottomNav.setSelectedItemId(R.id.searchFragment);
                                        }
                                    } else {
                                        SnackbarHelper.showInfo(binding.getRoot(), "Khuyến mãi được tự động áp dụng khi thanh toán!");
                                    }
                                } else {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                                    startActivity(intent);
                                }
                            } catch (Exception e) {
                                SnackbarHelper.showError(binding.getRoot(), "Không thể mở liên kết!");
                            }
                        }
                    });
                    binding.rvPromotions.setAdapter(adapter);
                }
            } else if (resource.isError()) {
                SnackbarHelper.showError(binding.getRoot(), resource.message);
                binding.rvPromotions.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
