package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.AuthProvider;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Tìm User theo Email (Dùng cho Login)
    Optional<User> findByEmail(String email);

    // Kiểm tra xem Email đã tồn tại chưa (Dùng cho Đăng ký)
    Boolean existsByEmail(String email);

    // Social login: tìm user theo provider + providerId
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    // Xoá FCM token an toàn (tránh detached entity ghi đè data)
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.fcmToken = null WHERE u.email = :email")
    void clearFcmToken(@Param("email") String email);
}

