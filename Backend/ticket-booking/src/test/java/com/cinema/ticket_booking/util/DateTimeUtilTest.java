package com.cinema.ticket_booking.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilTest {

    @Test
    void testFormatDate() {
        assertEquals("", DateTimeUtil.formatDate(null));
        LocalDate date = LocalDate.of(2024, 12, 25);
        assertEquals("25/12/2024", DateTimeUtil.formatDate(date));
    }

    @Test
    void testFormatTime() {
        assertEquals("", DateTimeUtil.formatTime(null));
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 25, 20, 30);
        assertEquals("20:30", DateTimeUtil.formatTime(dateTime));
    }

    @Test
    void testFormatDateTime() {
        assertEquals("", DateTimeUtil.formatDateTime(null));
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 25, 20, 30);
        assertEquals("20:30 - 25/12/2024", DateTimeUtil.formatDateTime(dateTime));
    }

    @Test
    void testToVnpayFormat() {
        assertEquals("", DateTimeUtil.toVnpayFormat(null));
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 25, 20, 30, 15);
        assertEquals("20241225203015", DateTimeUtil.toVnpayFormat(dateTime));
    }

    @Test
    void testFormatDateWithDayOfWeek() {
        assertEquals("", DateTimeUtil.formatDateWithDayOfWeek(null));
        LocalDate date = LocalDate.of(2024, 12, 25); // Thứ Tư
        String formatted = DateTimeUtil.formatDateWithDayOfWeek(date);
        assertTrue(formatted.contains("Thứ Tư") || formatted.contains("Thứ tư"));
        assertTrue(formatted.contains("25/12/2024"));
    }

    @Test
    void testIsWeekend() {
        LocalDateTime saturday = LocalDateTime.of(2024, 12, 28, 12, 0); // Saturday
        LocalDateTime sunday = LocalDateTime.of(2024, 12, 29, 12, 0); // Sunday
        LocalDateTime monday = LocalDateTime.of(2024, 12, 30, 12, 0); // Monday

        assertTrue(DateTimeUtil.isWeekend(saturday));
        assertTrue(DateTimeUtil.isWeekend(sunday));
        assertFalse(DateTimeUtil.isWeekend(monday));
    }

    @Test
    void testIsPeakHour() {
        LocalDateTime peak18 = LocalDateTime.of(2024, 12, 25, 18, 0);
        LocalDateTime peak21 = LocalDateTime.of(2024, 12, 25, 21, 59);
        LocalDateTime nonPeak17 = LocalDateTime.of(2024, 12, 25, 17, 59);
        LocalDateTime nonPeak22 = LocalDateTime.of(2024, 12, 25, 22, 0);

        assertTrue(DateTimeUtil.isPeakHour(peak18));
        assertTrue(DateTimeUtil.isPeakHour(peak21));
        assertFalse(DateTimeUtil.isPeakHour(nonPeak17));
        assertFalse(DateTimeUtil.isPeakHour(nonPeak22));
    }

    @Test
    void testMinutesUntil() {
        assertEquals(0, DateTimeUtil.minutesUntil(null));

        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDateTime nowInZone = LocalDateTime.now(zoneId);
        LocalDateTime future = nowInZone.plusMinutes(10);

        long diff = DateTimeUtil.minutesUntil(future);
        // diff should be close to 10
        assertTrue(diff >= 9 && diff <= 10);
    }
}
