package com.cinema.ticket_booking.data.model.request;

public class PaymentRequest {
    private String bookingId;
    private String returnUrl;

    public PaymentRequest(String bookingId, String returnUrl) {
        this.bookingId = bookingId;
        this.returnUrl = returnUrl;
    }
}
