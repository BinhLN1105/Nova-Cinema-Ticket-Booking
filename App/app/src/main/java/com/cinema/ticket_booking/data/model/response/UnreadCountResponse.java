package com.cinema.ticket_booking.data.model.response;
import com.google.gson.annotations.SerializedName;
public class UnreadCountResponse {
    @SerializedName("unreadCount") private long unreadCount;
    public long getUnreadCount()   { return unreadCount; }
}
