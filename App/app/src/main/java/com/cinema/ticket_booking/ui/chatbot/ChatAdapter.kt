package com.cinema.ticket_booking.ui.chatbot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.cinema.ticket_booking.R

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    private var messages: List<ChatMessage> = ArrayList()

    fun submitList(newList: List<ChatMessage>) {
        this.messages = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false))
        } else {
            BotViewHolder(inflater.inflate(R.layout.item_chat_bot, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is UserViewHolder) {
            holder.bind(msg)
        } else if (holder is BotViewHolder) {
            holder.bind(msg)
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.text
        }
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val layoutLoading: View = itemView.findViewById(R.id.layoutLoading)
        private val loadingIndicator: LottieAnimationView = itemView.findViewById(R.id.loadingIndicator)

        fun bind(msg: ChatMessage) {
            if (msg.isLoading) {
                tvMessage.visibility = View.GONE
                layoutLoading.visibility = View.VISIBLE
            } else {
                tvMessage.visibility = View.VISIBLE
                layoutLoading.visibility = View.GONE
                tvMessage.text = msg.text
            }
        }
    }
}
