package com.cinema.ticket_booking.dto.response;

import com.cinema.ticket_booking.enums.PaymentMethod;
import com.cinema.ticket_booking.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // ── CinePoint hybrid payment fields ─────────────────────────────────
    /** Số CP đã trừ, null nếu không dùng CP */
    private Long pointsUsed;

    /** Số tiền được giảm từ CP (pointsUsed * 1000), null nếu không dùng CP */
    private BigDecimal pointDiscount;

    /** Số tiền còn lại cần thanh toán qua cổng (VNPay/MoMo), null nếu full CP */
    private BigDecimal remainingAmount;
}
