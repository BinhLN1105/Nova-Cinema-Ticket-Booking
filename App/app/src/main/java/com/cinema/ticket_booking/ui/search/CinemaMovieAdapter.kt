package com.cinema.ticket_booking.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.ShowtimeResponse

class CinemaMovieAdapter(
    private val moviesWithShowtimes: List<List<ShowtimeResponse>>,
    private val sharedPool: RecyclerView.RecycledViewPool,
    private val listener: (ShowtimeResponse) -> Unit
) : RecyclerView.Adapter<CinemaMovieAdapter.MovieViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cinema_movie, parent, false)
        return MovieViewHolder(v)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val showtimes = moviesWithShowtimes[position]
        if (showtimes.isEmpty()) return

        val first = showtimes[0]
        holder.tvTitle.text = first.movieTitle
        holder.tvInfo.text = "${first.screenType} • Cinema Style"

        // Hiển thị thể loại phim
        if (!first.movieGenres.isNullOrEmpty()) {
            holder.tvGenres.text = first.movieGenres.joinToString(", ")
        } else {
            holder.tvGenres.text = "Chưa phân loại"
        }

        Glide.with(holder.itemView.context)
            .load(first.moviePosterUrl)
            .placeholder(R.drawable.ic_movie_placeholder)
            .into(holder.ivPoster)

        // Nested RecyclerView Setup
        val adapter = HorizontalShowtimeAdapter(showtimes) { showtime ->
            listener(showtime)
        }
        holder.rvShowtimes.adapter = adapter
        holder.rvShowtimes.setRecycledViewPool(sharedPool) // Crucial: Share the pool
    }

    override fun getItemCount(): Int = moviesWithShowtimes.size

    class MovieViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivPoster: ImageView = v.findViewById(R.id.ivMoviePoster)
        val tvTitle: TextView = v.findViewById(R.id.tvMovieTitle)
        val tvInfo: TextView = v.findViewById(R.id.tvMovieInfo)
        val tvGenres: TextView = v.findViewById(R.id.tvMovieGenres)
        val rvShowtimes: RecyclerView = v.findViewById(R.id.rvHorizontalShowtimes)

        init {
            val lm = LinearLayoutManager(v.context, LinearLayoutManager.HORIZONTAL, false)
            lm.initialPrefetchItemCount = 4 // Optimization
            rvShowtimes.layoutManager = lm
        }
    }
}
