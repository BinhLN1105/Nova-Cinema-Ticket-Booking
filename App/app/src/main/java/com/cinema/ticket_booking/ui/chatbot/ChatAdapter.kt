package com.cinema.ticket_booking.ui.chatbot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.cinema.ticket_booking.R
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private var onDraftConfirmClick: ((draftId: String, position: Int) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    private var messages: List<ChatMessage> = ArrayList()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun setOnDraftConfirmListener(listener: (draftId: String, position: Int) -> Unit) {
        this.onDraftConfirmClick = listener
    }

    fun submitList(newList: List<ChatMessage>) {
        this.messages = newList
        notifyDataSetChanged()
    }

    fun setDraftConfirmingState(position: Int, isConfirming: Boolean) {
        if (position in messages.indices) {
            messages[position].isConfirmingDraft = isConfirming
            notifyItemChanged(position)
        }
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
            holder.bind(msg, position)
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView? = itemView.findViewById(R.id.tvTimestamp)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.text
            tvTimestamp?.text = timeFormat.format(Date(msg.timestamp))
        }
    }

    inner class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView? = itemView.findViewById(R.id.tvTimestamp)
        private val layoutLoading: View = itemView.findViewById(R.id.layoutLoading)
        private val layoutDraftAction: View = itemView.findViewById(R.id.layoutDraftAction)
        private val btnConfirmDraft: MaterialButton = itemView.findViewById(R.id.btnConfirmDraft)
        private val layoutDraftLoading: View = itemView.findViewById(R.id.layoutDraftLoading)

        fun bind(msg: ChatMessage, position: Int) {
            if (msg.isLoading) {
                tvMessage.visibility = View.GONE
                layoutLoading.visibility = View.VISIBLE
                layoutDraftAction.visibility = View.GONE
                tvTimestamp?.visibility = View.GONE
            } else {
                tvMessage.visibility = View.VISIBLE
                layoutLoading.visibility = View.GONE
                tvTimestamp?.visibility = View.VISIBLE
                tvTimestamp?.text = timeFormat.format(Date(msg.timestamp))
                tvMessage.text = msg.text

                // Xử lý hiển thị nút Xác nhận vé nháp nếu có draftId
                if (!msg.draftId.isNullOrEmpty()) {
                    layoutDraftAction.visibility = View.VISIBLE
                    if (msg.isConfirmingDraft) {
                        btnConfirmDraft.visibility = View.GONE
                        layoutDraftLoading.visibility = View.VISIBLE
                    } else {
                        btnConfirmDraft.visibility = View.VISIBLE
                        layoutDraftLoading.visibility = View.GONE
                        btnConfirmDraft.setOnClickListener {
                            onDraftConfirmClick?.invoke(msg.draftId, position)
                        }
                    }
                } else {
                    layoutDraftAction.visibility = View.GONE
                }
            }
        }
    }
}
