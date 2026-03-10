package com.cinema.ticket_booking.data.model.request;
public class SocialLoginRequest {
    private String idToken;
    private String provider;
    public SocialLoginRequest(String idToken, String provider) {
        this.idToken = idToken; this.provider = provider;
    }
}
