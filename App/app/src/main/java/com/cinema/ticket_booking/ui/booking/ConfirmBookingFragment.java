package com.cinema.ticket_booking.ui.booking;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.*;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.util.SnackbarHelper;
import com.cinema.ticket_booking.databinding.FragmentConfirmBookingBinding;
import java.util.*;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConfirmBookingFragment extends Fragment {

    private FragmentConfirmBookingBinding binding;
    private ConfirmBookingViewModel viewModel;
    private CountDownTimer countDownTimer;

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
                java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .parse(dateStr);
                binding.tvShowDate.setText(
                        new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date));
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

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        // Voucher
        binding.btnApplyVoucher.setOnClickListener(v -> {
            String code = binding.etVoucher.getText().toString().trim();
            if (!code.isEmpty())
                viewModel.validateVoucher(code);
        });
        binding.btnClearVoucher.setOnClickListener(v -> {
            final String lastCode = binding.etVoucher.getText().toString().trim();
            viewModel.clearVoucher();
            binding.etVoucher.setText("");
            binding.tvVoucherInfo.setVisibility(View.GONE);
            binding.btnClearVoucher.setVisibility(View.GONE);

            SnackbarHelper.showWithAction(binding.getRoot(), "Đã gỡ mã giảm giá", "HOÀN TÁC", v2 -> {
                binding.etVoucher.setText(lastCode);
                viewModel.validateVoucher(lastCode);
            });
        });

        // Xác nhận đặt vé
        binding.btnConfirmBooking.setOnClickListener(v -> {
            viewModel.confirmBooking();
        });

        // ── Seed dữ liệu ban đầu từ Parcelable (Không gọi API lại nữa) ─────
        if (getArguments() != null) {
            BookingResponse initialQuote = getArguments().getParcelable("initialQuote");
            if (initialQuote != null) {
                viewModel.setInitialQuote(initialQuote);
            } else {
                viewModel.refreshQuote(); // Fallback nếu không có data (đi đường tắt)
            }
        } else {
            viewModel.refreshQuote();
        }

        // Khởi tạo đếm ngược dựa trên expireTime từ Bundle
        if (getArguments() != null) {
            long expireTime = getArguments().getLong("expireTime", 0);
            long remaining = expireTime - System.currentTimeMillis();
            if (remaining > 0) {
                startCountdown(remaining);
            } else {
                SnackbarHelper.showError(binding.getRoot(), "Hết thời gian giữ vé!");
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
                SnackbarHelper.showSuccess(binding.getRoot(), "Áp dụng mã giảm giá thành công!");
            } else if (resource.isError()) {
                SnackbarHelper.showError(binding.getRoot(), resource.message);
            }
        });

        viewModel.getQuoteResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.isSuccess() && resource.data != null) {
                updatePriceDetails(resource.data);
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
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                }
            }
        });
    }

    private void updatePriceDetails(BookingResponse quote) {
        Locale locale = Locale.getDefault();

        // 1. Tiền vé + Combo (Gốc)
        binding.tvSubtotal.setText(String.format(locale, "%,.0f ₫", quote.getTotalOriginalAmount()));

        // 2. Khuyến mãi hệ thống
        double promo = quote.getPromotionDiscountAmount();
        double voucherDiscount = quote.getDiscountAmount();

        // Hiển thị KM hệ thống
        if (promo > 0) {
            binding.tvPromotion.setVisibility(View.VISIBLE);
            binding.tvPromotion.setText(String.format(locale, "🎁 %s: -%,.0f ₫",
                    quote.getAppliedPromotionName() != null ? quote.getAppliedPromotionName() : "Khuyến mãi", promo));
        } else {
            binding.tvPromotion.setVisibility(View.GONE);
        }

        // Hiển thị Voucher
        if (voucherDiscount > 0) {
            binding.tvDiscount.setVisibility(View.VISIBLE);
            binding.tvDiscount.setText(String.format(locale, "🎫 Voucher: -%,.0f ₫", voucherDiscount));
        } else {
            binding.tvDiscount.setVisibility(View.GONE);
        }

        // Hiển thị cảnh báo voucher (nếu có)
        if (quote.getWarningMessage() != null) {
            binding.tvVoucherInfo.setText("⚠️ " + quote.getWarningMessage());
            binding.tvVoucherInfo.setTextColor(
                    getResources().getColor(R.id.btnBack != 0 ? R.color.primary : android.R.color.holo_red_dark));
            binding.tvVoucherInfo.setVisibility(View.VISIBLE);
        } else if (viewModel.getVoucher().getValue() != null && viewModel.getVoucher().getValue().isSuccess()) {
            binding.tvVoucherInfo.setTextColor(getResources().getColor(android.R.color.black));
        }

        // 3. Tổng cộng
        binding.tvTotal.setText(String.format(locale, "%,.0f ₫", quote.getTotalAmount()));
    }

    private void startCountdown(long duration) {
        countDownTimer = new android.os.CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null)
                    return;
                long minutes = millisUntilFinished / 1000 / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                binding.tvTimer.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                if (getContext() != null && binding != null) {
                    SnackbarHelper.showError(binding.getRoot(), "Hết thời gian giữ vé");
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
