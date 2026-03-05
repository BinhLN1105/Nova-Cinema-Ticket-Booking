package com.cinema.ticket_booking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Đơn đặt vé chứa ticket này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Chi tiết ghế tương ứng (1-1)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_item_id", nullable = false, unique = true)
    private BookingItem bookingItem;

    // QR Code đã chuyển sang bảng Booking (1 QR cho cả đơn).
    // Ticket chỉ track trạng thái check-in của từng ghế riêng lẻ.

    // true khi nhân viên đã quét QR và xác nhận ghế này vào rạp
    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    // Thời điểm check-in thực tế tại rạp
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private LocalDateTime issuedAt;
}
