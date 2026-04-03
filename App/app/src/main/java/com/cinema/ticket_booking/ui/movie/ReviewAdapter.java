package com.cinema.ticket_booking.ui.movie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.ReviewResponse;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {

    private final List<ReviewResponse> reviews;

    public ReviewAdapter(List<ReviewResponse> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReviewResponse r = reviews.get(position);
        holder.tvUserName.setText(r.getUserFullName());
        holder.tvComment.setText(r.getComment());
        holder.tvDate.setText(r.getCreatedAt() != null ? r.getCreatedAt().substring(0, 10) : "");

        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < r.getRating(); i++)
            stars.append("★");
        for (int i = r.getRating(); i < 5; i++)
            stars.append("☆");
        holder.tvRating.setText(stars.toString());

        if (r.getUserAvatarUrl() != null && !r.getUserAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(r.getUserAvatarUrl())
                    .circleCrop()
                    .into(holder.ivAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public void addAll(List<ReviewResponse> newReviews) {
        int start = reviews.size();
        reviews.addAll(newReviews);
        notifyItemRangeInserted(start, newReviews.size());
    }

    public void clear() {
        reviews.clear();
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvUserName, tvComment, tvDate, tvRating;
        ImageView ivAvatar;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRating = itemView.findViewById(R.id.tvRating);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }
    }
}
