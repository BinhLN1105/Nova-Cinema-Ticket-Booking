package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.BookingCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingComboRepository extends JpaRepository<BookingCombo, UUID> {
    // Tìm combo theo id vé
    List<BookingCombo> findByBookingId(UUID bookingId);
}
