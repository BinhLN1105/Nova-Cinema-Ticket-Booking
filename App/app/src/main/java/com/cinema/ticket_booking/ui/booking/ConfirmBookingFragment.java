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
    private android.os.CountDownTimer countDownTimer;

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

        int cbCount = 0;
        for (Integer qty : SelectComboViewModel.pendingCombos.values()) {
            cbCount += qty;
        }
        binding.tvComboCount.setText(cbCount + " phần");

        // Format and set date
        String dateStr = SelectShowtimeViewModel.pendingShowDate;
        if (dateStr != null) {
            try {
                java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateStr);
                binding.tvShowDate.setText(new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date));
            } catch (Exception e) {
                binding.tvShowDate.setText(dateStr);
            }
        }

        if (SelectShowtimeViewModel.pendingMoviePoster != null) {
            com.bumptech.glide.Glide.with(this)
                    .load(SelectShowtimeViewModel.pendingMoviePoster)
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .into(binding.ivPoster);
        }

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

        // Khởi tạo đếm ngược dựa trên expireTime từ Bundle
        if (getArguments() != null) {
            long expireTime = getArguments().getLong("expireTime", 0);
            long remaining = expireTime - System.currentTimeMillis();
            if (remaining > 0) {
                startCountdown(remaining);
            } else {
                Toast.makeText(requireContext(), "Hết thời gian giữ vé!", Toast.LENGTH_LONG).show();
                Navigation.findNavController(view).popBackStack(R.id.selectSeatFragment, false);
            }
        } else {
            // Fallback nếu không có bundle (không nên xảy ra)
            startCountdown(5 * 60 * 1000);
        }
        setupObservers(view);
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
        
        viewModel.getCombos().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.isSuccess()) {
                updatePriceSummary();
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
        double subtotal = SelectSeatViewModel.pendingTotalAmount + viewModel.calculateComboTotal();
        double discount = viewModel.calculateDiscount(subtotal);
        double total = subtotal - discount;
        binding.tvSubtotal.setText(String.format("%,.0f", subtotal));
        binding.tvDiscount.setText(discount > 0 ? "-" + String.format("%,.0f", discount) : "0");
        binding.tvTotal.setText(String.format("%,.0f", total));
    }

    private void startCountdown(long duration) {
        countDownTimer = new android.os.CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null) return;
                long minutes = millisUntilFinished / 1000 / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                binding.tvTimer.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                if (getContext() != null && binding != null) {
                    Toast.makeText(requireContext(), "Hết thời gian giữ vé", Toast.LENGTH_LONG).show();
                    // Go back to select seat or movie detail
                    Navigation.findNavController(requireView()).popBackStack();
                }
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        binding = null;
    }
}
