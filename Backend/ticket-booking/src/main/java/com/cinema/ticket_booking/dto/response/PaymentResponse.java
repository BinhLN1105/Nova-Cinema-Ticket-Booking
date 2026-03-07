package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.PaymentMethod;
import com.cinema.ticket_booking.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private String id;
    private String bookingId;
    private String bookingCode;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String vnpayTxnRef;
    private String vnpayBankCode;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    // URL thanh toán VNPay — Android mở WebView để user thanh toán
    private String paymentUrl;
}
