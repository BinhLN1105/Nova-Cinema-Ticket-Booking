package com.cinema.ticket_booking.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlugUtilTest {

    @Test
    void testToSlug_NullOrBlank() {
        assertEquals("", SlugUtil.toSlug(null));
        assertEquals("", SlugUtil.toSlug(""));
        assertEquals("", SlugUtil.toSlug("   "));
    }

    @Test
    void testToSlug_ValidVietnamese() {
        assertEquals("hanh-ong", SlugUtil.toSlug("Hành Động"));
        assertEquals("phim-sieu-nhan-e2e", SlugUtil.toSlug("Phim Siêu Nhân E2E"));
    }

    @Test
    void testToSlug_SpecialCharacters() {
        assertEquals("phim-hot-2026", SlugUtil.toSlug("Phim Hot 2026!!!"));
        assertEquals("giam-gia-soc", SlugUtil.toSlug("---Giảm Giá Sốc---"));
    }
}
