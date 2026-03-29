package com.cinema.ticket_booking.dto;

import java.math.BigDecimal;

/**
 * Record gọn nhẹ để đóng gói kết quả tính toán giá vé từ PricingEngine.
 * Chuẩn SOLID: Dễ đọc, bất biến và đầy đủ thông tin.
 */
public record PricingResult(
    BigDecimal finalPrice,           // Giá cuối sau khi áp dụng các quy tắc
    BigDecimal discountAmount,       // Số tiền được giảm (riêng phần Promotion)
    String appliedPromotionName      // Tên chương trình khuyến mãi đã áp dụng
) {}
