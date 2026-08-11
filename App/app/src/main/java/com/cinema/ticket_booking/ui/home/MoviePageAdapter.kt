package com.cinema.ticket_booking.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.data.model.response.MovieSummary
import com.cinema.ticket_booking.databinding.ItemMoviePageBinding

class MoviePageAdapter(
    private val pages: List<List<MovieSummary>>,
    private val listener: (String) -> Unit
) : RecyclerView.Adapter<MoviePageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemMoviePageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        // Force page width to match screen exactly
        val lp = b.root.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        b.root.layoutParams = lp

        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    inner class ViewHolder(private val b: ItemMoviePageBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            b.rvPage.layoutManager = GridLayoutManager(b.root.context, 3)
            b.rvPage.isNestedScrollingEnabled = false
        }

        fun bind(movies: List<MovieSummary>) {
            b.rvPage.adapter = MovieAdapter(movies, listener)
        }
    }
}
