package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_seat_screen_row_col",
        columnNames = {"screen_id", "row_label", "col_number"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    // Ký tự hàng: A, B, C, ...
    @Column(name = "row_label", length = 1, nullable = false)
    private Character rowLabel;

    // Số cột: 1, 2, 3, ...
    @Column(name = "col_number", nullable = false)
    private Integer colNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
