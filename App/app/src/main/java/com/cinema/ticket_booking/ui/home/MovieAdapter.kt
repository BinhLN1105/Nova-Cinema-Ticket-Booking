package com.cinema.ticket_booking.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.MovieSummary
import com.cinema.ticket_booking.databinding.ItemMovieGridBinding

class MovieAdapter(
    private val movies: List<MovieSummary>,
    private val listener: (String) -> Unit
) : RecyclerView.Adapter<MovieAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemMovieGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    inner class ViewHolder(private val b: ItemMovieGridBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(movie: MovieSummary) {
            b.tvTitle.text = movie.title
            b.tvRating.text = String.format("★ %.1f", movie.avgRating)
            b.tvDuration.text = "${movie.duration} phút"
            Glide.with(b.ivPoster.context)
                .load(movie.posterUrl)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .centerCrop()
                .into(b.ivPoster)
            b.root.setOnClickListener { movie.id?.let { id -> listener(id) } }
        }
    }
}
