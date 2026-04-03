package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.PromotionRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PromotionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface PromotionService {

    PageResponse<PromotionResponse> getAll(Pageable pageable);

    List<PromotionResponse> getActivePromotions();
    
    PromotionResponse getPopupPromotion();

    PromotionResponse getById(UUID id);

    PromotionResponse create(PromotionRequest request);

    PromotionResponse update(UUID id, PromotionRequest request);

    PromotionResponse updateImage(UUID id, MultipartFile file) throws IOException;

    PromotionResponse updateImageFromUrl(UUID id, String url) throws IOException;

    void delete(UUID id);

    void toggleActive(UUID id);
}
