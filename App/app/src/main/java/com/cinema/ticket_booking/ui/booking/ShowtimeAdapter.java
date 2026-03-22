package com.cinema.ticket_booking.ui.booking;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.ShowtimeResponse;
import java.util.List;

public class ShowtimeAdapter extends RecyclerView.Adapter<ShowtimeAdapter.VH> {
    public interface Listener {
        void onClick(ShowtimeResponse s);
    }

    private final List<ShowtimeResponse> items;
    private final Listener listener;

    public ShowtimeAdapter(List<ShowtimeResponse> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_showtime, p, false);
        return new VH(v);
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
        TextView tvTime, tvType, tvPrice, tvSeats;

        VH(View v) {
            super(v);
            tvTime = v.findViewById(R.id.tvTime);
            tvType = v.findViewById(R.id.tvType);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvSeats = v.findViewById(R.id.tvSeats);
        }

        void bind(ShowtimeResponse s) {
            String time = s.getStartTime();
            if (time != null && time.length() >= 16)
                time = time.substring(11, 16);
            tvTime.setText(time);
            tvType.setText(s.getScreenType());
            tvPrice.setText(String.format("%,.0fđ", s.getBasePrice()));
            tvSeats.setText(s.getAvailableSeats() + " ghế trống");
            itemView.setAlpha("CANCELLED".equals(s.getStatus()) ? 0.4f : 1f);
            itemView.setEnabled(!"CANCELLED".equals(s.getStatus()));
            itemView.setOnClickListener(v -> listener.onClick(s));
        }
    }
}
