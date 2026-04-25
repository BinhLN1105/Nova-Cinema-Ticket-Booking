package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.Cinema;
import com.cinema.ticket_booking.model.ScanLog;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.repository.ScanLogRepository;
import com.cinema.ticket_booking.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service riêng biệt với Propagation.REQUIRES_NEW để lưu ScanLog thất bại
 * mà không bị rollback cùng với transaction chính của checkIn.
 */
@Service
@RequiredArgsConstructor
public class ScanLogServiceImpl {

    private final ScanLogRepository scanLogRepository;
    private final StaffProfileRepository staffProfileRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedScan(User staff, String qrCode, String failReason) {
        Cinema staffCinema = staffProfileRepository.findByUserId(staff.getId())
                .map(profile -> profile.getCinema())
                .orElse(null);

        if (staffCinema == null) return; // Không thể log nếu không biết rạp

        scanLogRepository.save(ScanLog.builder()
                .staff(staff)
                .cinema(staffCinema)
                .qrCodeRaw(qrCode)
                .success(false)
                .failReason(failReason)
                .build());
    }
}
