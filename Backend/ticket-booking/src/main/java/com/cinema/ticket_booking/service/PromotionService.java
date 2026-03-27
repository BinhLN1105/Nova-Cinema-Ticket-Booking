package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.dto.request.PromotionRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PromotionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PromotionService {

    PageResponse<PromotionResponse> getAll(Pageable pageable);

    List<PromotionResponse> getActivePromotions();

    PromotionResponse getById(UUID id);

    PromotionResponse create(PromotionRequest request);

    PromotionResponse update(UUID id, PromotionRequest request);

    void delete(UUID id);

    void toggleActive(UUID id);
}
