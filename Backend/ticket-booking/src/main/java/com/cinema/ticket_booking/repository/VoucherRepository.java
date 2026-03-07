package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    // Tìm voucher theo mã code (không phân biệt hoa thường)
    Optional<Voucher> findByCodeIgnoreCase(String code);

    // Validate voucher hợp lệ để áp dụng cho đơn hàng
    @Query("""
        SELECT v FROM Voucher v
        WHERE UPPER(v.code) = UPPER(:code)
          AND v.isActive = true
          AND v.validFrom <= :now
          AND v.validTo   >= :now
          AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)
          AND v.minOrder <= :orderAmount
    """)
    Optional<Voucher> findValidVoucher(
        @Param("code") String code,
        @Param("now") LocalDateTime now,
        @Param("orderAmount") BigDecimal orderAmount
    );
}
