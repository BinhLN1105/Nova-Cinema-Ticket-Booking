package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.ShowtimeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "showtimes", indexes = {
    @Index(name = "idx_showtime_status_end", columnList = "status, end_time"),
    @Index(name = "idx_showtime_start_time", columnList = "start_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Showtime {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // Được tính tự động: start_time + movie.duration
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Giá vé cơ bản (VND) — ghế VIP/COUPLE có thể khác
    @Column(name = "base_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShowtimeStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
