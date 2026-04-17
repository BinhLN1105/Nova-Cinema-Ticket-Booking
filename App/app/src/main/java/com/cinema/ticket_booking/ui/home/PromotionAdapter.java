package com.cinema.ticket_booking.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.PromotionResponse;
import java.util.List;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder> {

    private final List<PromotionResponse> promotions;
    private final OnPromotionClickListener listener;

    public interface OnPromotionClickListener {
        void onPromotionClick(PromotionResponse promotion);
    }

    public PromotionAdapter(List<PromotionResponse> promotions, OnPromotionClickListener listener) {
        this.promotions = promotions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion_banner, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        PromotionResponse promotion = promotions.get(position);
        Glide.with(holder.itemView.getContext())
                .load(promotion.getImageUrl())
                .centerCrop()
                .placeholder(R.drawable.placeholder_hero)
                .into(holder.ivBanner);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPromotionClick(promotion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return promotions.size();
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBanner;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            // Assuming we reuse the default shape or create a simple ImageView layout.
            // Since we don't have item_promotion_banner.xml, we'll create it or use programmatic sizing.
            // For simplicity, let's assume we'll create item_promotion_banner.xml afterwards.
            ivBanner = itemView.findViewById(R.id.ivBannerImage);
        }
    }
}

