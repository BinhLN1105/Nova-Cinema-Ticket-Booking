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

        VH(ItemBookingBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(BookingSummary s) {
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
                    java.text.SimpleDateFormat sdfOut = new java.text.SimpleDateFormat("dd/MM/yyyy • HH:mm", java.util.Locale.getDefault());
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
                    
            if ("PENDING".equalsIgnoreCase(s.getStatus())) {
                b.btnViewTicket.setText("THANH TOÁN");
                b.btnViewTicket.setIconResource(0);
                b.btnViewTicket.setBackgroundTintList(android.content.res.ColorStateList.valueOf(b.getRoot().getContext().getColor(R.color.error)));
                b.btnViewTicket.setOnClickListener(v -> listener.onPayClick(s.getId()));
            } else {
                b.btnViewTicket.setText("VÉ ĐIỆN TỬ");
                b.btnViewTicket.setIconResource(R.drawable.ic_qr_code);
                b.btnViewTicket.setBackgroundTintList(android.content.res.ColorStateList.valueOf(b.getRoot().getContext().getColor(R.color.primary)));
                b.btnViewTicket.setOnClickListener(v -> listener.onClick(s.getId()));
            }
            b.getRoot().setOnClickListener(v -> listener.onClick(s.getId()));
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
    }
}
