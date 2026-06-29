package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.dto.request.PromotionRequest;
import com.cinema.ticket_booking.dto.request.VoucherRequest;
import com.cinema.ticket_booking.dto.response.PromotionResponse;
import com.cinema.ticket_booking.dto.response.VoucherResponse;
import com.cinema.ticket_booking.enums.DiscountType;
import com.cinema.ticket_booking.enums.UserVoucherStatus;
import com.cinema.ticket_booking.exception.BadRequestException;
import com.cinema.ticket_booking.exception.ConflictException;
import com.cinema.ticket_booking.exception.ResourceNotFoundException;
import com.cinema.ticket_booking.mapper.PromotionMapper;
import com.cinema.ticket_booking.mapper.VoucherMapper;
import com.cinema.ticket_booking.model.Promotion;
import com.cinema.ticket_booking.model.UserVoucher;
import com.cinema.ticket_booking.model.Voucher;
import com.cinema.ticket_booking.repository.BookingRepository;
import com.cinema.ticket_booking.repository.PromotionRepository;
import com.cinema.ticket_booking.repository.UserVoucherRepository;
import com.cinema.ticket_booking.repository.VoucherRepository;
import com.cinema.ticket_booking.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** White-box, repository-isolated tests for voucher validation and promotion management. */
@ExtendWith(MockitoExtension.class)
@Transactional
class VoucherPromotionTest {

    @Mock private VoucherRepository voucherRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private VoucherMapper voucherMapper;
    @Mock private UserVoucherRepository userVoucherRepository;
    @InjectMocks private VoucherServiceImpl voucherService;

    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionMapper promotionMapper;
    @Mock private CloudinaryService cloudinaryService;
    @InjectMocks private PromotionServiceImpl promotionService;

    @Test
    void validateForOrder_returnsVoucherWhenEligible() {
        UUID userId = UUID.randomUUID();
        Voucher voucher = voucher();
        when(voucherRepository.findValidVoucher(eq("SAVE20"), any(), eq(BigDecimal.valueOf(100_000))))
                .thenReturn(Optional.of(voucher));
        when(userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId())).thenReturn(Optional.empty());

        assertSame(voucher, voucherService.validateForOrder(userId, "SAVE20", BigDecimal.valueOf(100_000)));
    }

    @Test
    void validateForOrder_rejectsExpiredOrBelowMinimumVoucher() {
        when(voucherRepository.findValidVoucher(eq("EXPIRED"), any(), any())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> voucherService.validateForOrder(UUID.randomUUID(), "EXPIRED", BigDecimal.TEN));
    }

    @Test
    void validateForOrder_rejectsVoucherAlreadyUsedByCustomer() {
        UUID userId = UUID.randomUUID();
        Voucher voucher = voucher();
        UserVoucher userVoucher = UserVoucher.builder().status(UserVoucherStatus.USED).build();
        when(voucherRepository.findValidVoucher(anyString(), any(), any())).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId())).thenReturn(Optional.of(userVoucher));

        assertThrows(BadRequestException.class,
                () -> voucherService.validateForOrder(userId, "SAVE20", BigDecimal.valueOf(100_000)));
    }

    @Test
    void validateForOrder_rejectsVoucherHeldByActivePendingBooking() {
        UUID userId = UUID.randomUUID();
        Voucher voucher = voucher();
        UserVoucher userVoucher = UserVoucher.builder().status(UserVoucherStatus.PENDING).build();
        when(voucherRepository.findValidVoucher(anyString(), any(), any())).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId())).thenReturn(Optional.of(userVoucher));
        when(bookingRepository.existsByUserIdAndVoucherIdAndStatusAndExpiresAtAfter(eq(userId), eq(voucher.getId()), any(), any()))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> voucherService.validateForOrder(userId, "SAVE20", BigDecimal.valueOf(100_000)));
    }

    @Test
    void validateForOrder_releasesExpiredPendingHold() {
        UUID userId = UUID.randomUUID();
        Voucher voucher = voucher();
        when(voucherRepository.findValidVoucher(anyString(), any(), any())).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId()))
                .thenReturn(Optional.of(UserVoucher.builder().status(UserVoucherStatus.PENDING).build()));
        when(bookingRepository.existsByUserIdAndVoucherIdAndStatusAndExpiresAtAfter(eq(userId), eq(voucher.getId()), any(), any()))
                .thenReturn(false);

        assertSame(voucher, voucherService.validateForOrder(userId, "SAVE20", BigDecimal.valueOf(100_000)));
    }

    @Test
    void incrementUsedCount_incrementsUntilLimitAndThenRejects() {
        Voucher voucher = voucher();
        voucher.setUsageLimit(2);
        voucher.setUsedCount(1);
        when(voucherRepository.findById(voucher.getId())).thenReturn(Optional.of(voucher));

        voucherService.incrementUsedCount(voucher.getId());
        assertEquals(2, voucher.getUsedCount());
        verify(voucherRepository).save(voucher);

        assertThrows(BadRequestException.class, () -> voucherService.incrementUsedCount(voucher.getId()));
    }

    @Test
    void create_rejectsDuplicateAndPersistsNewVoucher() {
        VoucherRequest request = mock(VoucherRequest.class);
        when(request.getCode()).thenReturn("SAVE20");
        when(voucherRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(voucher()));
        assertThrows(ConflictException.class, () -> voucherService.create(request));

        Voucher entity = voucher();
        VoucherResponse response = VoucherResponse.builder().build();
        when(voucherRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.empty());
        when(voucherMapper.toEntity(request)).thenReturn(entity);
        when(voucherRepository.save(entity)).thenReturn(entity);
        when(voucherMapper.toResponse(entity)).thenReturn(response);
        assertSame(response, voucherService.create(request));
    }

    @Test
    void toggleAndDeleteHandleExistingAndMissingVoucher() {
        Voucher voucher = voucher();
        when(voucherRepository.findById(voucher.getId())).thenReturn(Optional.of(voucher));
        voucherService.toggleActive(voucher.getId());
        assertFalse(voucher.getIsActive());

        when(voucherRepository.existsById(voucher.getId())).thenReturn(true);
        voucherService.delete(voucher.getId());
        verify(voucherRepository).deleteById(voucher.getId());

        UUID missing = UUID.randomUUID();
        when(voucherRepository.existsById(missing)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> voucherService.delete(missing));
    }

    @Test
    void getSummaryRejectsInactiveVoucherAndReturnsActiveVoucher() {
        Voucher voucher = voucher();
        when(voucherRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(voucher));
        voucher.setIsActive(false);
        assertThrows(BadRequestException.class, () -> voucherService.getSummaryByCode("SAVE20"));

        VoucherResponse.Summary summary = mock(VoucherResponse.Summary.class);
        voucher.setIsActive(true);
        when(voucherMapper.toSummary(voucher)).thenReturn(summary);
        assertSame(summary, voucherService.getSummaryByCode("SAVE20"));
    }

    @Test
    void voucherReadAndUpdateOperationsMapAndPersistEntities() {
        Voucher voucher = voucher();
        VoucherRequest request = mock(VoucherRequest.class);
        VoucherResponse response = VoucherResponse.builder().build();
        when(voucherRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(voucher)));
        when(voucherMapper.toResponse(voucher)).thenReturn(response);
        assertEquals(1, voucherService.getAll(PageRequest.of(0, 10)).getContent().size());

        when(voucherRepository.findById(voucher.getId())).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(voucher)).thenReturn(voucher);
        assertSame(response, voucherService.update(voucher.getId(), request));
        verify(voucherMapper).updateEntity(request, voucher);
        assertSame(voucher, voucherService.findById(voucher.getId()));
    }

    @Test
    void activeVoucherSyncFiltersExpiredVouchers() {
        Voucher active = voucher();
        Voucher expired = voucher();
        expired.setValidTo(LocalDateTime.now().minusMinutes(1));
        when(voucherRepository.findAll()).thenReturn(List.of(active, expired));

        assertEquals(1, voucherService.getActiveVouchersForSync().size());
    }

    @Test
    void summaryRejectsNotYetActiveAndExhaustedVoucher() {
        Voucher voucher = voucher();
        when(voucherRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(voucher));
        voucher.setValidFrom(LocalDateTime.now().plusDays(1));
        assertThrows(BadRequestException.class, () -> voucherService.getSummaryByCode("SAVE20"));

        voucher.setValidFrom(LocalDateTime.now().minusDays(1));
        voucher.setUsageLimit(1);
        voucher.setUsedCount(1);
        assertThrows(BadRequestException.class, () -> voucherService.getSummaryByCode("SAVE20"));
    }

    @Test
    void createPopupDisablesExistingPopupBeforeSavingNewOne() {
        PromotionRequest request = mock(PromotionRequest.class);
        Promotion existing = Promotion.builder().id(UUID.randomUUID()).isPopup(true).isActive(true).build();
        Promotion incoming = Promotion.builder().id(UUID.randomUUID()).isPopup(true).isActive(true).build();
        PromotionResponse response = PromotionResponse.builder().build();
        when(promotionMapper.toEntity(request)).thenReturn(incoming);
        when(promotionRepository.findFirstByIsPopupTrueAndIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(existing));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promotionMapper.toResponse(incoming)).thenReturn(response);

        assertSame(response, promotionService.create(request));
        assertFalse(existing.getIsPopup());
        verify(promotionRepository).save(existing);
    }

    @Test
    void updateToPopupDisablesOtherPopupAndToggleDeleteValidateExistence() {
        UUID id = UUID.randomUUID();
        Promotion current = Promotion.builder().id(id).isPopup(false).isActive(true).build();
        Promotion existing = Promotion.builder().id(UUID.randomUUID()).isPopup(true).isActive(true).build();
        PromotionRequest request = mock(PromotionRequest.class);
        PromotionResponse response = PromotionResponse.builder().build();
        when(promotionRepository.findById(id)).thenReturn(Optional.of(current));
        doAnswer(invocation -> { invocation.getArgument(1, Promotion.class).setIsPopup(true); return null; })
                .when(promotionMapper).updateEntity(request, current);
        when(promotionRepository.findFirstByIsPopupTrueAndIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(existing));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promotionMapper.toResponse(current)).thenReturn(response);

        assertSame(response, promotionService.update(id, request));
        assertFalse(existing.getIsPopup());

        promotionService.toggleActive(id);
        assertFalse(current.getIsActive());
        when(promotionRepository.existsById(id)).thenReturn(true);
        promotionService.delete(id);
        verify(promotionRepository).deleteById(id);
    }

    @Test
    void promotionReadOperationsMapResultsAndMissingIdIsRejected() {
        Promotion promotion = Promotion.builder().id(UUID.randomUUID()).build();
        PromotionResponse response = PromotionResponse.builder().build();
        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(promotion));
        when(promotionMapper.toResponse(promotion)).thenReturn(response);
        assertEquals(List.of(response), promotionService.getActivePromotions());

        when(promotionRepository.findById(promotion.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> promotionService.getById(promotion.getId()));
    }

    @Test
    void promotionListAndPopupOperationsReturnMappedValues() {
        Promotion promotion = Promotion.builder().id(UUID.randomUUID()).build();
        PromotionResponse response = PromotionResponse.builder().build();
        when(promotionRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(promotion)));
        when(promotionRepository.findFirstByIsPopupTrueAndIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.of(promotion));
        when(promotionMapper.toResponse(promotion)).thenReturn(response);

        assertEquals(1, promotionService.getAll(PageRequest.of(0, 10)).getContent().size());
        assertSame(response, promotionService.getPopupPromotion());

        when(promotionRepository.findFirstByIsPopupTrueAndIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        assertNull(promotionService.getPopupPromotion());
    }

    @Test
    void updateImageReplacesImageAndCleansUpPreviousCloudinaryAsset() throws Exception {
        Promotion promotion = Promotion.builder().id(UUID.randomUUID()).isPopup(false).imageUrl("old-url").build();
        MultipartFile file = mock(MultipartFile.class);
        PromotionResponse response = PromotionResponse.builder().build();
        when(promotionRepository.findById(promotion.getId())).thenReturn(Optional.of(promotion));
        when(cloudinaryService.uploadImage(file, "Banner")).thenReturn("new-url");
        when(cloudinaryService.extractPublicId("old-url")).thenReturn("old-id");
        when(promotionRepository.save(promotion)).thenReturn(promotion);
        when(promotionMapper.toResponse(promotion)).thenReturn(response);

        assertSame(response, promotionService.updateImage(promotion.getId(), file));
        assertEquals("new-url", promotion.getImageUrl());
        verify(cloudinaryService).deleteImageAsync("old-id");
    }

    @Test
    void updateImageFromUrlCleansNewAssetWhenPersistenceFails() throws Exception {
        Promotion promotion = Promotion.builder().id(UUID.randomUUID()).isPopup(true).build();
        when(promotionRepository.findById(promotion.getId())).thenReturn(Optional.of(promotion));
        when(cloudinaryService.uploadImageFromUrl("https://image", "Promotion")).thenReturn("new-url");
        when(promotionRepository.save(promotion)).thenThrow(new RuntimeException("database unavailable"));
        when(cloudinaryService.extractPublicId("new-url")).thenReturn("new-id");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> promotionService.updateImageFromUrl(promotion.getId(), "https://image"));
        assertTrue(exception.getMessage().contains("Cập nhật ảnh"));
        verify(cloudinaryService).deleteImageAsync("new-id");
    }

    private Voucher voucher() {
        return Voucher.builder()
                .id(UUID.randomUUID()).code("SAVE20").discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20)).minOrder(BigDecimal.ZERO).usedCount(0)
                .isActive(true).validFrom(LocalDateTime.now().minusDays(1)).validTo(LocalDateTime.now().plusDays(1))
                .build();
    }
}
