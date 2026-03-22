package com.cinema.ticket_booking.ui.scanner;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.cinema.ticket_booking.data.model.response.CheckInResponse;
import com.cinema.ticket_booking.data.repository.BookingRepository;
import com.cinema.ticket_booking.data.repository.AuthRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ScannerViewModel extends ViewModel {

    private final BookingRepository bookingRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<Resource<CheckInResponse>> checkInResult = new MutableLiveData<>();

    @Inject
    public ScannerViewModel(BookingRepository bookingRepo, AuthRepository authRepo) {
        this.bookingRepo = bookingRepo;
        this.authRepo = authRepo;
    }

    public void logout() {
        authRepo.logout();
    }

    public LiveData<Resource<CheckInResponse>> getCheckInResult() {
        return checkInResult;
    }

    public void checkInTicket(String qrCode) {
        bookingRepo.checkIn(qrCode).observeForever(checkInResult::setValue);
    }
}
