package com.cinema.ticket_booking.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.MovieSummary;
import java.util.List;

/**
 * Adapter for the Hero ViewPager2 to display featured movie posters.
 */
public class HeroAdapter extends RecyclerView.Adapter<HeroAdapter.HeroViewHolder> {

    private final List<MovieSummary> movies;

    public HeroAdapter(List<MovieSummary> movies) {
        this.movies = movies;
    }

    @NonNull
    @Override
    public HeroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hero_poster, parent, false);
        return new HeroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeroViewHolder holder, int position) {
        MovieSummary movie = movies.get(position);
        Glide.with(holder.itemView.getContext())
                .load(movie.getPosterUrl())
                // Use the same placeholder as in HomeFragment
                .placeholder(R.drawable.placeholder_hero)
                .centerCrop()
                .into(holder.ivPoster);
    }

    @Override
    public int getItemCount() {
        return movies != null ? movies.size() : 0;
    }

    static class HeroViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;

        public HeroViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
        }
    }
}
