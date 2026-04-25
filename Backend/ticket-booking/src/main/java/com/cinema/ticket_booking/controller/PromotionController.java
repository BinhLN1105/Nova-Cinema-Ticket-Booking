package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.PromotionRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PromotionResponse;
import com.cinema.ticket_booking.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // Public
    @GetMapping("/promotions/active")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getActivePromotions(), "Lấy danh sách khuyến mãi thành công"));
    }

    @GetMapping("/promotions/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getById(id), "Lấy thông tin khuyến mãi thành công"));
    }

    // Admin
    @GetMapping("/admin/promotions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PromotionResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getAll(pageable), "Lấy danh sách khuyến mãi thành công"));
    }

    @PostMapping("/admin/promotions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> create(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(promotionService.create(request), "Tạo khuyến mãi thành công"));
    }

    @PutMapping("/admin/promotions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> update(
            @PathVariable UUID id, 
            @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.update(id, request), "Cập nhật khuyến mãi thành công"));
    }

    @DeleteMapping("/admin/promotions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        promotionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa khuyến mãi thành công"));
    }

    @PostMapping("/admin/promotions/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        if (file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn file ảnh");
        if (file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("Dung lượng ảnh tối đa 10MB");
        
        return ResponseEntity.ok(ApiResponse.success(
                promotionService.updateImage(id, file), 
                "Tải lên ảnh khuyến mãi thành công"));
    }

    @PostMapping("/admin/promotions/{id}/image-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> uploadImageViaUrl(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) throws IOException {
        String url = request.get("url");
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Vui lòng cung cấp URL ảnh");
        
        return ResponseEntity.ok(ApiResponse.success(
                promotionService.updateImageFromUrl(id, url), 
                "Cập nhật ảnh khuyến mãi từ URL thành công"));
    }

    @PatchMapping("/admin/promotions/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggle(@PathVariable UUID id) {
        promotionService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật trạng thái khuyến mãi"));
    }
}
