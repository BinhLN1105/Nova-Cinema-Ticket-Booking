package com.cinema.ticket_booking.data.model.response;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class MovieSummary {
    @SerializedName("id")        private String id;
    @SerializedName("title")     private String title;
    @SerializedName("posterUrl") private String posterUrl;
    @SerializedName("duration")  private int duration;
    @SerializedName("rated")     private String rated;
    @SerializedName("avgRating") private double avgRating;
    @SerializedName("status")    private String status;
    @SerializedName("genres")    private List<Genre> genres;
    public String getId()        { return id; }
    public String getTitle()     { return title; }
    public String getPosterUrl() { return posterUrl; }
    public int getDuration()     { return duration; }
    public String getRated()     { return rated; }
    public double getAvgRating() { return avgRating; }
    public String getStatus()    { return status; }
    public List<Genre> getGenres() { return genres; }
}
