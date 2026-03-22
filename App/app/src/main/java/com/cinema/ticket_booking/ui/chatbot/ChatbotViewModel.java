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
            List<ChatMessage> updateList = new ArrayList<>(_messages.getValue());
            // Remove loading indicator
            if (!updateList.isEmpty() && updateList.get(updateList.size() - 1).isLoading()) {
                updateList.remove(updateList.size() - 1);
            }

            switch (resource.status) {
                case SUCCESS:
                    updateList.add(new ChatMessage(resource.data, false));
                    break;
                case ERROR:
                    updateList.add(
                            new ChatMessage("Xin lỗi, tôi đang gặp sự cố kết nối (" + resource.message + ")", false));
                    break;
                case LOADING:
                    // do nothing, loading indicator is already added
                    break;
            }
            if (resource.status != com.cinema.ticket_booking.util.Resource.Status.LOADING) {
                _messages.setValue(updateList);
            }
        });
    }
}
