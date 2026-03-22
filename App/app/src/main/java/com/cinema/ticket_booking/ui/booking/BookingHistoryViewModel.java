package com.cinema.ticket_booking.ui.booking;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.data.repository.BookingRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class BookingHistoryViewModel extends ViewModel {
    private final BookingRepository repo;
    private final MutableLiveData<Resource<PageResponse<BookingSummary>>> bookings = new MutableLiveData<>();

    @Inject
    public BookingHistoryViewModel(BookingRepository repo) {
        this.repo = repo;
        load();
    }

    public LiveData<Resource<PageResponse<BookingSummary>>> getBookings() {
        return bookings;
    }

    public void refresh() {
        load();
    }

    private void load() {
        repo.getMyBookings(0, 20).observeForever(bookings::setValue);
    }
}
