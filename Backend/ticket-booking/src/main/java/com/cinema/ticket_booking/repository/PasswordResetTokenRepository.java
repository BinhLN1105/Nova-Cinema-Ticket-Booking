package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.PasswordResetToken;
import com.cinema.ticket_booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    Optional<PasswordResetToken> findByUser(User user);
    
    void deleteByUser(User user);
    
    void deleteByToken(String token);
}
