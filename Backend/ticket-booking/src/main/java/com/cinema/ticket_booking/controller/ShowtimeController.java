package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.ShowtimeRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.SeatMapResponse;
import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    // GET /api/v1/showtimes?movieId=...&date=2024-12-01
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> getShowtimes(
            @RequestParam UUID movieId,
            @RequestParam(required = false) UUID cinemaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ShowtimeResponse> data = (cinemaId != null)
                ? showtimeService.getByMovieCinemaAndDate(movieId, cinemaId, date)
                : showtimeService.getByMovieAndDate(movieId, date);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // GET /api/v1/showtimes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(showtimeService.getById(id)));
    }

    // GET /api/v1/showtimes/{id}/seats — sơ đồ ghế
    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<SeatMapResponse>> getSeatMap(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(showtimeService.getSeatMap(id)));
    }

    // POST /api/v1/showtimes [ADMIN]
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> create(
            @Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(showtimeService.create(request), "Tạo suất chiếu thành công"));
    }

    // GET /api/v1/showtimes/admin — danh sách toàn bộ suất chiếu cho admin
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<ShowtimeResponse>>> adminList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("startTime").descending());
        var result = showtimeService.adminList(pageable, cinemaId, date);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // DELETE /api/v1/showtimes/{id} [ADMIN]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        showtimeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá suất chiếu"));
    }

    // PUT /api/v1/showtimes/{id}/seats/price [ADMIN]
    @PutMapping("/{id}/seats/price")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> overrideSeatPrices(
            @PathVariable UUID id,
            @Valid @RequestBody com.cinema.ticket_booking.dto.request.OverrideSeatPriceRequest request) {
        showtimeService.overrideSeatPrices(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật giá ghế thủ công thành công"));
    }
}
