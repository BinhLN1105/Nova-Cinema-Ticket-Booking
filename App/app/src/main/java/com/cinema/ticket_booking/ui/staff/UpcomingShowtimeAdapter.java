package com.cinema.ticket_booking.ui.staff;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.UpcomingShowtimeResponse;

import java.util.ArrayList;
import java.util.List;

public class UpcomingShowtimeAdapter extends RecyclerView.Adapter<UpcomingShowtimeAdapter.ViewHolder> {

    private List<UpcomingShowtimeResponse> items = new ArrayList<>();

    public void submitList(List<UpcomingShowtimeResponse> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_showtime, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpcomingShowtimeResponse item = items.get(position);
        holder.tvMovieTitle.setText(item.movieTitle);
        holder.tvScreenName.setText("🎬 " + item.screenName);

        // Parse startTime "HH:mm" from ISO string
        String timeDisplay = "--:--";
        if (item.startTime != null && item.startTime.length() >= 16) {
            timeDisplay = item.startTime.substring(11, 16); // "HH:mm"
        }
        holder.tvStartTime.setText(timeDisplay);

        // Badge urgency
        if (item.minutesUntilStart <= 0) {
            holder.tvUrgency.setText("Bắt đầu!");
            holder.tvUrgency.setTextColor(0xFFEF4444);
        } else if ("SOON".equals(item.urgency)) {
            holder.tvUrgency.setText(item.minutesUntilStart + " phút");
            holder.tvUrgency.setTextColor(0xFFEF4444); // Red khi gấp
        } else {
            holder.tvUrgency.setText(item.minutesUntilStart + " phút");
            holder.tvUrgency.setTextColor(0xFFF5C518); // Gold khi còn thời gian
        }

        // Load poster
        if (item.moviePosterUrl != null && !item.moviePosterUrl.isEmpty()) {
            Glide.with(holder.ivPoster.getContext())
                    .load(item.moviePosterUrl)
                    .placeholder(R.color.surface)
                    .centerCrop()
                    .into(holder.ivPoster);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvMovieTitle, tvStartTime, tvScreenName, tvUrgency;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvMovieTitle = itemView.findViewById(R.id.tvMovieTitle);
            tvStartTime = itemView.findViewById(R.id.tvStartTime);
            tvScreenName = itemView.findViewById(R.id.tvScreenName);
            tvUrgency = itemView.findViewById(R.id.tvUrgency);
        }
    }
}
