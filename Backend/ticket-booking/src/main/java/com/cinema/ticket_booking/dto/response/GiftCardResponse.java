package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class GiftCardResponse {
    private String id;
    private String code;
    private BigDecimal price;
    private Long pointValue;
    private Boolean isRedeemed;
    private LocalDateTime redeemedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
