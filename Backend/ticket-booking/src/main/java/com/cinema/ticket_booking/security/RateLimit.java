package com.cinema.ticket_booking.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining rate limits on controller endpoints.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Unique key identifier for the rate limit category (e.g., "login", "vnpay").
     */
    String key() default "";

    /**
     * Maximum number of requests allowed within the specified period.
     */
    int limit() default 10;

    /**
     * Period duration in seconds.
     */
    int period() default 60;
}
