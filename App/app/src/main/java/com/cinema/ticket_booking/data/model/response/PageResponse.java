package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PageResponse<T> {
    @SerializedName("content")
    private List<T> content;
    @SerializedName("page")
    private int page;
    @SerializedName("size")
    private int size;
    @SerializedName("totalElements")
    private long totalElements;
    @SerializedName("totalPages")
    private int totalPages;
    @SerializedName("last")
    private boolean last;

    public List<T> getContent() {
        return content;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isLast() {
        return last;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
