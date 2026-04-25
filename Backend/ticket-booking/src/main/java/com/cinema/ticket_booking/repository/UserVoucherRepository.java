package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.enums.UserVoucherStatus;
import com.cinema.ticket_booking.model.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, UUID> {
    
    @Query("SELECT COUNT(uv) > 0 FROM UserVoucher uv WHERE uv.user.id = :userId AND uv.voucher.id = :voucherId")
    boolean existsByUserIdAndVoucherId(UUID userId, UUID voucherId);
    
    @Query("SELECT uv FROM UserVoucher uv JOIN FETCH uv.voucher v WHERE uv.user.id = :userId ORDER BY uv.savedAt DESC")
    List<UserVoucher> findAllByUserIdWithVoucher(UUID userId);
    
    @Query("SELECT uv FROM UserVoucher uv WHERE uv.user.id = :userId AND uv.voucher.id = :voucherId")
    Optional<UserVoucher> findByUserIdAndVoucherId(UUID userId, UUID voucherId);
}
