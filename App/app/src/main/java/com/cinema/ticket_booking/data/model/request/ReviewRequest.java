package com.cinema.ticket_booking.data.model.request;
public class ReviewRequest {
    private String movieId, bookingId, comment;
    private int rating;
    public ReviewRequest(String movieId, String bookingId, int rating, String comment) {
        this.movieId = movieId; this.bookingId = bookingId;
        this.rating = rating; this.comment = comment;
    }
}
