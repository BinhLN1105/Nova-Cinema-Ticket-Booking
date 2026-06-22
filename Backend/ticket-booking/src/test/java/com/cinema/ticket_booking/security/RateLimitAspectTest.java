package com.cinema.ticket_booking.security;

import com.cinema.ticket_booking.exception.AppException;
import com.cinema.ticket_booking.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private Environment env;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private RateLimit rateLimit;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    private MockedStatic<RequestContextHolder> mockedRequestContextHolder;
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        mockedRequestContextHolder = mockStatic(RequestContextHolder.class);
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        mockedRequestContextHolder.close();
        mockedSecurityContextHolder.close();
    }

    @Test
    void testLimit_BypassInDevTestProfile() throws Throwable {
        when(env.getActiveProfiles()).thenReturn(new String[]{"test"});
        when(joinPoint.proceed()).thenReturn("success");

        Object result = rateLimitAspect.limit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(joinPoint).proceed();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void testLimit_RequestContextAttributesNull() throws Throwable {
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});
        mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = rateLimitAspect.limit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(joinPoint).proceed();
    }

    @Test
    void testLimit_RateLimitUnderLimitForAnonymousUser() throws Throwable {
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");
        when(securityContext.getAuthentication()).thenReturn(auth);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(rateLimit.key()).thenReturn("test-key");
        when(rateLimit.limit()).thenReturn(5);
        when(rateLimit.period()).thenReturn(60);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        when(joinPoint.proceed()).thenReturn("proceeded");

        Object result = rateLimitAspect.limit(joinPoint, rateLimit);

        assertEquals("proceeded", result);
        verify(redisTemplate).expire(eq("rate_limit:api:test-key:127.0.0.1"), any(Duration.class));
        verify(joinPoint).proceed();
    }

    @Test
    void testLimit_RateLimitUnderLimitForAuthenticatedUser() throws Throwable {
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        User user = mock(User.class);
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(user);
        when(securityContext.getAuthentication()).thenReturn(auth);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(rateLimit.key()).thenReturn("vnpay");
        when(rateLimit.limit()).thenReturn(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(2L);

        when(joinPoint.proceed()).thenReturn("proceeded");

        Object result = rateLimitAspect.limit(joinPoint, rateLimit);

        assertEquals("proceeded", result);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
        verify(joinPoint).proceed();
    }

    @Test
    void testLimit_RateLimitExceeded() throws Throwable {
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(rateLimit.key()).thenReturn("test-key");
        when(rateLimit.limit()).thenReturn(3);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(4L);

        AppException exception = assertThrows(AppException.class, () -> {
            rateLimitAspect.limit(joinPoint, rateLimit);
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
        assertEquals("Quá nhiều yêu cầu. Vui lòng thử lại sau.", exception.getMessage());
        verify(joinPoint, never()).proceed();
    }
}
