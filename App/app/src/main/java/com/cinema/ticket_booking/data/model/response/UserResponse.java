package com.cinema.ticket_booking.data.model.response;
import com.google.gson.annotations.SerializedName;
public class UserResponse {
    @SerializedName("id")        private String id;
    @SerializedName("email")     private String email;
    @SerializedName("fullName")  private String fullName;
    @SerializedName("phone")     private String phone;
    @SerializedName("avatarUrl") private String avatarUrl;
    @SerializedName("role")      private String role;
    public String getId()        { return id; }
    public String getEmail()     { return email; }
    public String getFullName()  { return fullName; }
    public String getPhone()     { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getRole()      { return role; }
}
