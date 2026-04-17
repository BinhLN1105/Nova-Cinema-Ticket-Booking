package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class VoucherSummary {
    @SerializedName("code")
    private String code;
    @SerializedName("description")
    private String description;
    @SerializedName("discountType")
    private String discountType;
    @SerializedName("discountValue")
    private double discountValue;
    @SerializedName("maxDiscount")
    private double maxDiscount;
    @SerializedName("minOrder")
    private double minOrder;
    @SerializedName("endDate")
    private String endDate;
    @SerializedName("status")
    private String status;

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public double getMinOrder() {
        return minOrder;
    }

    public double getMaxDiscount() {
        return maxDiscount;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }
}
