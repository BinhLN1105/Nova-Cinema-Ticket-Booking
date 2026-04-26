package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.UserHiddenNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserHiddenNotificationRepository extends JpaRepository<UserHiddenNotification, UUID> {
    
    @Query("SELECT u.globalNotification.id FROM UserHiddenNotification u WHERE u.user.id = :userId")
    List<UUID> findHiddenGlobalNotificationIdsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndGlobalNotificationId(UUID userId, UUID globalNotificationId);
}
