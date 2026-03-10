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
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BookingHistoryFragment extends Fragment {

    private FragmentBookingHistoryBinding binding;
    private BookingHistoryViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentBookingHistoryBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BookingHistoryViewModel.class);

        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));

        viewModel.getBookings().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    if (resource.data == null || resource.data.getContent() == null || resource.data.getContent().isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        binding.rvBookings.setVisibility(View.GONE);
                    } else {
                        binding.tvEmpty.setVisibility(View.GONE);
                        binding.rvBookings.setVisibility(View.VISIBLE);
                        binding.rvBookings.setAdapter(new BookingHistoryAdapter(
                                resource.data.getContent(), bookingId -> {
                            Bundle args = new Bundle();
                            args.putString("bookingId", bookingId);
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_history_to_bookingDetail, args);
                        }));
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
