package com.cinema.ticket_booking.ui.booking;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.BookingResponse;
import com.cinema.ticket_booking.data.repository.BookingRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class BookingDetailViewModel extends ViewModel {
    private final BookingRepository repo;
    private final MutableLiveData<Resource<BookingResponse>> booking = new MutableLiveData<>();
    @Inject public BookingDetailViewModel(BookingRepository repo) { this.repo = repo; }
    public LiveData<Resource<BookingResponse>> getBooking() { return booking; }
    public void loadBooking(String id) { repo.getBookingDetail(id).observeForever(booking::setValue); }
}
