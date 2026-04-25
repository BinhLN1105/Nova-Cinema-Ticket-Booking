package com.cinema.ticket_booking.service;

public interface AiChatService {
    String sendMessage(String sessionId, String userMessage);
    void clearSession(String sessionId);
    boolean triggerSync();
}
