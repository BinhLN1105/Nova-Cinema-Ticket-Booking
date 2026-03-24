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
            b.tvCinema.setText(s.getCinemaName());
            b.tvShowtime.setText(s.getStartTime());
            b.tvTotal.setText(String.format("%,.0fđ", s.getTotalAmount()));
            b.tvStatus.setText(s.getStatus());
            int color = switch (s.getStatus()) {
                case "PAID" -> R.color.seat_available;
                case "PENDING" -> R.color.tertiary;
                default -> R.color.error;
            };
            b.tvStatus.setTextColor(b.getRoot().getContext().getColor(color));
            Glide.with(b.ivPoster.getContext()).load(s.getMoviePosterUrl())
                    .placeholder(R.drawable.ic_movie_placeholder).into(b.ivPoster);
            b.getRoot().setOnClickListener(v -> listener.onClick(s.getId()));
        }
    }
}
