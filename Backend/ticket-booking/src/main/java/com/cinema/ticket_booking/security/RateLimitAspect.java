package com.cinema.ticket_booking.security;

import com.cinema.ticket_booking.exception.AppException;
import com.cinema.ticket_booking.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.springframework.core.env.Environment;
import java.util.Arrays;
import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;
    private final Environment env;

    @Around("@annotation(rateLimit)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // Bypass rate limiting in dev/test environment to prevent automated tests from failing with 429
        if (env != null && (Arrays.asList(env.getActiveProfiles()).contains("test") || 
                            Arrays.asList(env.getActiveProfiles()).contains("dev"))) {
            return joinPoint.proceed();
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String clientIp = getClientIp(request);
        String identifier = clientIp;

        // Lấy User ID nếu người dùng đã xác thực
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User) {
                identifier = ((User) principal).getId().toString();
            }
        }

        // Tạo key Redis dựa trên tên key cấu hình + định danh user
        String redisKey = "rate_limit:api:" + rateLimit.key() + ":" + identifier;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            // Đặt thời gian hết hạn cho key khi khởi tạo lần đầu
            redisTemplate.expire(redisKey, Duration.ofSeconds(rateLimit.period()));
        }

        if (count != null && count > rateLimit.limit()) {
            log.warn("Rate limit exceeded for key: {} (count: {}, limit: {})", redisKey, count, rateLimit.limit());
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "Quá nhiều yêu cầu. Vui lòng thử lại sau.");
        }

        return joinPoint.proceed();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
