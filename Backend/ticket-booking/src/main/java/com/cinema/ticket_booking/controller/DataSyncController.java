package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.*;
import com.cinema.ticket_booking.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DataSyncController.java
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * Read-only endpoints dành riêng cho Python RAG server.
 * Bảo mật bằng X-Internal-Key header (không dùng JWT).
 *
 * URL pattern: /internal/api/**
 * Nên cấu hình firewall chỉ cho Python server IP gọi được.
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
@RestController
@RequestMapping("/internal/api")
@RequiredArgsConstructor
public class DataSyncController {

    @Value("${nova.internal.api-key}")
    private String internalApiKey;

    private final MovieService       movieService;
    private final ShowtimeService    showtimeService;
    private final SeatMapService     seatMapService;
    private final VoucherService     voucherService;
    private final CinemaService      cinemaService;

    // ── Security ────────────────────────────────────────────
    private void validateKey(String key) {
        if (!internalApiKey.equals(key)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Invalid internal API key"
            );
        }
    }

    // ════════════════════════════════════════════════════════
    //  GET /internal/api/movies/now-showing
    //  Python Tool: get_now_showing_movies()
    // ════════════════════════════════════════════════════════
    @GetMapping("/movies/now-showing")
    public ResponseEntity<List<MovieSyncResponse>> getNowShowing(
        @RequestHeader("X-Internal-Key") String key,
        @RequestParam(required = false) String genre
    ) {
        validateKey(key);
        // Dùng lại service hiện có, chỉ trả thêm field cần cho RAG
        return ResponseEntity.ok(movieService.getNowShowingForSync(genre));
    }

    // ════════════════════════════════════════════════════════
    //  GET /internal/api/showtimes?movieTitle=...&date=...
    //  Python Tool: get_showtimes()
    // ════════════════════════════════════════════════════════
    @GetMapping("/showtimes")
    public ResponseEntity<List<ShowtimeSyncResponse>> getShowtimes(
        @RequestHeader("X-Internal-Key") String key,
        @RequestParam(required = false) String movieTitle,
        @RequestParam(required = false) UUID   movieId,
        @RequestParam(required = false) String cinemaName,
        @RequestParam(required = false) String date
    ) {
        validateKey(key);
        LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(
            showtimeService.getShowtimesForSync(movieTitle, movieId, cinemaName, targetDate)
        );
    }

    // ════════════════════════════════════════════════════════
    //  GET /internal/api/seats/available?showtimeId=...
    //  Python Tool: get_available_seats()
    // ════════════════════════════════════════════════════════
    @GetMapping("/seats/available")
    public ResponseEntity<SeatMapResponse> getAvailableSeats(
        @RequestHeader("X-Internal-Key") String key,
        @RequestParam UUID showtimeId
    ) {
        validateKey(key);
        // Ưu tiên lấy từ Redis cache trước, fallback sang DB
        return ResponseEntity.ok(seatMapService.getSeatMap(showtimeId));
    }

    // ════════════════════════════════════════════════════════
    //  GET /internal/api/vouchers/active
    //  Python Tool: get_active_vouchers()
    // ════════════════════════════════════════════════════════
    @GetMapping("/vouchers/active")
    public ResponseEntity<List<VoucherSyncResponse>> getActiveVouchers(
        @RequestHeader("X-Internal-Key") String key
    ) {
        validateKey(key);
        return ResponseEntity.ok(voucherService.getActiveVouchersForSync());
    }

    // ════════════════════════════════════════════════════════
    //  GET /internal/api/cinemas
    //  (Dùng khi ingest cold data vào Vector DB)
    // ════════════════════════════════════════════════════════
    @GetMapping("/cinemas")
    public ResponseEntity<List<CinemaSyncResponse>> getCinemas(
        @RequestHeader("X-Internal-Key") String key
    ) {
        validateKey(key);
        return ResponseEntity.ok(cinemaService.getAllForSync());
    }
}
