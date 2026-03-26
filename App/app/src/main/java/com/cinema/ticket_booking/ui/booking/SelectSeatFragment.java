package com.cinema.ticket_booking.ui.booking;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.databinding.FragmentSelectSeatBinding;
import java.util.*;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectSeatFragment extends Fragment {

    private FragmentSelectSeatBinding binding;
    private SelectSeatViewModel viewModel;
    private String showtimeId;
    private SeatMapResponse currentSeatMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentSelectSeatBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SelectSeatViewModel.class);

        if (getArguments() != null) {
            showtimeId = getArguments().getString("showtimeId");
            viewModel.loadSeatMap(showtimeId);
        }

        String rawTime = SelectShowtimeViewModel.pendingShowtimeTime;
        String displayTime = rawTime;
        if (rawTime != null) {
            try {
                java.text.SimpleDateFormat sdfIn;
                if (rawTime.contains("T")) {
                    sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                } else {
                    sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                }
                java.util.Date d = sdfIn.parse(rawTime);
                java.text.SimpleDateFormat sdfOut = new java.text.SimpleDateFormat("HH:mm - dd/MM/yyyy", java.util.Locale.getDefault());
                displayTime = sdfOut.format(d);
            } catch (Exception e) {
                // Ignore
            }
        }

        binding.tvShowtimeInfo.setText(
                SelectShowtimeViewModel.pendingMovieTitle + " • " + displayTime);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        binding.btnConfirm.setOnClickListener(v -> {
            if (viewModel.getSelectedSeatIds().isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn ít nhất 1 ghế", Toast.LENGTH_SHORT).show();
                return;
            }
            SelectSeatViewModel.pendingSeatIds = new ArrayList<>(viewModel.getSelectedSeatIds());
            SelectSeatViewModel.pendingTotalAmount = viewModel.calculateTotal(currentSeatMap);
            Navigation.findNavController(view).navigate(R.id.action_selectSeat_to_selectCombo);
        });

        viewModel.getSeatMap().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null) {
                        currentSeatMap = resource.data;
                        renderSeatMap(resource.data);
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void renderSeatMap(SeatMapResponse seatMap) {
        binding.seatContainer.removeAllViews();
        if (seatMap.getSeats() == null)
            return;

        // Group by row
        Map<String, List<SeatMapResponse.SeatItem>> rows = new LinkedHashMap<>();
        for (SeatMapResponse.SeatItem seat : seatMap.getSeats()) {
            rows.computeIfAbsent(seat.getRowLabel(), k -> new ArrayList<>()).add(seat);
        }

        for (Map.Entry<String, List<SeatMapResponse.SeatItem>> entry : rows.entrySet()) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER);

            // Row label
            TextView rowLabel = new TextView(requireContext());
            rowLabel.setText(entry.getKey());
            rowLabel.setTextColor(getResources().getColor(R.color.on_surface_variant, null));
            rowLabel.setMinWidth(48);
            rowLabel.setGravity(android.view.Gravity.CENTER);
            row.addView(rowLabel);

            for (SeatMapResponse.SeatItem seat : entry.getValue()) {
                TextView seatBtn = new TextView(requireContext());
                seatBtn.setText(String.valueOf(seat.getColNumber()));
                seatBtn.setGravity(android.view.Gravity.CENTER);
                seatBtn.setTextSize(10f);

                int size = (int) (getResources().getDisplayMetrics().density * 32);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.setMargins(4, 4, 4, 4);
                seatBtn.setLayoutParams(lp);

                updateSeatColor(seatBtn, seat, false);

                seatBtn.setOnClickListener(v -> {
                    boolean toggled = viewModel.toggleSeat(seat);
                    if (toggled) {
                        boolean isSelected = viewModel.getSelectedSeatIds().contains(seat.getShowtimeSeatId());
                        updateSeatColor(seatBtn, seat, isSelected);
                        updateSummary();
                    }
                });
                row.addView(seatBtn);
            }
            binding.seatContainer.addView(row);
        }
        updateSummary();
    }

    private void updateSeatColor(TextView tv, SeatMapResponse.SeatItem seat, boolean selected) {
        int color;
        if (selected)
            color = R.color.seat_selected;
        else if ("BOOKED".equals(seat.getStatus()) || "LOCKED".equals(seat.getStatus()))
            color = R.color.seat_booked;
        else if ("VIP".equals(seat.getSeatType()))
            color = R.color.seat_vip;
        else if ("COUPLE".equals(seat.getSeatType()))
            color = R.color.seat_couple;
        else
            color = R.color.seat_available;
        tv.setBackgroundColor(getResources().getColor(color, null));
        tv.setTextColor(getResources().getColor(android.R.color.white, null));
    }

    private void updateSummary() {
        int count = viewModel.getSelectedSeatIds().size();
        double total = viewModel.calculateTotal(currentSeatMap);
        binding.tvSelectedCount.setText(count + " ghế đã chọn");
        binding.tvTotalPrice.setText(String.format("%,.0fđ", total));
        binding.btnConfirm.setEnabled(count > 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
