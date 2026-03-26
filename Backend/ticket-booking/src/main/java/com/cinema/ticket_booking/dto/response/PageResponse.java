package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

import java.io.Serializable;

/**
 * Wrapper chuẩn hoá response phân trang cho tất cả API.
 * Dùng: PageResponse.of(page) thay vì trả thẳng Page<T> của Spring.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> content;
    private int page; // Trang hiện tại (bắt đầu từ 0)
    private int size; // Số phần tử mỗi trang
    private long totalElements; // Tổng số phần tử
    private int totalPages; // Tổng số trang
    private boolean last; // Có phải trang cuối không

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
