package com.cinema.ticket_booking.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Tạo Pageable chuẩn hoá, tránh client gửi page/size tuỳ tiện gây lỗi.
 */
public final class PageableUtil {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private PageableUtil() {
    }

    public static Pageable of(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_SIZE);
        return PageRequest.of(safePage, safeSize);
    }

    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }

    public static Pageable ofDefault() {
        return PageRequest.of(0, DEFAULT_SIZE, Sort.by("createdAt").descending());
    }

    /** Dùng cho sort theo nhiều field từ query param: "createdAt,desc" */
    public static Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by("createdAt").descending();
        }
        String[] parts = sortParam.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(dir, parts[0].trim());
    }
}
