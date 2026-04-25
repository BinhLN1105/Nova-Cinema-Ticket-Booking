package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Payment;
import com.cinema.ticket_booking.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Lấy payment theo booking
    Optional<Payment> findByBookingId(UUID bookingId);

    // Đối soát giao dịch VNPay theo mã giao dịch
    Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef);

    // Kiểm tra booking đã thanh toán thành công chưa
    boolean existsByBookingIdAndStatus(UUID bookingId, PaymentStatus status);
}
