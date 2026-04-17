package com.cinema.ticket_booking.data.model.request;

import com.google.gson.annotations.SerializedName;

public class ClaimVoucherRequest {
    @SerializedName("code")
    private String code;

    public ClaimVoucherRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
