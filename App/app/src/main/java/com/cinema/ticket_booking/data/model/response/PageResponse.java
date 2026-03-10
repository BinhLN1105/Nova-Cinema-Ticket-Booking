package com.cinema.ticket_booking.data.model.response;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class PageResponse<T> {
    @SerializedName("content")       private List<T> content;
    @SerializedName("page")          private int page;
    @SerializedName("size")          private int size;
    @SerializedName("totalElements") private long totalElements;
    @SerializedName("totalPages")    private int totalPages;
    @SerializedName("last")          private boolean last;
    public List<T> getContent()      { return content; }
    public int getTotalPages()       { return totalPages; }
    public boolean isLast()          { return last; }
    public long getTotalElements()   { return totalElements; }
}
