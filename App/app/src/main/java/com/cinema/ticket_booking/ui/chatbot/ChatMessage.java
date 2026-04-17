package com.cinema.ticket_booking.ui.chatbot;

public class ChatMessage {
    private String text;
    private boolean isUser;
    private boolean isLoading; // Used for "typing..." indicator

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.isLoading = false;
    }

    public ChatMessage(boolean isLoading) {
        this.isLoading = isLoading;
        this.isUser = false;
        this.text = "";
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public boolean isLoading() {
        return isLoading;
    }
}

