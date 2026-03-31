package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.UserExpHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserExpHistoryRepository extends JpaRepository<UserExpHistory, UUID> {
    Page<UserExpHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
