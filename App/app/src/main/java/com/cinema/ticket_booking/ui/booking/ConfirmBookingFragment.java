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
import com.cinema.ticket_booking.ui.MainViewModel;
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
    private MainViewModel mainViewModel;
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
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

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

        // Voucher Station
        View.OnClickListener showVoucherSheet = v -> {
            double cartTotal = (viewModel.getQuoteResult().getValue() != null
                    && viewModel.getQuoteResult().getValue().data != null
                    && viewModel.getQuoteResult().getValue().data.getSubtotal() != null)
                            ? viewModel.getQuoteResult().getValue().data.getSubtotal()
                            : 0.0;

            VoucherSelectionBottomSheet sheet = VoucherSelectionBottomSheet.newInstance(
                    viewModel.getMyVouchers(), cartTotal);

            sheet.setListener(new VoucherSelectionBottomSheet.OnVoucherSelectedListener() {
                @Override
                public void onVoucherSelected(VoucherSummary voucher) {
                    viewModel.applyVoucherDirectly(voucher);
                }

                @Override
                public void onManualVoucherApplied(String code) {
                    viewModel.validateVoucher(code);
                }
            });
            sheet.show(getChildFragmentManager(), "VoucherSheet");
        };

        binding.cvVoucherStation.setOnClickListener(showVoucherSheet);
        binding.btnSelectVoucherAction.setOnClickListener(showVoucherSheet);

        binding.btnClearVoucher.setOnClickListener(v -> {
            viewModel.clearVoucher();
            SnackbarHelper.showSuccess(binding.getRoot(), "Đã gỡ mã ưu đãi");
        });

        // Payment methods select logic
        binding.cvWallet.setOnClickListener(v -> {
            binding.rbWallet.setChecked(true);
            binding.rbVnpay.setChecked(false);
            binding.rbMomo.setChecked(false);
        });
        binding.cvVnpay.setOnClickListener(v -> {
            binding.rbVnpay.setChecked(true);
            binding.rbWallet.setChecked(false);
            binding.rbMomo.setChecked(false);
        });
        binding.cvMomo.setOnClickListener(v -> {
            binding.rbMomo.setChecked(true);
            binding.rbWallet.setChecked(false);
            binding.rbVnpay.setChecked(false);
        });
        binding.rbWallet.setOnClickListener(v -> {
            binding.rbWallet.setChecked(true);
            binding.rbVnpay.setChecked(false);
            binding.rbMomo.setChecked(false);
        });
        binding.rbVnpay.setOnClickListener(v -> {
            binding.rbVnpay.setChecked(true);
            binding.rbWallet.setChecked(false);
            binding.rbMomo.setChecked(false);
        });
        binding.rbMomo.setOnClickListener(v -> {
            binding.rbMomo.setChecked(true);
            binding.rbWallet.setChecked(false);
            binding.rbVnpay.setChecked(false);
        });

        // Xác nhận đặt vé
        binding.btnConfirmBooking.setOnClickListener(v -> {
            if (binding.rbWallet.isChecked()) {
                viewModel.confirmBookingAndPayWithWallet();
            } else {
                viewModel.confirmBooking();
            }
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

        viewModel.getUserProfile().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.isSuccess() && resource.data != null) {
                binding.tvWalletBalance.setText(String.format(Locale.getDefault(),
                        "Số dư hiện tại: %,d CP (≈ %,.0f ₫)",
                        resource.data.getCinePoints(),
                        resource.data.getCinePoints() * 1000.0));
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

                        if ("PAID".equals(resource.data.getStatus())) {
                            mainViewModel.refreshUserProfile();
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_confirmBooking_to_bookingDetail, args);
                        } else {
                            // Default flow: go to Payment (WebView)
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_confirmBooking_to_payment, args);
                        }
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnConfirmBooking.setEnabled(true);
                    SnackbarHelper.showError(binding.getRoot(), resource.message);
                }
            }
        });

        // Observer cho kết quả thanh toán ví (Hybrid Flow)
        viewModel.getWalletPaymentResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null)
                return;
            switch (resource.status) {
                case LOADING -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.btnConfirmBooking.setEnabled(false);
                }
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null) {
                        if (resource.data.getRemainingAmount() != null && resource.data.getRemainingAmount() > 0) {
                            // Hybrid Flow: Tiếp tục thanh toán phần còn lại qua VNPay
                            Bundle args = new Bundle();
                            args.putString("bookingId", resource.data.getBookingId());
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_confirmBooking_to_payment, args);
                            Toast.makeText(getContext(), "Vui lòng thanh toán số tiền còn lại qua VNPay",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Full CinePoint Payment
                            mainViewModel.refreshUserProfile();
                            Bundle args = new Bundle();
                            args.putString("bookingId", resource.data.getBookingId());
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_confirmBooking_to_bookingDetail, args);
                            Toast.makeText(getContext(), "Thanh toán bằng CinePoint thành công!", Toast.LENGTH_SHORT)
                                    .show();
                        }
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
        binding.tvSubtotal.setText(String.format(locale, "%,.0f ₫",
                quote.getTotalOriginalAmount() != null ? quote.getTotalOriginalAmount() : 0.0));

        // 2. Khuyến mãi hệ thống
        double promo = quote.getPromotionDiscountAmount() != null ? quote.getPromotionDiscountAmount() : 0.0;
        double voucherDiscount = quote.getDiscountAmount() != null ? quote.getDiscountAmount() : 0.0;

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

        // Cập nhật Voucher Station UI
        VoucherSummary applied = viewModel.getAppliedVoucher();
        if (applied != null) {
            binding.tvVoucherStationTitle.setText("✓ Mã Tốt Nhất: " + applied.getCode());
            binding.tvVoucherStationTitle.setTextColor(0xFF4CAF50); // Green
            binding.tvVoucherStationDesc.setText(applied.getDescription());
            binding.btnClearVoucher.setVisibility(View.VISIBLE);
        } else {
            binding.tvVoucherStationTitle.setText("Chưa chọn mã giảm giá");
            binding.tvVoucherStationTitle.setTextColor(getResources().getColor(R.color.on_surface_variant));
            binding.tvVoucherStationDesc.setText("Nhấn để chọn hoặc nhập mã...");
            binding.btnClearVoucher.setVisibility(View.GONE);
        }

        // Hiển thị cảnh báo voucher (nếu có)
        if (quote.getWarningMessage() != null) {
            binding.tvVoucherInfo.setText("⚠️ " + quote.getWarningMessage());
            binding.tvVoucherInfo.setVisibility(View.VISIBLE);
        } else {
            binding.tvVoucherInfo.setVisibility(View.GONE);
        }

        // 3. Tổng cộng
        binding.tvTotal.setText(String.format(locale, "%,.0f ₫",
                quote.getTotalAmount() != null ? quote.getTotalAmount() : 0.0));

        // 4. CinePoint (Hybrid Flow Status)
        if (quote.getPointDiscount() != null && quote.getPointDiscount() > 0) {
            binding.tvWalletBalance.setText(String.format(locale, "Sử dụng %,d CP: -%,.0f ₫ (Còn lại: %,.0f ₫)",
                    quote.getPointsUsed(), quote.getPointDiscount(), quote.getRemainingAmount()));
        }
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
