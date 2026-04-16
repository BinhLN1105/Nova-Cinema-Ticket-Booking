package com.cinema.ticket_booking.ui.booking;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import com.cinema.ticket_booking.util.SnackbarHelper;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentBookingDetailBinding;
import com.cinema.ticket_booking.util.TicketCacheManager;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class BookingDetailFragment extends Fragment {

    private FragmentBookingDetailBinding binding;
    private BookingDetailViewModel viewModel;
    private boolean isCodeVisible = false;
    private String rawBookingCode = "";

    @Inject
    TicketCacheManager ticketCacheManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        binding = FragmentBookingDetailBinding.inflate(i, c, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BookingDetailViewModel.class);

        String bookingId = getArguments() != null ? getArguments().getString("bookingId") : null;
        if (bookingId != null)
            viewModel.loadBooking(bookingId);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        binding.btnBackTop.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        binding.ivToggleCode.setOnClickListener(v -> {
            isCodeVisible = !isCodeVisible;
            updateBookingCodeDisplay();
        });

        binding.btnShare.setOnClickListener(v -> {
            if (rawBookingCode.isEmpty()) return;
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Thông tin vé xem phim");
            String shareMessage = "Mình đã đặt vé xem phim *" + binding.tvMovieTitle.getText().toString() + "*\n" +
                                  "Tại: " + binding.tvCinema.getText().toString() + "\n" +
                                  "Lúc: " + binding.tvShowtime.getText().toString() + "\n" +
                                  "Mã đặt vé: " + rawBookingCode;
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);
            startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ vé"));
        });

        viewModel.getBooking().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> binding.progressBar.setVisibility(View.VISIBLE);
                case SUCCESS -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null) {
                        renderBooking(resource.data, false);
                    }
                }
                case ERROR -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (resource.data != null) {
                        renderBooking(resource.data, true);
                        if (resource.message != null && !resource.message.isEmpty()) {
                            SnackbarHelper.showSuccess(binding.getRoot(), resource.message);
                        }
                    } else {
                        SnackbarHelper.showError(binding.getRoot(), resource.message);
                    }
                }
            }
        });
    }

    private void renderBooking(com.cinema.ticket_booking.data.model.response.BookingResponse b, boolean isOffline) {
        rawBookingCode = b.getBookingCode();
        updateBookingCodeDisplay();
        binding.tvMovieTitle.setText(b.getMovieTitle());
        binding.tvCinema.setText("CineNoir " + b.getCinemaName());
        binding.tvScreen.setText(b.getScreenName());
        binding.tvFormat.setText(formatScreenType(b.getScreenType()));
        
        // Format showtime
        String rawTime = b.getStartTime();
        if (rawTime != null) {
            try {
                java.text.SimpleDateFormat sdfIn;
                if (rawTime.contains("T")) {
                    sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                } else {
                    sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                }
                java.util.Date d = sdfIn.parse(rawTime);
                java.text.SimpleDateFormat sdfOut = new java.text.SimpleDateFormat("h:mm a - dd/MM/yyyy", java.util.Locale.getDefault());
                binding.tvShowtime.setText(sdfOut.format(d));
            } catch (Exception e) {
                binding.tvShowtime.setText(rawTime);
            }
        } else {
            binding.tvShowtime.setText("");
        }
        
        // Format status
        String statusText = switch (b.getStatus() != null ? b.getStatus() : "") {
            case "PAID" -> "ĐÃ THANH TOÁN";
            case "PENDING" -> "CHỜ THANH TOÁN";
            case "CANCELLED" -> "ĐÃ HỦY";
            case "EXPIRED" -> "HẾT HẠN";
            default -> b.getStatus();
        };

        binding.tvStatus.setText(statusText);
        
        binding.tvTotal.setText(String.format("Tổng thanh toán: %,.0fđ", b.getTotalAmount()));

        // Danh sách ghế
        if (b.getSeats() != null) {
            StringBuilder seats = new StringBuilder();
            for (int i = 0; i < b.getSeats().size(); i++) {
                var s = b.getSeats().get(i);
                seats.append(s.getRowLabel()).append(s.getColNumber());
                if (i < b.getSeats().size() - 1) {
                    seats.append(", ");
                }
            }
            binding.tvSeats.setText(seats.toString().trim());
        }

        // Danh sách bắp nước
        if (b.getCombos() != null && !b.getCombos().isEmpty()) {
            StringBuilder combosStr = new StringBuilder();
            for (int k = 0; k < b.getCombos().size(); k++) {
                var cb = b.getCombos().get(k);
                combosStr.append(cb.getQuantity()).append("x ").append(cb.getComboName());
                if (k < b.getCombos().size() - 1) {
                    combosStr.append("\n");
                }
            }
            binding.tvCombos.setText(combosStr.toString().trim());
            binding.layoutCombos.setVisibility(View.VISIBLE);
        } else {
            binding.layoutCombos.setVisibility(View.GONE);
        }

        // Poster
        if (getContext() != null) {
            Glide.with(this).load(b.getMoviePosterUrl())
                    .placeholder(R.drawable.ic_movie_placeholder).into(binding.ivPoster);
        }

        // QR Code
        if (b.getQrCode() != null && !b.getQrCode().isEmpty()) {
            generateQrCode(b.getQrCode());
        } else {
            binding.ivQrCode.setVisibility(View.GONE);
        }

        // Trạng thái màu
        int statusColor = switch (b.getStatus() != null ? b.getStatus() : "") {
            case "PAID" -> R.color.seat_available;
            case "PENDING" -> R.color.tertiary;
            case "CANCELLED", "EXPIRED" -> R.color.error;
            default -> R.color.on_surface_variant;
        };
        binding.tvStatus.setTextColor(getResources().getColor(statusColor, null));

        if ("PENDING".equals(b.getStatus()) && !isOffline) {
            binding.btnShare.setVisibility(View.GONE);
            binding.btnPay.setVisibility(View.VISIBLE);
            binding.btnPay.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("bookingId", b.getId());
                if (getView() != null) {
                    Navigation.findNavController(getView()).navigate(R.id.action_bookingDetail_to_payment, args);
                }
            });
        } else {
            binding.btnShare.setVisibility(View.VISIBLE);
            binding.btnPay.setVisibility(View.GONE);
        }
    }

    private void updateBookingCodeDisplay() {
        if (binding == null) return;
        if (isCodeVisible) {
            binding.tvBookingCode.setText(rawBookingCode);
            binding.ivToggleCode.setImageResource(R.drawable.ic_eye);
            binding.tvBookingCode.setLetterSpacing(0.3f);
        } else {
            binding.tvBookingCode.setText("••••••");
            binding.ivToggleCode.setImageResource(R.drawable.ic_eye_off);
            binding.tvBookingCode.setLetterSpacing(0.8f);
        }
    }

    private void generateQrCode(String content) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            android.graphics.Bitmap bitmap = encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 400, 400);
            binding.ivQrCode.setImageBitmap(bitmap);
            binding.ivQrCode.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            binding.ivQrCode.setVisibility(View.GONE);
        }
    }

    private String formatScreenType(String rawType) {
        if (rawType == null) return "2D";
        switch (rawType.toUpperCase()) {
            case "STANDARD": return "2D";
            case "THREE_D": return "3D";
            case "IMAX": return "IMAX";
            case "FOUR_DX": return "4DX";
            default: return rawType;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

