package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.CampaignStatus;
import com.cinema.ticket_booking.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationCampaignResponse {
    private String id;
    private String title;
    private String body;
    private NotificationType type;
    private String targetId;
    private String targetTopic;
    private LocalDateTime scheduledAt;
    private CampaignStatus status;
    private LocalDateTime createdAt;
    private String createdByFullName;
}
