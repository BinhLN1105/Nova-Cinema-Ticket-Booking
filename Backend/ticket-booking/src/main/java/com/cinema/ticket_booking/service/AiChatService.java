package com.cinema.ticket_booking.service;

import java.util.Map;

public interface AiChatService {
    Map<String, Object> sendMessage(String sessionId, String userMessage);

    void clearSession(String sessionId);

    boolean triggerSync();
}
