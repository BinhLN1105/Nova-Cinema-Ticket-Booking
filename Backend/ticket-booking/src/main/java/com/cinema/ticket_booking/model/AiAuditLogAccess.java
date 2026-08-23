package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_audit_log_accesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAuditLogAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "accessor_user_id", nullable = false)
    private UUID accessorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "accessor_role", nullable = false)
    private UserRole accessorRole;

    @Column(name = "target_session_id")
    private String targetSessionId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "reason", length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "accessed_at", nullable = false, updatable = false)
    private LocalDateTime accessedAt;
}
