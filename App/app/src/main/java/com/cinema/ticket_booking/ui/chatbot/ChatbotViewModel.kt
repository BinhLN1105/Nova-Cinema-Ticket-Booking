package com.cinema.ticket_booking.ui.chatbot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.repository.ChatbotRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.ArrayList
import javax.inject.Inject

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val repo: ChatbotRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(ArrayList())
    val messages: LiveData<List<ChatMessage>> = _messages

    init {
        // Gửi tin nhắn chào mừng
        val initial = ArrayList<ChatMessage>()
        initial.add(ChatMessage("Xin chào! Giúp gì được bạn?", false))
        _messages.value = initial
    }

    fun sendMessage(msg: String) {
        val currentList = ArrayList<ChatMessage>(
            _messages.value ?: ArrayList()
        )

        // Add User Message
        currentList.add(ChatMessage(msg, true))

        // Add loading indicator
        currentList.add(ChatMessage(true))
        _messages.value = currentList

        repo.sendMessage(msg).observeForever { resource ->
            val updateList = ArrayList<ChatMessage>(
                _messages.value ?: ArrayList()
            )

            // Nova Error Handling: ensure loading indicator is always removed before adding response
            updateList.removeIf { it.isLoading }

            when (resource.status) {
                Resource.Status.SUCCESS -> {
                    if (!resource.data.isNullOrEmpty()) {
                        updateList.add(ChatMessage(resource.data, false))
                    } else {
                        updateList.add(ChatMessage("Tôi không nhận được phản hồi từ hệ thống.", false))
                    }
                }
                Resource.Status.ERROR -> {
                    // Use specific error message from Repository if available
                    val errorMsg = resource.message ?: "Không thể kết nối với CineAI. Vui lòng kiểm tra mạng!"
                    updateList.add(ChatMessage("⚠️ " + errorMsg.replace("⚠️ ", ""), false))
                }
                Resource.Status.LOADING -> return@observeForever
            }
            _messages.value = updateList
        }
    }
}
