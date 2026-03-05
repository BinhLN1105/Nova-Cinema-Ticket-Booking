package com.cinema.ticket_booking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItem {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Ghế trong suất chiếu đã được chọn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_seat_id", nullable = false)
    private ShowtimeSeat showtimeSeat;

    // Giá ghế tại thời điểm đặt (snapshot — tránh bị ảnh hưởng nếu giá thay đổi sau)
    @Column(name = "seat_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal seatPrice;

    // Quan hệ 1-1 với Ticket
    @OneToOne(mappedBy = "bookingItem", cascade = CascadeType.ALL)
    private Ticket ticket;
}
