package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND " +
           "(p.startDate IS NULL OR p.startDate <= :now) AND " +
           "(p.endDate IS NULL OR p.endDate >= :now) " +
           "ORDER BY p.priority DESC")
    List<Promotion> findActivePromotions(LocalDateTime now);
    
    java.util.Optional<Promotion> findFirstByIsPopupTrueAndIsActiveTrueOrderByCreatedAtDesc();
}
