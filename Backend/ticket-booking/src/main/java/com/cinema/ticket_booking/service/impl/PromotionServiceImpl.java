package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.PromotionRequest;
import com.cinema.ticket_booking.dto.response.PageResponse;
import com.cinema.ticket_booking.dto.response.PromotionResponse;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.PromotionMapper;
import com.cinema.ticket_booking.model.Promotion;
import com.cinema.ticket_booking.repository.PromotionRepository;
import com.cinema.ticket_booking.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> getAll(Pageable pageable) {
        org.springframework.data.domain.Page<Promotion> page = promotionRepository.findAll(pageable);
        return PageResponse.of(page.map(promotionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDateTime.now()).stream()
                .map(promotionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getById(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khuyến mãi", id));
        return promotionMapper.toResponse(promotion);
    }

    @Override
    public PromotionResponse create(PromotionRequest request) {
        Promotion promotion = promotionMapper.toEntity(request);
        return promotionMapper.toResponse(promotionRepository.save(promotion));
    }

    @Override
    public PromotionResponse update(UUID id, PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khuyến mãi", id));
        promotionMapper.updateEntity(request, promotion);
        return promotionMapper.toResponse(promotionRepository.save(promotion));
    }

    @Override
    public void delete(UUID id) {
        if (!promotionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Khuyến mãi", id);
        }
        promotionRepository.deleteById(id);
    }

    @Override
    public void toggleActive(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khuyến mãi", id));
        promotion.setIsActive(!promotion.getIsActive());
        promotionRepository.save(promotion);
    }
}
