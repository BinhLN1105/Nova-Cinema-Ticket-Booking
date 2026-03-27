package com.cinema.ticket_booking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "user_vouchers",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_voucher",
        columnNames = {"user_id", "voucher_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVoucher {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @CreationTimestamp
    @Column(name = "saved_at", updatable = false)
    private LocalDateTime savedAt;
}
