package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.AuthProvider;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.enums.MembershipTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", length = 150, unique = true, nullable = false)
    private String email;

    // NULLABLE để hỗ trợ Social Login (Google/Facebook)
    @Column(name = "password", length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    // Lưu UID từ Google/Facebook — null nếu LOCAL
    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.CUSTOMER;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Loyalty points earned from ticket cancellations or purchases
    @Column(name = "reward_points", nullable = false, columnDefinition = "bigint not null default 0")
    @Builder.Default
    private Long rewardPoints = 0L;

    // Actual experience points for tier upgrading
    @Column(name = "available_exp", nullable = false, columnDefinition = "bigint not null default 0")
    @Builder.Default
    private Long availableExp = 0L;

    // Membership tier mapping from available_exp
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_tier", nullable = false, length = 20, columnDefinition = "varchar(20) not null default 'BRONZE'")
    @Builder.Default
    private MembershipTier membershipTier = MembershipTier.BRONZE;

    // Firebase Cloud Messaging token cho Push Notification
    @Column(name = "fcm_token", columnDefinition = "TEXT")
    private String fcmToken;

    @Column(name = "allow_marketing_notification", nullable = false, columnDefinition = "boolean not null default true")
    @Builder.Default
    private Boolean allowMarketingNotification = true;

    @Column(name = "allow_transaction_notification", nullable = false, columnDefinition = "boolean not null default true")
    @Builder.Default
    private Boolean allowTransactionNotification = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
