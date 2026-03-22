package com.cinema.ticket_booking.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "bookings")
public class BookingEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String bookingCode, movieTitle, moviePosterUrl;
    private String startTime, cinemaName, status, createdAt;
    private double totalAmount;

    public BookingEntity(@NonNull String id, String bookingCode, String movieTitle,
            String moviePosterUrl, String startTime, String cinemaName,
            String status, double totalAmount, String createdAt) {
        this.id = id;
        this.bookingCode = bookingCode;
        this.movieTitle = movieTitle;
        this.moviePosterUrl = moviePosterUrl;
        this.startTime = startTime;
        this.cinemaName = cinemaName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public String getMoviePosterUrl() {
        return moviePosterUrl;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public String getStatus() {
        return status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
