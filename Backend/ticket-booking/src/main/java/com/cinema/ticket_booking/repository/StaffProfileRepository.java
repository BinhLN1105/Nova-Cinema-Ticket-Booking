package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {

    Optional<StaffProfile> findByUserId(UUID userId);

    /**
     * Batch lookup — dùng để tránh N+1 trong AdminController.getUsers().
     * Một câu query JOIN duy nhất lấy StaffProfile + Cinema cho tất cả userId.
     */
    @Query("SELECT sp FROM StaffProfile sp JOIN FETCH sp.cinema WHERE sp.user.id IN :userIds")
    List<StaffProfile> findByUserIdInWithCinema(@Param("userIds") List<UUID> userIds);
}
