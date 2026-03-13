package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    // Mã booking hiển thị cho khách: VD "BK20241201001"
    @Column(name = "booking_code", length = 20, unique = true, nullable = false)
    private String bookingCode;

    // Voucher đã áp dụng — null nếu không dùng mã giảm giá
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    // Số tiền được giảm từ voucher
    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // 1 QR Code duy nhất cho toàn bộ đơn hàng (thay vì 1 QR/ghế)
    // Nội dung: mã hoá JSON {bookingId, bookingCode, checksum} bằng HMAC-SHA256
    @Column(name = "qr_code", columnDefinition = "TEXT", unique = true)
    private String qrCode;

    // Tổng tiền SAU khi trừ voucher: sum(ghế) + sum(combo) - discountAmount
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "pending_exp")
    @Builder.Default
    private Long pendingExp = 0L;

    @Column(name = "exp_added")
    @Builder.Default
    private Boolean expAdded = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Thời hạn thanh toán khi status = PENDING
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Dùng để xác nhận hủy vé qua email
    @Column(name = "cancellation_token", length = 100)
    private String cancellationToken;

    @Column(name = "cancellation_token_expiry")
    private LocalDateTime cancellationTokenExpiry;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingItem> bookingItems = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingCombo> bookingCombos = new ArrayList<>();
}
