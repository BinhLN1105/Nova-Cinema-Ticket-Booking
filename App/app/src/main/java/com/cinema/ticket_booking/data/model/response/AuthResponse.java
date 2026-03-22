package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("accessToken")
    private String accessToken;
    @SerializedName("refreshToken")
    private String refreshToken;
    @SerializedName("tokenType")
    private String tokenType;
    @SerializedName("user")
    private UserInfo user;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public UserInfo getUser() {
        return user;
    }

    public static class UserInfo {
        @SerializedName("id")
        private String id;
        @SerializedName("email")
        private String email;
        @SerializedName("fullName")
        private String fullName;
        @SerializedName("avatarUrl")
        private String avatarUrl;
        @SerializedName("role")
        private String role;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getFullName() {
            return fullName;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public String getRole() {
            return role;
        }
    }
}
