package com.cinema.ticket_booking.ui.chatbot;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.repository.ChatbotRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ChatbotViewModel extends ViewModel {

    private final ChatbotRepository repo;
    private final MutableLiveData<List<ChatMessage>> _messages = new MutableLiveData<>(new ArrayList<>());

    @Inject
    public ChatbotViewModel(ChatbotRepository repo) {
        this.repo = repo;
        // Gửi tin nhắn chào mừng
        List<ChatMessage> initial = new ArrayList<>();
        initial.add(new ChatMessage("Xin chào! Giúp gì được bạn?", false));
        _messages.setValue(initial);
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return _messages;
    }

    public void sendMessage(String msg) {
        List<ChatMessage> currentList = new ArrayList<>(
                _messages.getValue() == null ? new ArrayList<>() : _messages.getValue());

        // Add User Message
        currentList.add(new ChatMessage(msg, true));

        // Add loading indicator
        currentList.add(new ChatMessage(true));
        _messages.setValue(currentList);

        repo.sendMessage(msg).observeForever(resource -> {
            List<ChatMessage> updateList = new ArrayList<>(
                _messages.getValue() == null ? new ArrayList<>() : _messages.getValue());
            
            // Nova Error Handling: ensure loading indicator is always removed before adding response
            updateList.removeIf(ChatMessage::isLoading);

            switch (resource.status) {
                case SUCCESS:
                    if (resource.data != null && !resource.data.isEmpty()) {
                        updateList.add(new ChatMessage(resource.data, false));
                    } else {
                        updateList.add(new ChatMessage("Tôi không nhận được phản hồi từ hệ thống.", false));
                    }
                    break;
                case ERROR:
                    // Use specific error message from Repository if available
                    String errorMsg = resource.message != null ? resource.message : "Không thể kết nối với CineAI. Vui lòng kiểm tra mạng!";
                    updateList.add(new ChatMessage("⚠️ " + errorMsg.replace("⚠️ ", ""), false));
                    break;
                case LOADING:
                    return; // Yield to next state
            }
            _messages.setValue(updateList);
        });
    }
}

