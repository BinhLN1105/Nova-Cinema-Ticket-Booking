package com.cinema.ticket_booking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gift_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftCard {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Mã thẻ dạng GC-XXXXXX
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    // Giá để mua thẻ (VND)
    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    // Số điểm CinePoint (CinePoint balance) thẻ này cộng vào tài khoản khi nạp
    @Column(name = "point_value", nullable = false)
    private Long pointValue;

    @Column(name = "is_redeemed", nullable = false)
    @Builder.Default
    private Boolean isRedeemed = false;

    // User đã mua thẻ này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bought_by")
    private User boughtBy;

    // User đã nạp mã này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_by")
    private User redeemedBy;

    // Thời gian nạp
    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    // Hạn sử dụng của thẻ (nếu chưa nạp sau ngày này thì hết hiệu lực)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
