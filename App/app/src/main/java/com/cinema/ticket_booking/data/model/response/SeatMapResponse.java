package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SeatMapResponse {
    @SerializedName("showtimeId")
    private String showtimeId;
    @SerializedName("totalRows")
    private int totalRows;
    @SerializedName("totalCols")
    private int totalCols;

    @SerializedName("maxGridRow")
    private int maxGridRow;

    @SerializedName("maxGridCol")
    private int maxGridCol;

    @SerializedName("seatHoldMins")
    private int seatHoldMins;

    @SerializedName("seats")
    private List<SeatItem> seats;

    public String getShowtimeId() {
        return showtimeId;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getTotalCols() {
        return totalCols;
    }

    public int getMaxGridRow() {
        return maxGridRow;
    }

    public int getMaxGridCol() {
        return maxGridCol;
    }

    public int getSeatHoldMins() {
        return seatHoldMins;
    }

    public List<SeatItem> getSeats() {
        return seats;
    }

    public static class SeatItem {
        @SerializedName("showtimeSeatId")
        private String showtimeSeatId;
        @SerializedName("seatId")
        private String seatId;
        @SerializedName("rowLabel")
        private String rowLabel;
        @SerializedName("colNumber")
        private int colNumber;
        @SerializedName("gridRow")
        private int gridRow;
        @SerializedName("gridCol")
        private int gridCol;
        @SerializedName("seatLabel")
        private String seatLabel;
        @SerializedName("seatType")
        private String seatType;
        @SerializedName("status")
        private String status;
        @SerializedName("price")
        private double price;

        public String getShowtimeSeatId() {
            return showtimeSeatId;
        }

        public String getRowLabel() {
            return rowLabel;
        }

        public int getColNumber() {
            return colNumber;
        }

        public int getGridRow() {
            return gridRow;
        }

        public int getGridCol() {
            return gridCol;
        }

        public String getSeatLabel() {
            return seatLabel;
        }

        public String getSeatType() {
            return seatType;
        }

        public String getStatus() {
            return status;
        }

        public double getPrice() {
            return price;
        }
    }
}
