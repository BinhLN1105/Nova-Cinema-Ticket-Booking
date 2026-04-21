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
import com.cinema.ticket_booking.data.model.response.CheckInHistoryItemResponse;

import java.util.ArrayList;
import java.util.List;

public class CheckInHistoryAdapter extends RecyclerView.Adapter<CheckInHistoryAdapter.ViewHolder> {

    private List<CheckInHistoryItemResponse> items = new ArrayList<>();

    public void submitList(List<CheckInHistoryItemResponse> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_check_in_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CheckInHistoryItemResponse item = items.get(position);

        // Movie title
        holder.tvMovieTitle.setText(item.movieTitle != null ? item.movieTitle : "Không xác định");

        // Screen + Seats
        String screenSeats = "";
        if (item.screenName != null) screenSeats += item.screenName;
        if (item.seatsChecked != null && !item.seatsChecked.isEmpty()) {
            screenSeats += " • " + item.seatsChecked;
        }
        holder.tvScreenAndSeats.setText(screenSeats.isEmpty() ? "-" : screenSeats);

        // Customer name
        holder.tvCustomerName.setText(item.customerName != null ? item.customerName : "Khách vãng lai");

        // Time
        String timeDisplay = "--:--";
        if (item.scannedAt != null && item.scannedAt.length() >= 16) {
            timeDisplay = item.scannedAt.substring(11, 16);
        }
        holder.tvScannedAt.setText(timeDisplay);

        // Status badge
        if (item.success) {
            holder.tvStatusBadge.setText("✓ Thành công");
            holder.tvStatusBadge.setTextColor(0xFF22C55E);
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_success);
            holder.tvFailReason.setVisibility(View.GONE);
        } else {
            holder.tvStatusBadge.setText("✗ Thất bại");
            holder.tvStatusBadge.setTextColor(0xFFEF4444);
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_fail);
            if (item.failReason != null && !item.failReason.isEmpty()) {
                holder.tvFailReason.setVisibility(View.VISIBLE);
                holder.tvFailReason.setText("↳ " + item.failReason);
            } else {
                holder.tvFailReason.setVisibility(View.GONE);
            }
        }

        // Movie poster
        if (item.moviePosterUrl != null && !item.moviePosterUrl.isEmpty()) {
            Glide.with(holder.ivMoviePoster.getContext())
                    .load(item.moviePosterUrl)
                    .placeholder(R.color.surface)
                    .centerCrop()
                    .into(holder.ivMoviePoster);
        } else {
            holder.ivMoviePoster.setImageResource(R.color.surface);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMoviePoster;
        TextView tvMovieTitle, tvScreenAndSeats, tvCustomerName, tvScannedAt,
                tvStatusBadge, tvFailReason;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMoviePoster = itemView.findViewById(R.id.ivMoviePoster);
            tvMovieTitle = itemView.findViewById(R.id.tvMovieTitle);
            tvScreenAndSeats = itemView.findViewById(R.id.tvScreenAndSeats);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvScannedAt = itemView.findViewById(R.id.tvScannedAt);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvFailReason = itemView.findViewById(R.id.tvFailReason);
        }
    }
}
