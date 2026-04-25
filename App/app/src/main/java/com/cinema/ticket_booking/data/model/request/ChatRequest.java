package com.cinema.ticket_booking.data.model.request;

public class ChatRequest {
    private String userMessage;

    public ChatRequest(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
}
