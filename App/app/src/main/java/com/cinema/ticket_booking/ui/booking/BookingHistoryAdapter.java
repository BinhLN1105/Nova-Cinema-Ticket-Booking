package com.cinema.ticket_booking.ui.booking;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.BookingSummary;
import com.cinema.ticket_booking.databinding.ItemBookingBinding;
import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.VH> {
    public interface Listener {
        void onClick(String bookingId);

        void onPayClick(String bookingId);

        void onReviewClick(BookingSummary s);

        void onCancelClick(BookingSummary s);
    }

    private final List<BookingSummary> items;
    private final Listener listener;

    public BookingHistoryAdapter(List<BookingSummary> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(ItemBookingBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.bind(items.get(pos));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemBookingBinding b;
        private android.os.CountDownTimer countDownTimer;

        VH(ItemBookingBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(BookingSummary s) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            b.tvMovieTitle.setText(s.getMovieTitle());
            b.tvCinema.setText("CineNoir " + s.getCinemaName());
            b.tvFormat.setText(formatScreenType(s.getScreenType()));

            // Format time
            String rawTime = s.getStartTime();
            if (rawTime != null) {
                try {
                    java.text.SimpleDateFormat sdfIn;
                    if (rawTime.contains("T")) {
                        sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                    } else {
                        sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                    }
                    java.util.Date d = sdfIn.parse(rawTime);
                    java.text.SimpleDateFormat sdfOut = new java.text.SimpleDateFormat("dd/MM/yyyy • HH:mm",
                            java.util.Locale.getDefault());
                    b.tvShowtime.setText(sdfOut.format(d));
                } catch (Exception e) {
                    b.tvShowtime.setText(rawTime);
                }
            } else {
                b.tvShowtime.setText("N/A");
            }

            // Format Seat
            String screen = s.getScreenName() != null ? s.getScreenName() : "";
            String seats = s.getSeats() != null ? s.getSeats() : "";

            if (seats.isEmpty() && screen.isEmpty()) {
                b.tvSeat.setText("Mã vé: " + (s.getBookingCode() != null ? s.getBookingCode() : "N/A"));
            } else {
                b.tvSeat.setText(screen + " - " + seats);
            }

            // We can optionally show total amount or status if we want
            // b.tvTotal.setText(String.format("%,.0fđ", s.getTotalAmount()));

            Glide.with(b.ivPoster.getContext()).load(s.getMoviePosterUrl())
                    .placeholder(R.drawable.ic_movie_placeholder).into(b.ivPoster);

            // Display Status Tag
            String status = s.getStatus() != null ? s.getStatus().toUpperCase() : "UNKNOWN";
            b.tvStatus.setText(formatStatusText(status));
            b.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getStatusColor(b.getRoot().getContext(), status)
            ));

            if ("PENDING".equalsIgnoreCase(s.getStatus())) {
                b.btnReview.setVisibility(View.GONE);

                // Kiểm tra vé PENDING đã quá hạn thanh toán chưa
                boolean isExpired = false;
                if (s.getExpiresAt() != null) {
                    try {
                        java.text.SimpleDateFormat sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                        java.util.Date expDate = sdfIn.parse(s.getExpiresAt());
                        if (expDate != null && expDate.getTime() <= System.currentTimeMillis()) {
                            isExpired = true;
                        }
                    } catch (Exception ignored) {}
                }

                if (isExpired) {
                    // Vé PENDING đã quá hạn: hiển thị nút "ĐÃ HẾT HẠN" bị vô hiệu hoá
                    b.tvCountdown.setVisibility(View.VISIBLE);
                    b.tvCountdown.setText("Đã quá hạn");
                    b.btnViewTicket.setText("ĐÃ HẾT HẠN");
                    b.btnViewTicket.setIconResource(0);
                    b.btnViewTicket.setEnabled(false);
                    b.btnViewTicket.setAlpha(0.6f);
                    b.btnViewTicket.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
                    b.btnViewTicket.setOnClickListener(null);
                    // Cập nhật status tag
                    b.tvStatus.setText("HẾT HẠN");
                    b.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#9E9E9E")));
                } else {
                    // Vé PENDING còn hạn: hiển thị nút thanh toán + countdown
                    b.btnViewTicket.setText("THANH TOÁN");
                    b.btnViewTicket.setIconResource(0);
                    b.btnViewTicket.setEnabled(true);
                    b.btnViewTicket.setAlpha(1f);
                    b.btnViewTicket.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(b.getRoot().getContext().getColor(R.color.error)));
                    b.btnViewTicket.setOnClickListener(v -> listener.onPayClick(s.getId()));
                
                    // Countdown Logic
                    if (s.getExpiresAt() != null) {
                        try {
                            java.text.SimpleDateFormat sdfIn = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                            java.util.Date expDate = sdfIn.parse(s.getExpiresAt());
                            if (expDate != null) {
                                long diffInMillis = expDate.getTime() - System.currentTimeMillis();
                                if (diffInMillis > 0) {
                                    b.tvCountdown.setVisibility(View.VISIBLE);
                                    countDownTimer = new android.os.CountDownTimer(diffInMillis, 1000) {
                                        public void onTick(long millisUntilFinished) {
                                            long m = (millisUntilFinished / 1000) / 60;
                                            long sec = (millisUntilFinished / 1000) % 60;
                                            b.tvCountdown.setText(String.format(java.util.Locale.getDefault(), "Hủy sau: %02d:%02d", m, sec));
                                        }
                                        public void onFinish() {
                                            b.tvCountdown.setText("Đã quá hạn");
                                            // Khi countdown chạy xong, disable nút thanh toán luôn
                                            b.btnViewTicket.setText("ĐÃ HẾT HẠN");
                                            b.btnViewTicket.setEnabled(false);
                                            b.btnViewTicket.setAlpha(0.6f);
                                            b.btnViewTicket.setBackgroundTintList(
                                                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
                                            b.btnViewTicket.setOnClickListener(null);
                                            b.tvStatus.setText("HẾT HẠN");
                                            b.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                                    android.graphics.Color.parseColor("#9E9E9E")));
                                        }
                                    }.start();
                                } else {
                                    b.tvCountdown.setVisibility(View.VISIBLE);
                                    b.tvCountdown.setText("Đã quá hạn");
                                }
                            }
                        } catch (Exception ignored) {}
                    } else {
                        b.tvCountdown.setVisibility(View.GONE);
                    }
                }
                
            } else if ("CANCELLED".equalsIgnoreCase(s.getStatus())) {
                // Vé đã huỷ: nút Hủy vé (btnReview) chuyển thành "ĐÃ HỦY VÉ", xám đi và vô hiệu hóa
                b.tvCountdown.setVisibility(View.GONE);
                
                b.btnViewTicket.setText("VÉ ĐIỆN TỬ");
                b.btnViewTicket.setIconResource(R.drawable.ic_qr_code);
                b.btnViewTicket.setEnabled(true);
                b.btnViewTicket.setAlpha(1f);
                b.btnViewTicket.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(b.getRoot().getContext().getColor(R.color.primary)));
                b.btnViewTicket.setOnClickListener(v -> listener.onClick(s.getId()));

                b.btnReview.setVisibility(View.VISIBLE);
                b.btnReview.setText("ĐÃ HỦY VÉ");
                b.btnReview.setIconResource(0);
                b.btnReview.setEnabled(false);
                b.btnReview.setAlpha(0.6f);
                b.btnReview.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0")));
                b.btnReview.setTextColor(android.graphics.Color.parseColor("#757575"));
                b.btnReview.setStrokeWidth(0); // Bỏ border
                b.btnReview.setOnClickListener(null);
            } else {
                b.tvCountdown.setVisibility(View.GONE);
                b.btnViewTicket.setText("VÉ ĐIỆN TỬ");
                b.btnViewTicket.setIconResource(R.drawable.ic_qr_code);
                b.btnViewTicket.setEnabled(true);
                b.btnViewTicket.setAlpha(1f);
                b.btnViewTicket.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(b.getRoot().getContext().getColor(R.color.primary)));
                b.btnViewTicket.setOnClickListener(v -> listener.onClick(s.getId()));

                // Show Review button if status is CHECKED_IN or PAID+Past
                boolean isPast = false;
                if (s.getStartTime() != null) {
                    try {
                        java.text.SimpleDateFormat sdfIn = s.getStartTime().contains("T") 
                            ? new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                            : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                        java.util.Date startDate = sdfIn.parse(s.getStartTime());
                        if (startDate != null && startDate.getTime() < System.currentTimeMillis()) {
                            isPast = true;
                        }
                    } catch (Exception ignored) {}
                }

                if ("CHECKED_IN".equalsIgnoreCase(s.getStatus()) || 
                   ("PAID".equalsIgnoreCase(s.getStatus()) && isPast)) {
                    b.btnReview.setVisibility(View.VISIBLE);
                    b.btnReview.setText("ĐÁNH GIÁ");
                    b.btnReview.setIconResource(0);
                    b.btnReview.setEnabled(true);
                    b.btnReview.setAlpha(1f);
                    b.btnReview.setTextColor(b.getRoot().getContext().getColor(R.color.on_surface));
                    b.btnReview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            b.getRoot().getContext().getColor(R.color.primary)));
                    b.btnReview.setOnClickListener(v -> listener.onReviewClick(s));
                } else if ("PAID".equalsIgnoreCase(s.getStatus())) {
                    // Show Cancel button for PAID upcoming tickets
                    b.btnReview.setVisibility(View.VISIBLE);
                    b.btnReview.setText("HỦY VÉ");
                    b.btnReview.setIconResource(0);
                    b.btnReview.setEnabled(true);
                    b.btnReview.setAlpha(1f);
                    b.btnReview.setTextColor(b.getRoot().getContext().getColor(R.color.on_surface));
                    b.btnReview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            b.getRoot().getContext().getColor(R.color.error)));
                    b.btnReview.setOnClickListener(v -> listener.onCancelClick(s));
                } else {
                    b.btnReview.setVisibility(View.GONE);
                }
            }
            b.getRoot().setOnClickListener(v -> listener.onClick(s.getId()));
        }

        private String formatScreenType(String rawType) {
            if (rawType == null)
                return "2D";
            switch (rawType.toUpperCase()) {
                case "STANDARD":
                    return "2D";
                case "THREE_D":
                    return "3D";
                case "IMAX":
                    return "IMAX";
                case "FOUR_DX":
                    return "4DX";
                default:
                    return rawType;
            }
        }

        private String formatStatusText(String status) {
            switch (status) {
                case "PAID": return "ĐÃ TT";
                case "PENDING": return "CHỜ TT";
                case "CHECKED_IN": return "ĐÃ QUÉT";
                case "EXPIRED": return "HẾT HẠN";
                case "CANCELLED": return "ĐÃ HỦY";
                default: return status;
            }
        }

        private int getStatusColor(android.content.Context ctx, String status) {
            switch (status) {
                case "PAID": return android.graphics.Color.parseColor("#4CAF50"); // Green
                case "PENDING": return android.graphics.Color.parseColor("#FF9800"); // Orange
                case "CHECKED_IN": return android.graphics.Color.parseColor("#2196F3"); // Blue
                case "EXPIRED": return android.graphics.Color.parseColor("#9E9E9E"); // Grey
                case "CANCELLED": return android.graphics.Color.parseColor("#F44336"); // Red
                default: return android.graphics.Color.parseColor("#9E9E9E");
            }
        }
    }
}

