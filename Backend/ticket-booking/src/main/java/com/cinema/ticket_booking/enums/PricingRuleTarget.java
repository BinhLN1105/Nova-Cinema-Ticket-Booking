package com.cinema.ticket_booking.enums;

public enum PricingRuleTarget {
    TICKET,         // Áp dụng cho giá vé (từng ghế)
    COMBO,          // Áp dụng cho giá bắp nước (từng combo)
    ORDER_TOTAL     // Áp dụng cho tổng đơn hàng (sau khi đã tính vé & combo)
}
