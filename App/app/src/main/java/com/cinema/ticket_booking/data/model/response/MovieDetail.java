package com.cinema.ticket_booking.data.model.response;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class MovieDetail {
    @SerializedName("id")           private String id;
    @SerializedName("title")        private String title;
    @SerializedName("description")  private String description;
    @SerializedName("duration")     private int duration;
    @SerializedName("releaseDate")  private String releaseDate;
    @SerializedName("director")     private String director;
    @SerializedName("cast")         private String cast;
    @SerializedName("rated")        private String rated;
    @SerializedName("posterUrl")    private String posterUrl;
    @SerializedName("trailerUrl")   private String trailerUrl;
    @SerializedName("avgRating")    private double avgRating;
    @SerializedName("status")       private String status;
    @SerializedName("genres")       private List<Genre> genres;
    public String getId()           { return id; }
    public String getTitle()        { return title; }
    public String getDescription()  { return description; }
    public int getDuration()        { return duration; }
    public String getReleaseDate()  { return releaseDate; }
    public String getDirector()     { return director; }
    public String getCast()         { return cast; }
    public String getRated()        { return rated; }
    public String getPosterUrl()    { return posterUrl; }
    public String getTrailerUrl()   { return trailerUrl; }
    public double getAvgRating()    { return avgRating; }
    public String getStatus()       { return status; }
    public List<Genre> getGenres()  { return genres; }
}
