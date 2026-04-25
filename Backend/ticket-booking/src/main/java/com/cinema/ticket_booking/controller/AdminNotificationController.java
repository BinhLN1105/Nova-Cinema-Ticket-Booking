package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.dto.request.NotificationCampaignRequest;
import com.cinema.ticket_booking.dto.response.ApiResponse;
import com.cinema.ticket_booking.dto.response.NotificationCampaignResponse;
import com.cinema.ticket_booking.model.NotificationCampaign;
import com.cinema.ticket_booking.model.User;
import com.cinema.ticket_booking.enums.CampaignStatus;
import com.cinema.ticket_booking.service.NotificationService;
import com.cinema.ticket_booking.repository.NotificationCampaignRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final NotificationCampaignRepository campaignRepository;

    /**
     * Tạo hoặc thực hiện một chiến dịch thông báo.
     * Nếu scheduledAt là null -> Thực hiện gửi ngay lập tức (Broadcast).
     * Nếu scheduledAt có giá trị -> Lưu vào trạng thái PENDING để Scheduler xử lý.
     */
    @PostMapping("/campaign")
    public ResponseEntity<ApiResponse<NotificationCampaignResponse>> createCampaign(
            @Valid @RequestBody NotificationCampaignRequest request,
            @AuthenticationPrincipal User currentUser) {

        NotificationCampaign campaign;

        if (request.getScheduledAt() == null
                || request.getScheduledAt().isBefore(java.time.LocalDateTime.now().plusSeconds(30))) {
            // Trường hợp: Gửi ngay lập tức
            notificationService.broadcastGlobalNotification(
                    request.getTitle(),
                    request.getBody(),
                    request.getType(),
                    request.getTargetId(),
                    request.getSegment());

            // Lưu lịch sử gửi với trạng thái SENT
            campaign = notificationService.createCampaign(
                    request.getTitle(),
                    request.getBody(),
                    request.getType(),
                    request.getTargetId(),
                    request.getSegment(),
                    LocalDateTime.now(),
                    currentUser);
            campaign.setStatus(CampaignStatus.SENT);
            campaignRepository.save(campaign);
        } else {
            // Trường hợp: Hẹn giờ gửi trong tương lai
            campaign = notificationService.createCampaign(
                    request.getTitle(),
                    request.getBody(),
                    request.getType(),
                    request.getTargetId(),
                    request.getSegment(),
                    request.getScheduledAt(),
                    currentUser);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(campaign), "Đã xử lý chiến dịch thông báo"));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<ApiResponse<List<NotificationCampaignResponse>>> getCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var campaigns = campaignRepository.findAllWithUser(PageRequest.of(page, size));
        var responseList = campaigns.stream().map(this::toResponse).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responseList));
    }

    private NotificationCampaignResponse toResponse(NotificationCampaign campaign) {
        return NotificationCampaignResponse.builder()
                .id(campaign.getId().toString())
                .title(campaign.getTitle())
                .body(campaign.getBody())
                .type(campaign.getType())
                .targetId(campaign.getTargetId() != null ? campaign.getTargetId().toString() : null)
                .targetTopic(campaign.getTargetTopic())
                .scheduledAt(campaign.getScheduledAt())
                .status(campaign.getStatus())
                .createdAt(campaign.getCreatedAt())
                .createdByFullName(campaign.getCreatedBy() != null ? campaign.getCreatedBy().getFullName() : "System")
                .build();
    }
}
