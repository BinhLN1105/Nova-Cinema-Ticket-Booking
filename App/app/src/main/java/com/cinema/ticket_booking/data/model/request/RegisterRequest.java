package com.cinema.ticket_booking.data.model.request;

public class RegisterRequest {
    private String email, password, fullName, phone;

    public RegisterRequest(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phone = null;
    }
}
