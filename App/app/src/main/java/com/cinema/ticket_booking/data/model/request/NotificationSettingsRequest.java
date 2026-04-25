package com.cinema.ticket_booking.data.model.request;

import com.google.gson.annotations.SerializedName;

public class NotificationSettingsRequest {
    @SerializedName("allowMarketingNotification")
    private Boolean allowMarketingNotification;

    @SerializedName("allowTransactionNotification")
    private Boolean allowTransactionNotification;

    public NotificationSettingsRequest(Boolean allowMarketingNotification, Boolean allowTransactionNotification) {
        this.allowMarketingNotification = allowMarketingNotification;
        this.allowTransactionNotification = allowTransactionNotification;
    }

    public Boolean getAllowMarketingNotification() {
        return allowMarketingNotification;
    }

    public Boolean getAllowTransactionNotification() {
        return allowTransactionNotification;
    }
}
