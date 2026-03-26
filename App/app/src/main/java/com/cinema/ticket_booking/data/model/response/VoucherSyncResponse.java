package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class VoucherSyncResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("code")
    private String code;

    @SerializedName("description")
    private String description;

    @SerializedName("discountType")
    private String discountType;

    @SerializedName("discountValue")
    private BigDecimal discountValue;

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }
}
