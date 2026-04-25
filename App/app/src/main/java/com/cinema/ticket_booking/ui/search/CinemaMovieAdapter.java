package com.cinema.ticket_booking.ui.search;

import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.ShowtimeResponse;
import java.util.List;

public class CinemaMovieAdapter extends RecyclerView.Adapter<CinemaMovieAdapter.MovieViewHolder> {

    private final List<List<ShowtimeResponse>> moviesWithShowtimes;
    private final RecyclerView.RecycledViewPool sharedPool;
    private final OnShowtimeClickListener listener;

    public interface OnShowtimeClickListener {
        void onShowtimeClick(ShowtimeResponse showtime);
    }

    public CinemaMovieAdapter(List<List<ShowtimeResponse>> data, RecyclerView.RecycledViewPool pool, OnShowtimeClickListener listener) {
        this.moviesWithShowtimes = data;
        this.sharedPool = pool;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cinema_movie, parent, false);
        return new MovieViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        List<ShowtimeResponse> showtimes = moviesWithShowtimes.get(position);
        if (showtimes.isEmpty()) return;
        
        ShowtimeResponse first = showtimes.get(0);
        holder.tvTitle.setText(first.getMovieTitle());
        holder.tvInfo.setText(first.getScreenType() + " • Cinema Style");
        
        // Hiển thị thể loại phim
        if (first.getMovieGenres() != null && !first.getMovieGenres().isEmpty()) {
            holder.tvGenres.setText(String.join(", ", first.getMovieGenres()));
        } else {
            holder.tvGenres.setText("Chưa phân loại");
        }
        
        Glide.with(holder.itemView.getContext())
            .load(first.getMoviePosterUrl())
            .placeholder(R.drawable.ic_movie_placeholder)
            .into(holder.ivPoster);

        // Nested RecyclerView Setup
        HorizontalShowtimeAdapter adapter = new HorizontalShowtimeAdapter(showtimes, listener);
        holder.rvShowtimes.setAdapter(adapter);
        holder.rvShowtimes.setRecycledViewPool(sharedPool); // Crucial: Share the pool
    }

    @Override
    public int getItemCount() {
        return moviesWithShowtimes.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle, tvInfo, tvGenres;
        RecyclerView rvShowtimes;

        public MovieViewHolder(View v) {
            super(v);
            ivPoster = v.findViewById(R.id.ivMoviePoster);
            tvTitle = v.findViewById(R.id.tvMovieTitle);
            tvInfo = v.findViewById(R.id.tvMovieInfo);
            tvGenres = v.findViewById(R.id.tvMovieGenres);
            rvShowtimes = v.findViewById(R.id.rvHorizontalShowtimes);
            
            LinearLayoutManager lm = new LinearLayoutManager(v.getContext(), LinearLayoutManager.HORIZONTAL, false);
            lm.setInitialPrefetchItemCount(4); // Optimization
            rvShowtimes.setLayoutManager(lm);
        }
    }
}

