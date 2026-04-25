package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class CinemaResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("address")
    private String address;
    @SerializedName("city")
    private String city;
    @SerializedName("imageUrl")
    private String imageUrl;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
