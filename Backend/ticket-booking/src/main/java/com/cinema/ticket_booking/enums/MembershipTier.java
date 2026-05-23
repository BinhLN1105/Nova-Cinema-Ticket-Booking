package com.cinema.ticket_booking.enums;

import java.math.BigDecimal;

public enum MembershipTier {
    BRONZE(0, 0, BigDecimal.ZERO, 0L),
    SILVER(5, 2, BigDecimal.valueOf(30000.0), 500L),
    GOLD(10, 4, BigDecimal.valueOf(50000.0), 3000L),
    DIAMOND(15, 6, BigDecimal.valueOf(100000.0), 10000L);

    private final int discountPercent;
    private final int maxUsage;
    private final BigDecimal maxCap;
    private final long requiredExp;

    MembershipTier(int discountPercent, int maxUsage, BigDecimal maxCap, long requiredExp) {
        this.discountPercent = discountPercent;
        this.maxUsage = maxUsage;
        this.maxCap = maxCap;
        this.requiredExp = requiredExp;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public int getMaxUsage() {
        return maxUsage;
    }

    public BigDecimal getMaxCap() {
        return maxCap;
    }

    public long getRequiredExp() {
        return requiredExp;
    }
}
