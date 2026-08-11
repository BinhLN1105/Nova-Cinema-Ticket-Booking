package com.cinema.ticket_booking.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.CinemaResponse

class CinemaAdapter(
    private var cinemas: List<CinemaResponse>,
    private val listener: OnCinemaClickListener
) : RecyclerView.Adapter<CinemaAdapter.CinemaViewHolder>() {

    interface OnCinemaClickListener {
        fun onCinemaClick(cinema: CinemaResponse)
    }

    fun updateData(newCinemas: List<CinemaResponse>) {
        this.cinemas = newCinemas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CinemaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cinema, parent, false)
        return CinemaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CinemaViewHolder, position: Int) {
        val cinema = cinemas[position]
        holder.tvName.text = cinema.name
        holder.tvAddress.text = cinema.address

        Glide.with(holder.itemView.context)
            .load(cinema.imageUrl)
            .placeholder(R.drawable.placeholder_hero)
            .error(R.drawable.placeholder_hero)
            .centerCrop()
            .into(holder.ivCinema)

        holder.itemView.setOnClickListener { listener.onCinemaClick(cinema) }
    }

    override fun getItemCount(): Int = cinemas.size

    class CinemaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCinema: ImageView = itemView.findViewById(R.id.ivCinema)
        val tvName: TextView = itemView.findViewById(R.id.tvCinemaName)
        val tvAddress: TextView = itemView.findViewById(R.id.tvCinemaAddress)
    }
}
