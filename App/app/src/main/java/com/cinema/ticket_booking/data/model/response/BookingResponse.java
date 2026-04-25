package com.cinema.ticket_booking.data.model.response;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class BookingResponse implements Parcelable {
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
    @SerializedName("screenType")
    private String screenType;
    @SerializedName("subtotal")
    private Double subtotal;
    @SerializedName("discountAmount")
    private Double discountAmount;
    @SerializedName("totalAmount")
    private Double totalAmount;
    @SerializedName("totalOriginalAmount")
    private Double totalOriginalAmount;
    @SerializedName("promotionDiscountAmount")
    private Double promotionDiscountAmount;
    @SerializedName("appliedPromotionName")
    private String appliedPromotionName;
    @SerializedName("warningMessage")
    private String warningMessage;
    @SerializedName("qrCode")
    private String qrCode;
    @SerializedName("expiresAt")
    private String expiresAt;
    @SerializedName("seats")
    private List<SeatItem> seats;
    @SerializedName("combos")
    private List<ComboItem> combos;
    @SerializedName("pointsUsed")
    private Long pointsUsed;
    @SerializedName("pointDiscount")
    private Double pointDiscount;
    @SerializedName("remainingAmount")
    private Double remainingAmount;

    protected BookingResponse(Parcel in) {
        id = in.readString();
        bookingCode = in.readString();
        status = in.readString();
        movieTitle = in.readString();
        moviePosterUrl = in.readString();
        startTime = in.readString();
        cinemaName = in.readString();
        cinemaAddress = in.readString();
        screenName = in.readString();
        screenType = in.readString();
        
        if (in.readByte() == 0) subtotal = null;
        else subtotal = in.readDouble();
        
        if (in.readByte() == 0) discountAmount = null;
        else discountAmount = in.readDouble();
        
        if (in.readByte() == 0) totalAmount = null;
        else totalAmount = in.readDouble();
        
        if (in.readByte() == 0) totalOriginalAmount = null;
        else totalOriginalAmount = in.readDouble();
        
        if (in.readByte() == 0) promotionDiscountAmount = null;
        else promotionDiscountAmount = in.readDouble();
        
        appliedPromotionName = in.readString();
        warningMessage = in.readString();
        qrCode = in.readString();
        expiresAt = in.readString();
        seats = in.createTypedArrayList(SeatItem.CREATOR);
        combos = in.createTypedArrayList(ComboItem.CREATOR);

        // Read Hybrid Fields with Null Safety
        if (in.readByte() == 0) pointsUsed = null;
        else pointsUsed = in.readLong();
        
        if (in.readByte() == 0) pointDiscount = null;
        else pointDiscount = in.readDouble();
        
        if (in.readByte() == 0) remainingAmount = null;
        else remainingAmount = in.readDouble();
    }

    public static final Creator<BookingResponse> CREATOR = new Creator<BookingResponse>() {
        @Override
        public BookingResponse createFromParcel(Parcel in) {
            return new BookingResponse(in);
        }

        @Override
        public BookingResponse[] newArray(int size) {
            return new BookingResponse[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(bookingCode);
        dest.writeString(status);
        dest.writeString(movieTitle);
        dest.writeString(moviePosterUrl);
        dest.writeString(startTime);
        dest.writeString(cinemaName);
        dest.writeString(cinemaAddress);
        dest.writeString(screenName);
        dest.writeString(screenType);
        
        if (subtotal == null) dest.writeByte((byte) 0);
        else { dest.writeByte((byte) 1); dest.writeDouble(subtotal); }
        
        if (discountAmount == null) dest.writeByte((byte) 0);
        else { dest.writeByte((byte) 1); dest.writeDouble(discountAmount); }
        
        if (totalAmount == null) dest.writeByte((byte) 0);
        else { dest.writeByte((byte) 1); dest.writeDouble(totalAmount); }
        
        if (totalOriginalAmount == null) dest.writeByte((byte) 0);
        else { dest.writeByte((byte) 1); dest.writeDouble(totalOriginalAmount); }
        
        if (promotionDiscountAmount == null) dest.writeByte((byte) 0);
        else { dest.writeByte((byte) 1); dest.writeDouble(promotionDiscountAmount); }
        
        dest.writeString(appliedPromotionName);
        dest.writeString(warningMessage);
        dest.writeString(qrCode);
        dest.writeString(expiresAt);
        dest.writeTypedList(seats);
        dest.writeTypedList(combos);

        // Write Hybrid Fields with Null Safety
        if (pointsUsed == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(pointsUsed);
        }

        if (pointDiscount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(pointDiscount);
        }

        if (remainingAmount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(remainingAmount);
        }
    }

    // Default constructor for Retrofit/GSON
    public BookingResponse() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public Double getTotalOriginalAmount() { return totalOriginalAmount; }
    public void setTotalOriginalAmount(Double totalOriginalAmount) { this.totalOriginalAmount = totalOriginalAmount; }
    public Double getPromotionDiscountAmount() { return promotionDiscountAmount; }
    public void setPromotionDiscountAmount(Double promotionDiscountAmount) { this.promotionDiscountAmount = promotionDiscountAmount; }
    public String getAppliedPromotionName() { return appliedPromotionName; }
    public void setAppliedPromotionName(String appliedPromotionName) { this.appliedPromotionName = appliedPromotionName; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public List<SeatItem> getSeats() { return seats; }
    public void setSeats(List<SeatItem> seats) { this.seats = seats; }
    public List<ComboItem> getCombos() { return combos; }
    public void setCombos(List<ComboItem> combos) { this.combos = combos; }

    public Long getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(Long pointsUsed) { this.pointsUsed = pointsUsed; }
    public Double getPointDiscount() { return pointDiscount; }
    public void setPointDiscount(Double pointDiscount) { this.pointDiscount = pointDiscount; }
    public Double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Double remainingAmount) { this.remainingAmount = remainingAmount; }

    public static class SeatItem implements Parcelable {
        @SerializedName("rowLabel")
        private String rowLabel;
        @SerializedName("colNumber")
        private int colNumber;
        @SerializedName("seatType")
        private String seatType;
        @SerializedName("price")
        private double price;
        @SerializedName("showtimeSeatId")
        private String showtimeSeatId;

        public SeatItem() {}

        protected SeatItem(Parcel in) {
            rowLabel = in.readString();
            colNumber = in.readInt();
            seatType = in.readString();
            price = in.readDouble();
            showtimeSeatId = in.readString();
        }

        public static final Creator<SeatItem> CREATOR = new Creator<SeatItem>() {
            @Override
            public SeatItem createFromParcel(Parcel in) {
                return new SeatItem(in);
            }

            @Override
            public SeatItem[] newArray(int size) {
                return new SeatItem[size];
            }
        };

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(rowLabel);
            dest.writeInt(colNumber);
            dest.writeString(seatType);
            dest.writeDouble(price);
            dest.writeString(showtimeSeatId);
        }

        public String getRowLabel() { return rowLabel; }
        public int getColNumber() { return colNumber; }
        public String getSeatType() { return seatType; }
        public double getPrice() { return price; }
        public String getShowtimeSeatId() { return showtimeSeatId; }
        public void setShowtimeSeatId(String id) { this.showtimeSeatId = id; }
    }

    public static class ComboItem implements Parcelable {
        @SerializedName("comboName")
        private String comboName;
        @SerializedName("quantity")
        private int quantity;
        @SerializedName("unitPrice")
        private double unitPrice;
        @SerializedName("subtotal")
        private double subtotal;
        @SerializedName("comboId")
        private String comboId;

        public ComboItem() {}

        protected ComboItem(Parcel in) {
            comboName = in.readString();
            quantity = in.readInt();
            unitPrice = in.readDouble();
            subtotal = in.readDouble();
            comboId = in.readString();
        }

        public static final Creator<ComboItem> CREATOR = new Creator<ComboItem>() {
            @Override
            public ComboItem createFromParcel(Parcel in) {
                return new ComboItem(in);
            }

            @Override
            public ComboItem[] newArray(int size) {
                return new ComboItem[size];
            }
        };

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(comboName);
            dest.writeInt(quantity);
            dest.writeDouble(unitPrice);
            dest.writeDouble(subtotal);
            dest.writeString(comboId);
        }

        public String getComboName() { return comboName; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getSubtotal() { return subtotal; }
        public String getComboId() { return comboId; }
        public void setComboId(String id) { this.comboId = id; }
    }
}
