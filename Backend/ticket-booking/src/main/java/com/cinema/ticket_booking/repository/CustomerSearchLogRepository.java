package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.CustomerSearchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerSearchLogRepository extends JpaRepository<CustomerSearchLog, UUID> {
    Page<CustomerSearchLog> findByAccessorUserIdOrderBySearchedAtDesc(UUID accessorUserId, Pageable pageable);
}
