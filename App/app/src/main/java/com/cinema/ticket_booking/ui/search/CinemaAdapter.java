package com.cinema.ticket_booking.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.CinemaResponse;
import java.util.List;

public class CinemaAdapter extends RecyclerView.Adapter<CinemaAdapter.CinemaViewHolder> {

    private List<CinemaResponse> cinemas;
    private final OnCinemaClickListener listener;

    public interface OnCinemaClickListener {
        void onCinemaClick(CinemaResponse cinema);
    }

    public CinemaAdapter(List<CinemaResponse> cinemas, OnCinemaClickListener listener) {
        this.cinemas = cinemas;
        this.listener = listener;
    }

    public void updateData(List<CinemaResponse> newCinemas) {
        this.cinemas = newCinemas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CinemaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cinema, parent, false);
        return new CinemaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CinemaViewHolder holder, int position) {
        CinemaResponse cinema = cinemas.get(position);
        holder.tvName.setText(cinema.getName());
        holder.tvAddress.setText(cinema.getAddress());

        Glide.with(holder.itemView.getContext())
                .load(cinema.getImageUrl())
                .placeholder(R.drawable.placeholder_hero)
                .error(R.drawable.placeholder_hero)
                .centerCrop()
                .into(holder.ivCinema);

        holder.itemView.setOnClickListener(v -> listener.onCinemaClick(cinema));
    }

    @Override
    public int getItemCount() {
        return cinemas != null ? cinemas.size() : 0;
    }

    static class CinemaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCinema;
        TextView tvName, tvAddress;

        public CinemaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCinema = itemView.findViewById(R.id.ivCinema);
            tvName = itemView.findViewById(R.id.tvCinemaName);
            tvAddress = itemView.findViewById(R.id.tvCinemaAddress);
        }
    }
}
