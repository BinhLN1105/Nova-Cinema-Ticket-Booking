package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.PaymentMethod;
import com.cinema.ticket_booking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Mỗi booking chỉ có 1 giao dịch thanh toán
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    // Các field VNPay — null nếu method = CASH
    @Column(name = "vnpay_txn_ref", length = 100)
    private String vnpayTxnRef;

    // Response code từ VNPay: "00" = thành công
    @Column(name = "vnpay_response_code", length = 10)
    private String vnpayResponseCode;

    @Column(name = "vnpay_bank_code", length = 20)
    private String vnpayBankCode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
