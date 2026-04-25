package com.cinema.ticket_booking.util;

import com.cinema.ticket_booking.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Tiện ích JWT thuần — parse, extract, validate token.
 *
 * Khác với JwtService (Spring @Service, inject được):
 * JwtUtil là static utility — dùng được ở bất kỳ đâu không cần Spring context.
 * Thích hợp dùng trong filter, test, hoặc các class không được Spring quản lý.
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    // ── Build token ───────────────────────────────────────────────────────

    /**
     * Tạo JWT access token từ User entity.
     *
     * @param user         User đang đăng nhập
     * @param secret       JWT secret key (>= 32 ký tự)
     * @param expiryMillis Thời gian hết hạn tính bằng milliseconds
     */
    public static String generateAccessToken(User user, String secret, long expiryMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMillis);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole().name()))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(buildKey(secret))
                .compact();
    }

    /**
     * Tạo opaque refresh token (UUID hex, 64 ký tự).
     * Không mang thông tin, chỉ dùng để tra cứu trong DB.
     */
    public static String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    // ── Extract ───────────────────────────────────────────────────────────

    /** Lấy userId (subject) từ token */
    public static String extractUserId(String token, String secret) {
        return extractClaim(token, secret, Claims::getSubject);
    }

    /** Lấy email từ claims */
    public static String extractEmail(String token, String secret) {
        return extractClaim(token, secret, c -> c.get("email", String.class));
    }

    /** Lấy role từ claims */
    public static String extractRole(String token, String secret) {
        return extractClaim(token, secret, c -> c.get("role", String.class));
    }

    /** Lấy thời điểm hết hạn */
    public static Date extractExpiration(String token, String secret) {
        return extractClaim(token, secret, Claims::getExpiration);
    }

    /** Generic extract — truyền vào function để lấy bất kỳ claim nào */
    public static <T> T extractClaim(String token, String secret, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token, secret));
    }

    // ── Validate ──────────────────────────────────────────────────────────

    /**
     * Kiểm tra token hợp lệ về mặt chữ ký và chưa hết hạn.
     * Không throw exception — trả về false nếu không hợp lệ.
     */
    public static boolean isValid(String token, String secret) {
        try {
            extractAllClaims(token, secret);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Kiểm tra token đã hết hạn chưa (không quan tâm chữ ký).
     * Hữu ích khi muốn thông báo riêng "token hết hạn" vs "token giả mạo".
     */
    public static boolean isExpired(String token, String secret) {
        try {
            return extractExpiration(token, secret).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Kiểm tra token hợp lệ và thuộc về đúng user.
     *
     * @param token  JWT access token
     * @param userId UUID của user cần kiểm tra
     * @param secret JWT secret key
     */
    public static boolean isValidForUser(String token, UUID userId, String secret) {
        try {
            String tokenUserId = extractUserId(token, secret);
            return userId.toString().equals(tokenUserId) && !isExpired(token, secret);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Lấy thông tin từ token đã hết hạn (dùng khi cần biết userId để refresh).
     * Chỉ dùng khi chủ động xử lý expired token — KHÔNG dùng để xác thực.
     */
    public static Claims extractClaimsIgnoreExpiry(String token, String secret) {
        try {
            return extractAllClaims(token, secret);
        } catch (ExpiredJwtException e) {
            return e.getClaims(); // lấy claims từ token hết hạn
        }
    }

    // ── Private ───────────────────────────────────────────────────────────

    private static Claims extractAllClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith(buildKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey buildKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
