package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.enums.CampaignStatus;
import com.cinema.ticket_booking.model.NotificationCampaign;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationCampaignRepository extends JpaRepository<NotificationCampaign, UUID> {

    /**
     * Tìm các chiến dịch đang PENDING và đã tới giờ gửi.
     */
    List<NotificationCampaign> findByStatusAndScheduledAtBefore(
            CampaignStatus status, LocalDateTime now);

    /**
     * Lấy các chiến dịch sắp xếp theo thời gian mới nhất.
     */
    List<NotificationCampaign> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT c FROM NotificationCampaign c LEFT JOIN FETCH c.createdBy ORDER BY c.createdAt DESC")
    List<NotificationCampaign> findAllWithUser(Pageable pageable);
}
