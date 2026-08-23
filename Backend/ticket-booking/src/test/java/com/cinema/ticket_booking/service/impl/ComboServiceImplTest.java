package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.CreateComboRequest;
import com.cinema.ticket_booking.dto.request.UpdateComboRequest;
import com.cinema.ticket_booking.dto.response.ComboResponse;
import com.cinema.ticket_booking.enums.ComboType;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.ComboMapper;
import com.cinema.ticket_booking.model.Combo;
import com.cinema.ticket_booking.repository.ComboRepository;
import com.cinema.ticket_booking.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComboServiceImplTest {

    @Mock
    private ComboRepository comboRepository;
    @Mock
    private ComboMapper comboMapper;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ComboServiceImpl comboService;

    private UUID comboId;
    private Combo combo;
    private ComboResponse comboResponse;

    @BeforeEach
    void setUp() {
        comboId = UUID.randomUUID();
        combo = Combo.builder()
                .id(comboId)
                .name("Combo Solo")
                .price(BigDecimal.valueOf(89000))
                .isAvailable(true)
                .type(ComboType.COMBO)
                .build();

        comboResponse = ComboResponse.builder()
                .id(comboId.toString())
                .name("Combo Solo")
                .price(BigDecimal.valueOf(89000))
                .isAvailable(true)
                .build();
    }

    @Test
    @DisplayName("1. getAvailable trả về danh sách combo khả dụng")
    void testGetAvailable() {
        when(comboRepository.findByIsAvailableTrue()).thenReturn(List.of(combo));
        when(comboMapper.toResponse(combo)).thenReturn(comboResponse);

        List<ComboResponse> result = comboService.getAvailable();
        assertEquals(1, result.size());
        assertEquals("Combo Solo", result.get(0).getName());
    }

    @Test
    @DisplayName("2. createCombo thành công")
    void testCreateCombo() {
        CreateComboRequest request = new CreateComboRequest();
        request.setName("Combo Solo");
        request.setPrice(BigDecimal.valueOf(89000));
        request.setType("COMBO");
        request.setIsAvailable(true);

        when(comboRepository.save(any(Combo.class))).thenReturn(combo);
        when(comboMapper.toResponse(any(Combo.class))).thenReturn(comboResponse);

        ComboResponse result = comboService.createCombo(request);
        assertNotNull(result);
        verify(comboRepository, times(1)).save(any(Combo.class));
    }

    @Test
    @DisplayName("3. updateCombo ném ResourceNotFoundException khi combo không tồn tại")
    void testUpdateComboNotFound() {
        UpdateComboRequest request = new UpdateComboRequest();
        when(comboRepository.findById(comboId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> comboService.updateCombo(comboId, request));
    }

    @Test
    @DisplayName("4. deleteCombo xóa thành công khi combo tồn tại")
    void testDeleteComboSuccess() {
        when(comboRepository.findById(comboId)).thenReturn(Optional.of(combo));

        comboService.deleteCombo(comboId);
        verify(comboRepository, times(1)).save(combo);
    }
}
