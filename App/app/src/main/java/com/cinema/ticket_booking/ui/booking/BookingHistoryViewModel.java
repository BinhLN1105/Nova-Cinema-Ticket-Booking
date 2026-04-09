package com.cinema.ticket_booking.ui.booking;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.data.repository.BookingRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@HiltViewModel
public class BookingHistoryViewModel extends ViewModel {
    private final BookingRepository repo;
    private final MutableLiveData<Resource<PageResponse<BookingSummary>>> bookings = new MutableLiveData<>();
    private final List<BookingSummary> accumulatedBookings = new ArrayList<>();
    
    private boolean isUpcomingTab = true; 
    private int currentPage = 0;
    private final int pageSize = 15;
    private boolean isLastPage = false;
    private boolean isLoading = false;

    @Inject
    public BookingHistoryViewModel(BookingRepository repo) {
        this.repo = repo;
        loadPage(0);
    }

    public boolean isUpcomingTab() {
        return isUpcomingTab;
    }

    public void setUpcomingTab(boolean upcomingTab) {
        isUpcomingTab = upcomingTab;
    }

    public LiveData<Resource<PageResponse<BookingSummary>>> getBookings() {
        return bookings;
    }

    public void refresh() {
        if (isLoading) return;
        accumulatedBookings.clear();
        currentPage = 0;
        isLastPage = false;
        loadPage(currentPage);
    }

    public void loadMore() {
        if (isLoading || isLastPage) return;
        loadPage(currentPage + 1);
    }

    public boolean isLoadingMore() {
        return isLoading && currentPage > 0;
    }

    private void loadPage(int page) {
        isLoading = true;
        repo.getMyBookings(page, pageSize).observeForever(resource -> {
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                isLoading = false;
                currentPage = page;
                isLastPage = resource.data.isLast();
                if (page == 0) accumulatedBookings.clear();
                accumulatedBookings.addAll(resource.data.getContent());

                PageResponse<BookingSummary> newPageResp = new PageResponse<>();
                newPageResp.setContent(accumulatedBookings);
                newPageResp.setLast(isLastPage);
                newPageResp.setTotalElements(resource.data.getTotalElements());

                bookings.setValue(Resource.success(newPageResp));
            } else if (resource.status == Resource.Status.ERROR) {
                isLoading = false;
                bookings.setValue(resource);
            } else if (resource.status == Resource.Status.LOADING && page == 0) {
                bookings.setValue(Resource.loading());
            }
        });
    }
}
