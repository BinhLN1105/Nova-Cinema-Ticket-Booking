package com.cinema.ticket_booking.data.model.response;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class SeatMapResponse {
    @SerializedName("showtimeId") private String showtimeId;
    @SerializedName("totalRows")  private int totalRows;
    @SerializedName("totalCols")  private int totalCols;
    @SerializedName("seats")      private List<SeatItem> seats;
    public String getShowtimeId() { return showtimeId; }
    public int getTotalRows()     { return totalRows; }
    public int getTotalCols()     { return totalCols; }
    public List<SeatItem> getSeats() { return seats; }

    public static class SeatItem {
        @SerializedName("showtimeSeatId") private String showtimeSeatId;
        @SerializedName("seatId")         private String seatId;
        @SerializedName("rowLabel")       private String rowLabel;
        @SerializedName("colNumber")      private int colNumber;
        @SerializedName("seatType")       private String seatType;
        @SerializedName("status")         private String status;
        @SerializedName("price")          private double price;
        public String getShowtimeSeatId() { return showtimeSeatId; }
        public String getRowLabel()       { return rowLabel; }
        public int getColNumber()         { return colNumber; }
        public String getSeatType()       { return seatType; }
        public String getStatus()         { return status; }
        public double getPrice()          { return price; }
    }
}
