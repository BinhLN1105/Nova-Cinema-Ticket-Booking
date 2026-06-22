package com.cinema.ticket_booking.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyUtilTest {

    @Test
    void testFormatVnd() {
        assertEquals("0 ₫", CurrencyUtil.formatVnd(null));

        BigDecimal amount = new BigDecimal("150000");
        String formatted = CurrencyUtil.formatVnd(amount);
        assertNotNull(formatted);
        assertTrue(formatted.contains("150") && formatted.contains("000"));
    }

    @Test
    void testFormatShort() {
        assertEquals("0đ", CurrencyUtil.formatShort(null));

        BigDecimal amount = new BigDecimal("150000");
        String formatted = CurrencyUtil.formatShort(amount);
        assertNotNull(formatted);
        assertTrue(formatted.contains("150") && formatted.contains("000") && formatted.endsWith("đ"));
    }

    @Test
    void testRoundToThousand() {
        assertEquals(BigDecimal.ZERO, CurrencyUtil.roundToThousand(null));

        BigDecimal amount1 = new BigDecimal("149500");
        BigDecimal expected1 = new BigDecimal("150000");
        assertEquals(0, expected1.compareTo(CurrencyUtil.roundToThousand(amount1)));

        BigDecimal amount2 = new BigDecimal("150000");
        BigDecimal expected2 = new BigDecimal("150000");
        assertEquals(0, expected2.compareTo(CurrencyUtil.roundToThousand(amount2)));

        BigDecimal amount3 = new BigDecimal("150100");
        BigDecimal expected3 = new BigDecimal("151000");
        assertEquals(0, expected3.compareTo(CurrencyUtil.roundToThousand(amount3)));
    }
}
