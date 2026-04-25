package com.cinema.ticket_booking.ui.promotion;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.PromotionResponse;
import com.cinema.ticket_booking.databinding.ItemPromotionCardBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromotionListAdapter extends RecyclerView.Adapter<PromotionListAdapter.ViewHolder> {

    private final List<PromotionResponse> promotions;
    private final OnPromotionClickListener listener;

    public interface OnPromotionClickListener {
        void onPromotionClick(PromotionResponse promotion);
    }

    public PromotionListAdapter(List<PromotionResponse> promotions, OnPromotionClickListener listener) {
        this.promotions = promotions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPromotionCardBinding binding = ItemPromotionCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PromotionResponse promotion = promotions.get(position);
        holder.bind(promotion);
    }

    @Override
    public int getItemCount() {
        return promotions != null ? promotions.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPromotionCardBinding binding;

        public ViewHolder(ItemPromotionCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(PromotionResponse promotion) {
            binding.tvPromoTitle.setText(promotion.getTitle());
            binding.tvPromoDescription.setText(promotion.getDescription());

            // Format date safely
            String formattedDate = "N/A";
            if (promotion.getEndDate() != null) {
                try {
                    SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat sdfOut = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    Date date = sdfIn.parse(promotion.getEndDate());
                    if (date != null) {
                        formattedDate = sdfOut.format(date);
                    }
                } catch (ParseException e) {
                    // Fallback to substring if parsing fails
                    if (promotion.getEndDate().length() >= 10) {
                        formattedDate = promotion.getEndDate().substring(0, 10);
                    }
                }
            }
            binding.tvPromoDate.setText("HSD: " + formattedDate);

            Glide.with(itemView.getContext())
                    .load(promotion.getImageUrl())
                    .placeholder(R.drawable.bg_placeholder_banner)
                    .error(R.drawable.bg_placeholder_banner)
                    .centerCrop()
                    .into(binding.ivPromoBanner);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPromotionClick(promotion);
                }
            });
            
            binding.btnPromoDetail.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPromotionClick(promotion);
                }
            });
        }
    }
}
