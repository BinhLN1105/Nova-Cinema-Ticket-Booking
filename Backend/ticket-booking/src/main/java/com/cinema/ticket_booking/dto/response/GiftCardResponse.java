package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String code;
    private BigDecimal price;
    private Long pointValue;
    private Boolean isRedeemed;
    private LocalDateTime redeemedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
