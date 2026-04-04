package com.cinema.ticket_booking.dto.request;

import com.cinema.ticket_booking.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationCampaignRequest {
    private String title;
    private String body;
    private NotificationType type;
    private UUID targetId;
    private String segment; // "ALL", "VIP"
    private LocalDateTime scheduledAt; // null for "Send Now"
}
