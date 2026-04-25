package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.ScanLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScanLogRepository extends JpaRepository<ScanLog, UUID> {

    /** Lấy lịch sử soát vé của một rạp, sắp xếp theo thời gian mới nhất */
    Page<ScanLog> findByCinemaIdOrderByScannedAtDesc(UUID cinemaId, Pageable pageable);

    long countByCinemaId(UUID cinemaId);
}
