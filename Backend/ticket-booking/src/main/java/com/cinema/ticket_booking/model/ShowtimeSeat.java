package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "showtime_seats",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_showtime_seat",
        columnNames = {"showtime_id", "seat_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeSeat {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    // User đang tạm giữ ghế — null nếu AVAILABLE hoặc BOOKED
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    private User lockedBy;

    // Thời hạn giữ ghế, @Scheduled sẽ reset sau khi hết hạn
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // Giá ghế cho suất chiếu này (có thể khác base_price theo loại ghế)
    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    // Optimistic locking để tránh race condition khi nhiều user chọn cùng 1 ghế
    @Version
    @Column(name = "version")
    private Long version;
}
