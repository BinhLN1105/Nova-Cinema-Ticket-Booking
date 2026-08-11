package com.cinema.ticket_booking.ui.movie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.ReviewResponse

class ReviewAdapter(
    private val reviews: MutableList<ReviewResponse>
) : RecyclerView.Adapter<ReviewAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = reviews[position]
        holder.tvUserName.text = r.userFullName
        holder.tvComment.text = r.comment
        holder.tvDate.text = if (r.createdAt != null && r.createdAt.length >= 10) r.createdAt.substring(0, 10) else ""

        val stars = StringBuilder()
        for (i in 0 until r.rating) {
            stars.append("★")
        }
        for (i in r.rating until 5) {
            stars.append("☆")
        }
        holder.tvRating.text = stars.toString()

        if (!r.userAvatarUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(r.userAvatarUrl)
                .circleCrop()
                .into(holder.ivAvatar)
        }
    }

    override fun getItemCount(): Int = reviews.size

    fun addAll(newReviews: List<ReviewResponse>) {
        val start = reviews.size
        reviews.addAll(newReviews)
        notifyItemRangeInserted(start, newReviews.size)
    }

    fun clear() {
        reviews.clear()
        notifyDataSetChanged()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvComment: TextView = itemView.findViewById(R.id.tvComment)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
    }
}
