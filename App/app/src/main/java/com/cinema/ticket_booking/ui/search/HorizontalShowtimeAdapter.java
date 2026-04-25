package com.cinema.ticket_booking.ui.search;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.ShowtimeResponse;
import java.util.List;

public class HorizontalShowtimeAdapter extends RecyclerView.Adapter<HorizontalShowtimeAdapter.ShowtimeChipViewHolder> {

    private final List<ShowtimeResponse> showtimes;
    private final CinemaMovieAdapter.OnShowtimeClickListener listener;

    public HorizontalShowtimeAdapter(List<ShowtimeResponse> data, CinemaMovieAdapter.OnShowtimeClickListener listener) {
        this.showtimes = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShowtimeChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse item_showtime.xml if possible or create a simpler one
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_showtime, parent, false);
        return new ShowtimeChipViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowtimeChipViewHolder holder, int position) {
        ShowtimeResponse s = showtimes.get(position);
        holder.tvTime.setText(formatTime(s.getStartTime()));
        holder.itemView.setOnClickListener(v -> listener.onShowtimeClick(s));
    }

    @Override
    public int getItemCount() {
        return showtimes.size();
    }

    private String formatTime(String s) {
        if (s == null || !s.contains("T")) return s;
        try {
            return s.split("T")[1].substring(0, 5);
        } catch (Exception e) { return s; }
    }

    static class ShowtimeChipViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        public ShowtimeChipViewHolder(View v) {
            super(v);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}

