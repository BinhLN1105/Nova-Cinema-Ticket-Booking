package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.GiftCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GiftCardRepository extends JpaRepository<GiftCard, UUID> {
    Optional<GiftCard> findByCode(String code);
    Page<GiftCard> findByRedeemedByIdOrderByRedeemedAtDesc(UUID userId, Pageable pageable);
    Page<GiftCard> findByBoughtByIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
