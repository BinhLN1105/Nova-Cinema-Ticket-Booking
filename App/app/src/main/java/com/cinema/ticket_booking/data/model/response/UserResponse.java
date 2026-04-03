package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("email")
    private String email;
    @SerializedName("fullName")
    private String fullName;
    @SerializedName("phone")
    private String phone;
    @SerializedName("avatarUrl")
    private String avatarUrl;
    @SerializedName("role")
    private String role;
    @SerializedName("rewardPoints")
    private int cinePoints;
    @SerializedName("cinemaId")
    private String cinemaId;
    @SerializedName("cinemaName")
    private String cinemaName;

    @SerializedName("currentTierMinPoints")
    private Long currentTierMinPoints;
    @SerializedName("nextTierMinPoints")
    private Long nextTierMinPoints;
    @SerializedName("membershipTier")
    private String rank;

    @SerializedName("availableExp")
    private long availableExp;

    public long getAvailableExp() {
        return availableExp;
    }

    public String getRank() {
        return rank;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public int getCinePoints() {
        return cinePoints;
    }

    public String getCinemaId() {
        return cinemaId;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public Long getCurrentTierMinPoints() {
        return currentTierMinPoints;
    }

    public Long getNextTierMinPoints() {
        return nextTierMinPoints;
    }
}
