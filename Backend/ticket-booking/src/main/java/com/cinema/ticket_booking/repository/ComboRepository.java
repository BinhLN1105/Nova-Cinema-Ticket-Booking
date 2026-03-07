package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComboRepository extends JpaRepository<Combo, UUID> {

    // Lấy tất cả combo đang cung cấp
    List<Combo> findByIsAvailableTrue();
}
