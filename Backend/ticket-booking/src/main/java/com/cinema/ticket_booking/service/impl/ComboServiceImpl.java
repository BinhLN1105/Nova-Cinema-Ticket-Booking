package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.response.ComboResponse;
import com.cinema.ticket_booking.mapper.ComboMapper;
import com.cinema.ticket_booking.repository.ComboRepository;
import com.cinema.ticket_booking.service.ComboService;
import com.cinema.ticket_booking.service.CloudinaryService;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.model.Combo;
import com.cinema.ticket_booking.dto.request.CreateComboRequest;
import com.cinema.ticket_booking.dto.request.UpdateComboRequest;
import com.cinema.ticket_booking.enums.ComboType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComboServiceImpl implements ComboService {

    private final ComboRepository comboRepository;
    private final ComboMapper comboMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Cacheable(value = "combos", key = "'available'")
    public List<ComboResponse> getAvailable() {
        return comboRepository.findByIsAvailableTrue()
                .stream().map(comboMapper::toResponse).toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public ComboResponse updateImage(UUID id, MultipartFile file) throws IOException {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo", id));

        String oldUrl = combo.getImageUrl();
        String newUrl = null;

        try {
            newUrl = cloudinaryService.uploadImage(file, "Combo");

            combo.setImageUrl(newUrl);
            comboRepository.save(combo);

            if (oldUrl != null && !oldUrl.isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null)
                    cloudinaryService.deleteImageAsync(publicId);
            }
            return comboMapper.toResponse(combo);
        } catch (Exception e) {
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null)
                    cloudinaryService.deleteImageAsync(newPublicId);
            }
            throw new RuntimeException("Cập nhật ảnh combo thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public ComboResponse updateImageFromUrl(UUID id, String url) throws IOException {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo", id));

        String oldUrl = combo.getImageUrl();
        String newUrl = null;

        try {
            newUrl = cloudinaryService.uploadImageFromUrl(url, "Combo");
            combo.setImageUrl(newUrl);
            comboRepository.save(combo);

            if (oldUrl != null && !oldUrl.isEmpty()) {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null) cloudinaryService.deleteImageAsync(publicId);
            }
            return comboMapper.toResponse(combo);
        } catch (Exception e) {
            if (newUrl != null) {
                String newPublicId = cloudinaryService.extractPublicId(newUrl);
                if (newPublicId != null) cloudinaryService.deleteImageAsync(newPublicId);
            }
            throw new RuntimeException("Cập nhật ảnh combo từ URL thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public ComboResponse createCombo(CreateComboRequest request) {
        Combo combo = Combo.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .isAvailable(request.getIsAvailable())
                .type(ComboType.valueOf(request.getType() != null ? request.getType() : "COMBO"))
                .build();

        combo = comboRepository.save(combo);
        return comboMapper.toResponse(combo);
    }

    @Override
    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public ComboResponse updateCombo(UUID id, UpdateComboRequest request) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo", id));

        combo.setName(request.getName());
        combo.setDescription(request.getDescription());
        combo.setPrice(request.getPrice());

        if (request.getIsAvailable() != null) {
            combo.setIsAvailable(request.getIsAvailable());
        }

        if (request.getType() != null) {
            combo.setType(ComboType.valueOf(request.getType()));
        }

        comboRepository.save(combo);
        return comboMapper.toResponse(combo);
    }

    @Override
    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public void deleteCombo(UUID id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo", id));

        // Soft delete bằng cách set isAvailable = false
        combo.setIsAvailable(false);
        comboRepository.save(combo);
    }
}
