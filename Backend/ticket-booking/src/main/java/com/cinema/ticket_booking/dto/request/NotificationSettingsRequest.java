package com.cinema.ticket_booking.dto.request;

import lombok.Data;

@Data
public class NotificationSettingsRequest {
    private Boolean allowMarketingNotification;
    private Boolean allowTransactionNotification;
}
