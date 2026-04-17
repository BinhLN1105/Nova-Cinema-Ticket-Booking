package com.cinema.ticket_booking.ui.home;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.MovieSummary;
import com.cinema.ticket_booking.databinding.ItemMovieGridBinding;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder> {

    public interface OnMovieClickListener {
        void onClick(String movieId);
    }

    private final List<MovieSummary> movies;
    private final OnMovieClickListener listener;

    public MovieAdapter(List<MovieSummary> movies, OnMovieClickListener listener) {
        this.movies = movies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMovieGridBinding b = ItemMovieGridBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(movies.get(position));
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMovieGridBinding b;

        ViewHolder(ItemMovieGridBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(MovieSummary movie) {
            b.tvTitle.setText(movie.getTitle());
            b.tvRating.setText(String.format("★ %.1f", movie.getAvgRating()));
            b.tvDuration.setText(movie.getDuration() + " phút");
            Glide.with(b.ivPoster.getContext())
                    .load(movie.getPosterUrl())
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.ic_movie_placeholder)
                    .centerCrop()
                    .into(b.ivPoster);
            b.getRoot().setOnClickListener(v -> listener.onClick(movie.getId()));
        }
    }
}

