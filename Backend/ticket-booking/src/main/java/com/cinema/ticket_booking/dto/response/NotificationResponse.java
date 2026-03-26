package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.NotificationType;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String body;
    private NotificationType type;
    private String refId;
    private Boolean isRead;
    private LocalDateTime sentAt;
}
