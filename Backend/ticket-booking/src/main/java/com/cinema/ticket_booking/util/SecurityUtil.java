package com.cinema.ticket_booking.util;

import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.UserRole;
import com.cinema.ticket_booking.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Tiện ích lấy thông tin user đang đăng nhập từ SecurityContext.
 * Dùng trong Service khi không inject được qua @AuthenticationPrincipal.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * Trả về User đang đăng nhập.
     * 
     * @throws UnauthorizedException nếu chưa đăng nhập.
     */
    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)) {
            throw new UnauthorizedException();
        }
        return user;
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Kiểm tra user hiện tại có role ADMIN không.
     * Dùng trong Service để bypass quyền khi cần.
     */
    public static boolean isAdmin() {
        try {
            return getCurrentUser().getRole() == UserRole.ADMIN;
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User;
    }
}
