package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.CinemaRequest;
import com.cinema.ticket_booking.dto.request.ScreenRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.CinemaResponse;
import com.cinema.ticket_booking.dto.response.ScreenResponse;
import com.cinema.ticket_booking.service.CinemaService;
import com.cinema.ticket_booking.service.ScreenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;
    private final ScreenService screenService;

    // ── Cinema ────────────────────────────────────────────────────────────

    // GET /api/v1/cinemas?city=HoChiMinh
    @GetMapping
    public ResponseEntity<ApiResponse<List<CinemaResponse>>> getAll(
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(ApiResponse.success(cinemaService.getAll(city)));
    }

    // GET /api/v1/cinemas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(cinemaService.getById(id)));
    }

    // POST /api/v1/cinemas [ADMIN]
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CinemaResponse>> create(
            @Valid @RequestBody CinemaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cinemaService.create(request), "Tạo rạp thành công"));
    }

    // PUT /api/v1/cinemas/{id} [ADMIN]
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CinemaResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CinemaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cinemaService.update(id, request), "Cập nhật thành công"));
    }

    // DELETE /api/v1/cinemas/{id} [ADMIN]
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        cinemaService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã vô hiệu hoá rạp"));
    }

    // ── Screen (nested resource) ──────────────────────────────────────────

    // GET /api/v1/cinemas/{cinemaId}/screens
    @GetMapping("/{cinemaId}/screens")
    public ResponseEntity<ApiResponse<List<ScreenResponse>>> getScreens(
            @PathVariable UUID cinemaId) {
        return ResponseEntity.ok(ApiResponse.success(screenService.getByCinema(cinemaId)));
    }

    // POST /api/v1/cinemas/{cinemaId}/screens [ADMIN]
    @PostMapping("/{cinemaId}/screens")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScreenResponse>> createScreen(
            @PathVariable UUID cinemaId,
            @Valid @RequestBody ScreenRequest request) {
        request.setCinemaId(cinemaId.toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(screenService.create(request), "Tạo phòng chiếu thành công"));
    }

    // PUT /api/v1/cinemas/{cinemaId}/screens/{screenId} [ADMIN]
    @PutMapping("/{cinemaId}/screens/{screenId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScreenResponse>> updateScreen(
            @PathVariable UUID cinemaId,
            @PathVariable UUID screenId,
            @Valid @RequestBody ScreenRequest request) {
        request.setCinemaId(cinemaId.toString());
        return ResponseEntity
                .ok(ApiResponse.success(screenService.update(screenId, request), "Cập nhật phòng chiếu thành công"));
    }
}
