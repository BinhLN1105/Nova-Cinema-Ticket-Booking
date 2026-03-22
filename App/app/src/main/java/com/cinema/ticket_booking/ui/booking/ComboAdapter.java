package com.cinema.ticket_booking.ui.booking;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.ComboResponse;
import com.cinema.ticket_booking.databinding.ItemComboBinding;
import java.util.*;

public class ComboAdapter extends RecyclerView.Adapter<ComboAdapter.VH> {
    public interface OnQtyChange {
        void onChange(String comboId);
    }

    private final List<ComboResponse> items;
    private final OnQtyChange onAdd, onRemove;
    private final Map<String, Integer> qtys;

    public ComboAdapter(List<ComboResponse> items, OnQtyChange onAdd, OnQtyChange onRemove, Map<String, Integer> qtys) {
        this.items = items;
        this.onAdd = onAdd;
        this.onRemove = onRemove;
        this.qtys = qtys;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(ItemComboBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
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
        final ItemComboBinding b;

        VH(ItemComboBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(ComboResponse c) {
            b.tvComboName.setText(c.getName());
            b.tvComboDesc.setText(c.getDescription());
            b.tvComboPrice.setText(String.format("%,.0fđ", c.getPrice()));
            Glide.with(b.ivCombo.getContext()).load(c.getImageUrl())
                    .placeholder(R.drawable.ic_movie_placeholder).into(b.ivCombo);
            int qty = qtys.getOrDefault(c.getId(), 0);
            b.tvQty.setText(String.valueOf(qty));
            b.btnMinus.setOnClickListener(v -> {
                onRemove.onChange(c.getId());
                notifyItemChanged(getAdapterPosition());
            });
            b.btnPlus.setOnClickListener(v -> {
                onAdd.onChange(c.getId());
                notifyItemChanged(getAdapterPosition());
            });
        }
    }
}
