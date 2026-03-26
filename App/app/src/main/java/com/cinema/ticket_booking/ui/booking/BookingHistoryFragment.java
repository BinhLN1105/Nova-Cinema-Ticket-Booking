package com.cinema.ticket_booking.ui.booking;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentBookingHistoryBinding;
import com.cinema.ticket_booking.data.local.TokenManager;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BookingHistoryFragment extends Fragment {

    private FragmentBookingHistoryBinding binding;
    private BookingHistoryViewModel viewModel;
    private java.util.List<com.cinema.ticket_booking.data.model.response.BookingSummary> allBookings = new java.util.ArrayList<>();
    private boolean isUpcomingTab = true;

    @Inject
    TokenManager tokenManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentBookingHistoryBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // If not logged in, show login prompt BEFORE creating ViewModel
        if (!tokenManager.isLoggedIn()) {
            binding.rvBookings.setVisibility(View.GONE);
            binding.tvEmpty.setVisibility(View.GONE);
            binding.layoutLoginPrompt.setVisibility(View.VISIBLE);
            binding.swipeRefresh.setEnabled(false);
            binding.btnLoginPrompt.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.loginFragment));
            return;
        }

        // Only create ViewModel (which auto-loads data) AFTER login check
        viewModel = new ViewModelProvider(this).get(BookingHistoryViewModel.class);

        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.tabUpcoming.setOnClickListener(v -> {
            if (isUpcomingTab) return;
            isUpcomingTab = true;
            updateTabStyles();
            updateList();
        });
        
        binding.tabHistory.setOnClickListener(v -> {
            if (!isUpcomingTab) return;
            isUpcomingTab = false;
            updateTabStyles();
            updateList();
        });

        viewModel.getBookings().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    allBookings.clear();
                    if (resource.data != null && resource.data.getContent() != null) {
                        allBookings.addAll(resource.data.getContent());
                    }
                    updateList();
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.btnBrowseMovies.setOnClickListener(v -> {
            // Navigate back to Home fragment which is the start destination
            Navigation.findNavController(view).popBackStack(R.id.homeFragment, false);
        });

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void updateTabStyles() {
        int colorPrimary = getResources().getColor(R.color.primary, null);
        int colorOnSurfaceVariant = getResources().getColor(R.color.on_surface_variant, null);
        android.content.res.ColorStateList bgActive = android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.surface_container_highest, null));
        android.content.res.ColorStateList bgInactive = android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.transparent, null));

        binding.tabUpcoming.setTextColor(isUpcomingTab ? colorPrimary : colorOnSurfaceVariant);
        binding.tabUpcoming.setBackgroundTintList(isUpcomingTab ? bgActive : bgInactive);
        
        binding.tabHistory.setTextColor(!isUpcomingTab ? colorPrimary : colorOnSurfaceVariant);
        binding.tabHistory.setBackgroundTintList(!isUpcomingTab ? bgActive : bgInactive);
    }

    private void updateList() {
        java.util.List<com.cinema.ticket_booking.data.model.response.BookingSummary> filtered = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
        java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        
        for (com.cinema.ticket_booking.data.model.response.BookingSummary b : allBookings) {
            boolean isFuture = true;
            try {
                if (b.getStartTime() != null) {
                    java.util.Date d;
                    try {
                        d = sdf.parse(b.getStartTime());
                    } catch (Exception e) {
                        d = sdf2.parse(b.getStartTime());
                    }
                    if (d != null) isFuture = d.getTime() > now;
                }
            } catch (Exception e) {}

            boolean isUpcoming = ("PAID".equals(b.getStatus()) || "PENDING".equals(b.getStatus())) && isFuture;
            
            if (isUpcomingTab == isUpcoming) {
                filtered.add(b);
            }
        }

        if (filtered.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvBookings.setVisibility(View.GONE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvBookings.setVisibility(View.VISIBLE);
            binding.rvBookings.setAdapter(new BookingHistoryAdapter(
                    filtered, new BookingHistoryAdapter.Listener() {
                        @Override
                        public void onClick(String bookingId) {
                            Bundle args = new Bundle();
                            args.putString("bookingId", bookingId);
                            if (getView() != null) {
                                Navigation.findNavController(getView())
                                        .navigate(R.id.action_history_to_bookingDetail, args);
                            }
                        }

                        @Override
                        public void onPayClick(String bookingId) {
                            Bundle args = new Bundle();
                            args.putString("bookingId", bookingId);
                            if (getView() != null) {
                                Navigation.findNavController(getView())
                                        .navigate(R.id.action_history_to_payment, args);
                            }
                        }
                    }));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
