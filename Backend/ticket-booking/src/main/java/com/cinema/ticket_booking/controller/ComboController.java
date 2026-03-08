package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.ComboResponse;
import com.cinema.ticket_booking.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    // GET /api/v1/combos — danh sách combo đang bán
    @GetMapping
    public ResponseEntity<ApiResponse<List<ComboResponse>>> getAvailable() {
        return ResponseEntity.ok(ApiResponse.success(comboService.getAvailable()));
    }
}
