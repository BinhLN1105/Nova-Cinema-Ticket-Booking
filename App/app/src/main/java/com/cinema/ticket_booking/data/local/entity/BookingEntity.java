package com.cinema.ticket_booking.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "bookings")
public class BookingEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String bookingCode;
    private String status;
    private String movieTitle;
    private String moviePosterUrl;
    private String startTime;
    private String cinemaName;
    private String cinemaAddress;
    private String screenName;
    private String screenType;
    private double subtotal;
    private double discountAmount;
    private double totalAmount;
    private double totalOriginalAmount;
    private double promotionDiscountAmount;
    private String appliedPromotionName;
    private String warningMessage;
    private String qrCode;
    private String expiresAt;
    private String seatsJson;
    private String combosJson;
    private String createdAt;

    public BookingEntity() {
        this.id = "";
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public String getMoviePosterUrl() { return moviePosterUrl; }
    public void setMoviePosterUrl(String moviePosterUrl) { this.moviePosterUrl = moviePosterUrl; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getCinemaName() { return cinemaName; }
    public void setCinemaName(String cinemaName) { this.cinemaName = cinemaName; }
    public String getCinemaAddress() { return cinemaAddress; }
    public void setCinemaAddress(String cinemaAddress) { this.cinemaAddress = cinemaAddress; }
    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }
    public String getScreenType() { return screenType; }
    public void setScreenType(String screenType) { this.screenType = screenType; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getTotalOriginalAmount() { return totalOriginalAmount; }
    public void setTotalOriginalAmount(double totalOriginalAmount) { this.totalOriginalAmount = totalOriginalAmount; }
    public double getPromotionDiscountAmount() { return promotionDiscountAmount; }
    public void setPromotionDiscountAmount(double promotionDiscountAmount) { this.promotionDiscountAmount = promotionDiscountAmount; }
    public String getAppliedPromotionName() { return appliedPromotionName; }
    public void setAppliedPromotionName(String appliedPromotionName) { this.appliedPromotionName = appliedPromotionName; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public String getSeatsJson() { return seatsJson; }
    public void setSeatsJson(String seatsJson) { this.seatsJson = seatsJson; }
    public String getCombosJson() { return combosJson; }
    public void setCombosJson(String combosJson) { this.combosJson = combosJson; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
