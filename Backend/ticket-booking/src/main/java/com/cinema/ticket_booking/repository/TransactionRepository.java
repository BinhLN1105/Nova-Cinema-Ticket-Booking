package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.enums.TransactionType;
import com.cinema.ticket_booking.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<Transaction> findByReferenceId(String referenceId);
    List<Transaction> findAllByReferenceId(String referenceId);
    List<Transaction> findByReferenceIdAndType(String referenceId, TransactionType type);
    boolean existsByReferenceIdAndType(String referenceId, TransactionType type);
}
