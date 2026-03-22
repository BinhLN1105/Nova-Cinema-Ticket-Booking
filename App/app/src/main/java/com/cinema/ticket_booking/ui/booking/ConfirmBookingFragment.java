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
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.databinding.FragmentConfirmBookingBinding;
import java.util.*;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConfirmBookingFragment extends Fragment {

    private FragmentConfirmBookingBinding binding;
    private ConfirmBookingViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentConfirmBookingBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ConfirmBookingViewModel.class);

        // Hiển thị thông tin đã chọn
        binding.tvMovieTitle.setText(SelectShowtimeViewModel.pendingMovieTitle);
        binding.tvCinemaName.setText(SelectShowtimeViewModel.pendingCinemaName);
        binding.tvShowtimeTime.setText(SelectShowtimeViewModel.pendingShowtimeTime);
        binding.tvSeatCount.setText(SelectSeatViewModel.pendingSeatIds.size() + " ghế");
        updatePriceSummary();

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        // Voucher
        binding.btnApplyVoucher.setOnClickListener(v -> {
            String code = binding.etVoucher.getText().toString().trim();
            if (!code.isEmpty())
                viewModel.validateVoucher(code);
        });
        binding.btnClearVoucher.setOnClickListener(v -> {
            viewModel.clearVoucher();
            binding.etVoucher.setText("");
            binding.tvVoucherInfo.setVisibility(View.GONE);
            binding.btnClearVoucher.setVisibility(View.GONE);
            updatePriceSummary();
        });

        // Xác nhận đặt vé
        binding.btnConfirmBooking.setOnClickListener(v -> {
            viewModel.confirmBooking(SelectShowtimeViewModel.pendingShowtimeId);
        });

        setupObservers(view);
        setupComboList();
    }

    private void setupComboList() {
        viewModel.getCombos().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.data != null) {
                binding.rvCombos.setLayoutManager(new LinearLayoutManager(requireContext()));
                binding.rvCombos.setAdapter(new ComboAdapter(resource.data,
                        comboId -> {
                            viewModel.addCombo(comboId);
                            updatePriceSummary();
                        },
                        comboId -> {
                            viewModel.removeCombo(comboId);
                            updatePriceSummary();
                        },
                        viewModel.getSelectedCombos()));
            }
        });
    }

    private void setupObservers(View view) {
        viewModel.getVoucher().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null)
                return;
            if (resource.isSuccess() && resource.data != null) {
                binding.tvVoucherInfo.setText("✓ " + resource.data.getDescription());
                binding.tvVoucherInfo.setVisibility(View.VISIBLE);
                binding.btnClearVoucher.setVisibility(View.VISIBLE);
                updatePriceSummary();
            } else if (resource.isError()) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getBookingResult().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.btnConfirmBooking.setEnabled(false);
                }
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null) {
                        Bundle args = new Bundle();
                        args.putString("bookingId", resource.data.getId());
                        Navigation.findNavController(view)
                                .navigate(R.id.action_confirmBooking_to_payment, args);
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnConfirmBooking.setEnabled(true);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updatePriceSummary() {
        double subtotal = SelectSeatViewModel.pendingTotalAmount;
        double discount = viewModel.calculateDiscount(subtotal);
        double total = subtotal - discount;
        binding.tvSubtotal.setText(String.format("%,.0fđ", subtotal));
        binding.tvDiscount.setText(discount > 0 ? "-" + String.format("%,.0fđ", discount) : "0đ");
        binding.tvTotal.setText(String.format("%,.0fđ", total));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
