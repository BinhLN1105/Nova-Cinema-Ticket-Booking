package com.cinema.ticket_booking.data.local.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "movies")
public class MovieEntity {
    @PrimaryKey @NonNull
    private String id;
    private String title, posterUrl, status, rated;
    private int duration;
    private double avgRating;

    public MovieEntity(@NonNull String id, String title, String posterUrl,
                       String status, String rated, int duration, double avgRating) {
        this.id = id; this.title = title; this.posterUrl = posterUrl;
        this.status = status; this.rated = rated;
        this.duration = duration; this.avgRating = avgRating;
    }
    @NonNull public String getId()       { return id; }
    public String getTitle()             { return title; }
    public String getPosterUrl()         { return posterUrl; }
    public String getStatus()            { return status; }
    public String getRated()             { return rated; }
    public int getDuration()             { return duration; }
    public double getAvgRating()         { return avgRating; }
}
