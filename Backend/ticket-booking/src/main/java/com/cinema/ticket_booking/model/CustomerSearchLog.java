package com.cinema.ticket_booking.model;

import com.cinema.ticket_booking.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_search_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "accessor_user_id", nullable = false)
    private UUID accessorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "accessor_role", nullable = false)
    private UserRole accessorRole;

    @Column(name = "search_query", nullable = false)
    private String searchQuery;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "searched_at", nullable = false, updatable = false)
    private LocalDateTime searchedAt;
}
