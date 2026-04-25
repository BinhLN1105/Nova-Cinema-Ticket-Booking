package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    java.util.Optional<Transaction> findByReferenceId(String referenceId);
}
