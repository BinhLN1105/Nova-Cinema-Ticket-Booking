package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.ScreenType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "screens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_type", nullable = false, length = 20)
    private ScreenType screenType;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows;

    @Column(name = "total_cols", nullable = false)
    private Integer totalCols;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
