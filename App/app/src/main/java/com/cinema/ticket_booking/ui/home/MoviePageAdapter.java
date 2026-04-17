package com.cinema.ticket_booking.ui.home;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cinema.ticket_booking.data.model.response.MovieSummary;
import com.cinema.ticket_booking.databinding.ItemMoviePageBinding;
import java.util.List;

public class MoviePageAdapter extends RecyclerView.Adapter<MoviePageAdapter.ViewHolder> {

    private final List<List<MovieSummary>> pages;
    private final MovieAdapter.OnMovieClickListener listener;

    public MoviePageAdapter(List<List<MovieSummary>> pages, MovieAdapter.OnMovieClickListener listener) {
        this.pages = pages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMoviePageBinding b = ItemMoviePageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        
        // Force page width to match screen exactly
        ViewGroup.LayoutParams lp = b.getRoot().getLayoutParams();
        if (lp == null) {
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        b.getRoot().setLayoutParams(lp);

        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(pages.get(position));
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMoviePageBinding b;

        ViewHolder(ItemMoviePageBinding b) {
            super(b.getRoot());
            this.b = b;
            b.rvPage.setLayoutManager(new GridLayoutManager(b.getRoot().getContext(), 3));
            b.rvPage.setNestedScrollingEnabled(false);
        }

        void bind(List<MovieSummary> movies) {
            b.rvPage.setAdapter(new MovieAdapter(movies, listener));
        }
    }
}

