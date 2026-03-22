package com.cinema.ticket_booking.data.model.request;

public class UpdateProfileRequest {
    private String fullName, phone, avatarUrl;

    public UpdateProfileRequest(String fullName, String phone, String avatarUrl) {
        this.fullName = fullName;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
    }
}
