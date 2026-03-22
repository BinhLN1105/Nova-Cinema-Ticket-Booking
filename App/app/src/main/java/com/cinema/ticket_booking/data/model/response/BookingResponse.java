package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BookingResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("bookingCode")
    private String bookingCode;
    @SerializedName("status")
    private String status;
    @SerializedName("movieTitle")
    private String movieTitle;
    @SerializedName("moviePosterUrl")
    private String moviePosterUrl;
    @SerializedName("startTime")
    private String startTime;
    @SerializedName("cinemaName")
    private String cinemaName;
    @SerializedName("cinemaAddress")
    private String cinemaAddress;
    @SerializedName("screenName")
    private String screenName;
    @SerializedName("subtotal")
    private double subtotal;
    @SerializedName("discountAmount")
    private double discountAmount;
    @SerializedName("totalAmount")
    private double totalAmount;
    @SerializedName("qrCode")
    private String qrCode;
    @SerializedName("expiresAt")
    private String expiresAt;
    @SerializedName("seats")
    private List<SeatItem> seats;
    @SerializedName("combos")
    private List<ComboItem> combos;

    public String getId() {
        return id;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getStatus() {
        return status;
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

    public String getScreenName() {
        return screenName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public List<SeatItem> getSeats() {
        return seats;
    }

    public List<ComboItem> getCombos() {
        return combos;
    }

    public static class SeatItem {
        @SerializedName("rowLabel")
        private String rowLabel;
        @SerializedName("colNumber")
        private int colNumber;
        @SerializedName("seatType")
        private String seatType;
        @SerializedName("price")
        private double price;

        public String getRowLabel() {
            return rowLabel;
        }

        public int getColNumber() {
            return colNumber;
        }

        public String getSeatType() {
            return seatType;
        }

        public double getPrice() {
            return price;
        }
    }

    public static class ComboItem {
        @SerializedName("comboName")
        private String comboName;
        @SerializedName("quantity")
        private int quantity;
        @SerializedName("unitPrice")
        private double unitPrice;
        @SerializedName("subtotal")
        private double subtotal;

        public String getComboName() {
            return comboName;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public double getSubtotal() {
            return subtotal;
        }
    }
}
