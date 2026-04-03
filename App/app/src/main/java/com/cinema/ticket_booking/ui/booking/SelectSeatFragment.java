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
import com.cinema.ticket_booking.util.SnackbarHelper;
import com.cinema.ticket_booking.databinding.FragmentSelectSeatBinding;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectSeatFragment extends Fragment {

    private FragmentSelectSeatBinding binding;
    private SelectSeatViewModel viewModel;
    private String showtimeId;
    private SeatMapResponse currentSeatMap;
    private long expireTime = 0;

    // Polling for real-time seat status updates
    private final android.os.Handler pollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pollRunnable;
    private static final long POLL_INTERVAL_MS = 12_000; // 12 seconds

    // Map of showtimeSeatId -> seat button view (for efficient targeted updates)
    private final Map<String, TextView> seatButtonMap = new HashMap<>();

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
                SimpleDateFormat sdfIn;
                if (rawTime.contains("T")) {
                    sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                } else {
                    sdfIn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                }
                Date d = sdfIn.parse(rawTime);
                SimpleDateFormat sdfOut = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
                displayTime = sdfOut.format(d);
            } catch (Exception e) {
                // Ignore
            }
        }

        binding.tvShowtimeInfo.setText(
                SelectShowtimeViewModel.pendingMovieTitle + " • " + displayTime);

        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(view).popBackStack();
        });

        binding.btnConfirm.setOnClickListener(v -> {
            if (viewModel.getSelectedSeatIds().isEmpty()) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng chọn ít nhất 1 ghế");
                return;
            }
            SelectSeatViewModel.pendingSeatIds = new ArrayList<>(viewModel.getSelectedSeatIds());
            SelectSeatViewModel.pendingTotalAmount = viewModel.calculateTotal(currentSeatMap);

            Bundle bundle = new Bundle();
            bundle.putLong("expireTime", expireTime);
            Navigation.findNavController(view).navigate(R.id.action_selectSeat_to_selectCombo, bundle);
        });

        viewModel.getSeatMap().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null) {
                        currentSeatMap = resource.data;
                        expireTime = System.currentTimeMillis() + (long) resource.data.getSeatHoldMins() * 60 * 1000;
                        seatButtonMap.clear();
                        renderSeatMap(resource.data);
                        startPolling();
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                }
            }
        });

        // Silent refresh observer — only updates changed seat colors
        viewModel.getSeatRefresh().observe(getViewLifecycleOwner(), freshMap -> {
            if (freshMap == null || freshMap.getSeats() == null) return;
            currentSeatMap = freshMap;
            boolean anyDeselected = false;
            for (SeatMapResponse.SeatItem seat : freshMap.getSeats()) {
                TextView btn = seatButtonMap.get(seat.getShowtimeSeatId());
                if (btn == null) continue;
                boolean isSelectedByMe = viewModel.getSelectedSeatIds().contains(seat.getShowtimeSeatId());
                // If seat became taken but user had selected it, deselect
                if (isSelectedByMe && ("BOOKED".equals(seat.getStatus()) || "LOCKED".equals(seat.getStatus()))) {
                    viewModel.getSelectedSeatIds().remove(seat.getShowtimeSeatId());
                    isSelectedByMe = false;
                    anyDeselected = true;
                }
                updateSeatButtonStyle(btn, seat, isSelectedByMe);
            }
            if (anyDeselected) {
                SnackbarHelper.showError(binding.getRoot(), "Một số ghế bạn chọn vừa bị người khác đặt mất!");
                updateSummary();
            }
        });
    }

    private void startPolling() {
        if (pollRunnable != null) pollHandler.removeCallbacks(pollRunnable);
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding != null) {
                    viewModel.refreshSeatStatuses();
                    pollHandler.postDelayed(this, POLL_INTERVAL_MS);
                }
            }
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void renderSeatMap(SeatMapResponse seatMap) {
        binding.seatContainer.removeAllViews();
        if (seatMap.getSeats() == null)
            return;

        int totalRows = seatMap.getMaxGridRow() + 1;
        int totalCols = seatMap.getMaxGridCol() + 1;

        // Build lookup map: "gridRow_gridCol" -> SeatItem
        Map<String, SeatMapResponse.SeatItem> seatGrid = new HashMap<>();
        for (SeatMapResponse.SeatItem seat : seatMap.getSeats()) {
            seatGrid.put(seat.getGridRow() + "_" + seat.getGridCol(), seat);
        }

        int seatSize = (int) (getResources().getDisplayMetrics().density * 34);
        int margin = (int) (getResources().getDisplayMetrics().density * 3);

        for (int r = 0; r < totalRows; r++) {
            // Determine row label: scan seats in this row to find the label char
            String label = String.valueOf((char) ('A' + r));
            for (int c = 0; c < totalCols; c++) {
                SeatMapResponse.SeatItem s = seatGrid.get(r + "_" + c);
                if (s != null && s.getSeatLabel() != null && !s.getSeatLabel().isEmpty()) {
                    label = String.valueOf(s.getSeatLabel().charAt(0));
                    break;
                }
            }
            // Always render the row (including empty rows) so the seat map is consistent

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER);
            row.setPadding(0, margin, 0, margin);

            // Left label
            TextView rowLabelLeft = new TextView(requireContext());
            rowLabelLeft.setText(label);
            rowLabelLeft.setTextColor(getResources().getColor(R.color.on_surface_variant, null));
            rowLabelLeft.setTypeface(null, android.graphics.Typeface.BOLD);
            rowLabelLeft.setMinWidth((int) (getResources().getDisplayMetrics().density * 40));
            rowLabelLeft.setGravity(android.view.Gravity.CENTER);
            row.addView(rowLabelLeft);

            for (int c = 0; c < totalCols; c++) {
                SeatMapResponse.SeatItem seat = seatGrid.get(r + "_" + c);

                if (seat != null) {
                    TextView seatBtn = new TextView(requireContext());
                    String seatText = seat.getSeatLabel() != null && seat.getSeatLabel().length() > 1
                            ? seat.getSeatLabel().substring(1)
                            : String.valueOf(seat.getColNumber());

                    seatBtn.setText(seatText);
                    seatBtn.setGravity(android.view.Gravity.CENTER);
                    seatBtn.setTextSize(10f);

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(seatSize, seatSize);
                    lp.setMargins(margin, 0, margin, 0);
                    seatBtn.setLayoutParams(lp);

                    // Track this button for real-time updates
                    seatButtonMap.put(seat.getShowtimeSeatId(), seatBtn);

                    boolean isInitiallySelected = viewModel.getSelectedSeatIds().contains(seat.getShowtimeSeatId());
                    updateSeatButtonStyle(seatBtn, seat, isInitiallySelected);

                    seatBtn.setOnClickListener(v -> {
                        boolean toggled = viewModel.toggleSeat(seat);
                        if (toggled) {
                            boolean isSelected = viewModel.getSelectedSeatIds().contains(seat.getShowtimeSeatId());
                            updateSeatButtonStyle(seatBtn, seat, isSelected);
                            updateSummary();
                        }
                    });
                    row.addView(seatBtn);
                } else {
                    View spacer = new View(requireContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(seatSize, seatSize);
                    lp.setMargins(margin, 0, margin, 0);
                    spacer.setLayoutParams(lp);
                    row.addView(spacer);
                }
            }
            // Right label
            TextView rowLabelRight = new TextView(requireContext());
            rowLabelRight.setText(label);
            rowLabelRight.setTextColor(getResources().getColor(R.color.on_surface_variant, null));
            rowLabelRight.setMinWidth((int) (getResources().getDisplayMetrics().density * 40));
            rowLabelRight.setGravity(android.view.Gravity.CENTER);
            row.addView(rowLabelRight);

            binding.seatContainer.addView(row);
        }
        updateSummary();

        binding.zoomLayout.post(() -> {
            binding.zoomLayout.zoomTo(1.0f, false);
        });
    }

    private void updateSeatButtonStyle(TextView tv, SeatMapResponse.SeatItem seat, boolean selected) {
        int colorRes;
        if (selected)
            colorRes = R.color.seat_selected;
        else if ("BOOKED".equals(seat.getStatus()) || "LOCKED".equals(seat.getStatus()))
            colorRes = R.color.seat_booked;
        else if ("VIP".equals(seat.getSeatType()))
            colorRes = R.color.seat_vip;
        else if ("COUPLE".equals(seat.getSeatType()))
            colorRes = R.color.seat_couple;
        else
            colorRes = R.color.seat_available;

        int color = getResources().getColor(colorRes, null);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(color);
        float radius = getResources().getDisplayMetrics().density * 6;
        gd.setCornerRadius(radius);

        if (!selected && !"BOOKED".equals(seat.getStatus())) {
            gd.setStroke(2, getResources().getColor(R.color.outline_variant, null));
        }

        tv.setBackground(gd);
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
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        binding = null;
    }
}
