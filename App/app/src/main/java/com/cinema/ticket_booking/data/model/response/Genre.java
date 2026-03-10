package com.cinema.ticket_booking.data.model.response;
import com.google.gson.annotations.SerializedName;
public class Genre {
    @SerializedName("id")   private int id;
    @SerializedName("name") private String name;
    @SerializedName("slug") private String slug;
    public int getId()      { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
}
