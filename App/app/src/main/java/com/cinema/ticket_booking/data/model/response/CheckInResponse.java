package com.cinema.ticket_booking.data.model.response;

import java.util.List;

public class CheckInResponse {
    private String bookingCode;
    private String movieTitle;
    private String startTime;
    private String cinemaName;
    private String screenName;
    private List<SeatItem> seats;
    private Boolean allCheckedIn;
    private String checkedInAt;

    public static class SeatItem {
        private String rowLabel;
        private Integer colNumber;
        private String seatType;
        private Boolean isUsed;

        public String getRowLabel() { return rowLabel; }
        public Integer getColNumber() { return colNumber; }
        public String getSeatType() { return seatType; }
        public Boolean getIsUsed() { return isUsed; }
    }

    public String getBookingCode() { return bookingCode; }
    public String getMovieTitle() { return movieTitle; }
    public String getStartTime() { return startTime; }
    public String getCinemaName() { return cinemaName; }
    public String getScreenName() { return screenName; }
    public List<SeatItem> getSeats() { return seats; }
    public Boolean getAllCheckedIn() { return allCheckedIn; }
    public String getCheckedInAt() { return checkedInAt; }
}
