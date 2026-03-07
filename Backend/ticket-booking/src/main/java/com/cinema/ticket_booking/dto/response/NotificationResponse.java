package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private String id;
    private String title;
    private String body;
    private NotificationType type;
    private String refId;
    private Boolean isRead;
    private LocalDateTime sentAt;
}
