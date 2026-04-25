package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_seat_screen_row_col",
            columnNames = {"screen_id", "row_label", "col_number"}
        ),
        @UniqueConstraint(
            name = "uk_seat_screen_grid",
            columnNames = {"screen_id", "grid_row", "grid_col"}
        )
    }
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

    // Ký tự hàng: A, B, C, ... (legacy, giữ lại cho backward compat)
    @Column(name = "row_label", length = 1, nullable = false)
    private Character rowLabel;

    // Số cột: 1, 2, 3, ... (legacy, giữ lại cho backward compat)
    @Column(name = "col_number", nullable = false)
    private Integer colNumber;

    // ── Virtual Grid: vị trí trên ma trận ảo để UI render ──
    @Column(name = "grid_row", nullable = false)
    @Builder.Default
    private Integer gridRow = 0;

    @Column(name = "grid_col", nullable = false)
    @Builder.Default
    private Integer gridCol = 0;

    // Nhãn hiển thị cho khách hàng (VD: "A1", "VIP-5")
    @Column(name = "seat_label", length = 10, nullable = false)
    @Builder.Default
    private String seatLabel = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
