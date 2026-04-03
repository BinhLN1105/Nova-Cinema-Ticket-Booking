package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.ComboResponse;
import com.cinema.ticket_booking.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // POST /api/v1/combos/{id}/image [ADMIN]
    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComboResponse>> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        if (file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn file ảnh");
        if (file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("Dung lượng ảnh tối đa 10MB");
        
        return ResponseEntity.ok(ApiResponse.success(
                comboService.updateImage(id, file), 
                "Tải lên ảnh combo thành công"));
    }

    @PostMapping("/{id}/image-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComboResponse>> uploadImageViaUrl(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) throws IOException {
        String url = request.get("url");
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Vui lòng cung cấp URL ảnh");
        
        return ResponseEntity.ok(ApiResponse.success(
                comboService.updateImageFromUrl(id, url), 
                "Cập nhật ảnh combo từ URL thành công"));
    }
}
