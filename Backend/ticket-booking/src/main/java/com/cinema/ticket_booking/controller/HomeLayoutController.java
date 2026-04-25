package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.MovieResponse;
import com.cinema.ticket_booking.dto.response.PromotionResponse;
import com.cinema.ticket_booking.enums.PlatformType;
import com.cinema.ticket_booking.service.MovieService;
import com.cinema.ticket_booking.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for managing dynamic Home Screen content.
 * Admin-controlled data for Hero Banner and Promotion Popups.
 */
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeLayoutController {

    private final MovieService movieService;
    private final PromotionService promotionService;

    /**
     * GET /api/v1/home/featured-movies
     * Returns a list of featured movies based on Admin configuration (Mode/Manual).
     */
    @GetMapping("/featured-movies")
    public ResponseEntity<ApiResponse<List<MovieResponse.Summary>>> getFeaturedMovies(
            @RequestParam(defaultValue = "ANDROID") PlatformType platform) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getFeaturedMovies(platform)));
    }

    /**
     * GET /api/v1/home/popup-promotion
     * Returns the currently active promotion set to appear as a popup.
     */
    @GetMapping("/popup-promotion")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPopupPromotion() {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getPopupPromotion()));
    }
}
