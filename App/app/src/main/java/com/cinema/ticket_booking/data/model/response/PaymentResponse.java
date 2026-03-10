package com.cinema.ticket_booking.data.model.response;
import com.google.gson.annotations.SerializedName;
public class PaymentResponse {
    @SerializedName("id")          private String id;
    @SerializedName("bookingId")   private String bookingId;
    @SerializedName("bookingCode") private String bookingCode;
    @SerializedName("amount")      private double amount;
    @SerializedName("status")      private String status;
    @SerializedName("paymentUrl")  private String paymentUrl;
    @SerializedName("paidAt")      private String paidAt;
    public String getId()          { return id; }
    public String getBookingId()   { return bookingId; }
    public double getAmount()      { return amount; }
    public String getStatus()      { return status; }
    public String getPaymentUrl()  { return paymentUrl; }
    public String getPaidAt()      { return paidAt; }
}
