package com.cinema.ticket_booking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // bật @PreAuthorize trên Controller
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtFilter;
        private final JwtAuthenticationEntryPoint entryPoint;

        @Value("${app.cors.allowed-origins:*}")
        private String allowedOrigins;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
                                .authorizeHttpRequests(auth -> auth

                                                // ── PUBLIC endpoints ─────────────────────────────────────
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/social-login",
                                                                "/api/v1/auth/refresh",
                                                                "/api/v1/auth/logout")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/v1/movies",
                                                                "/api/v1/movies/**",
                                                                "/api/v1/cinemas",
                                                                "/api/v1/cinemas/**",
                                                                "/api/v1/showtimes",
                                                                "/api/v1/showtimes/**",
                                                                "/api/v1/combos",
                                                                "/api/v1/reviews",
                                                                "/api/v1/vouchers/validate",
                                                                "/api/v1/vouchers/active",
                                                                "/api/v1/promotions/**",
                                                                "/api/v1/home/**")
                                                .permitAll()

                                                // VNPay callback không mang token
                                                .requestMatchers(HttpMethod.GET, "/api/v1/payment/vnpay-return",
                                                                "/api/v1/payments/vnpay/callback",
                                                                "/api/v1/wallet/vnpay-return",
                                                                "/api/v1/gift-cards/vnpay-return")
                                                .permitAll()

                                                // Internal API (Python RAG) - Protected by X-Internal-Key in Controller
                                                .requestMatchers("/internal/api/**")
                                                .permitAll()

                                                // Swagger / Actuator (dev)
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/actuator/health")
                                                .permitAll()

                                                // ── Tất cả endpoint còn lại cần đăng nhập ────────────────
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Hỗ trợ nhiều origin từ cấu hình (comma-separated)
                if (allowedOrigins != null && !allowedOrigins.isBlank()) {
                        configuration.setAllowedOriginPatterns(java.util.Arrays.asList(allowedOrigins.split(",")));
                } else {
                        configuration.setAllowedOriginPatterns(List.of("*"));
                }

                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
