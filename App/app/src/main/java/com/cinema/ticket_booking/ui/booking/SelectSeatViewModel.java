package com.cinema.ticket_booking.ui.booking;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.data.repository.*;
import com.cinema.ticket_booking.util.Resource;
import java.util.*;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class SelectSeatViewModel extends ViewModel {
    private final ShowtimeRepository showtimeRepo;
    private final BookingRepository bookingRepo;

    private final MutableLiveData<Resource<SeatMapResponse>> seatMap = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<ComboResponse>>> combos = new MutableLiveData<>();
    private final Set<String> selectedSeatIds = new LinkedHashSet<>();
    private final Map<String, Integer> selectedCombos = new LinkedHashMap<>();

    // Shared booking state
    public static List<String> pendingSeatIds = new ArrayList<>();
    public static Map<String, Integer> pendingCombos = new LinkedHashMap<>();
    public static double pendingTotalAmount;

    @Inject
    public SelectSeatViewModel(ShowtimeRepository showtimeRepo, BookingRepository bookingRepo) {
        this.showtimeRepo = showtimeRepo;
        this.bookingRepo = bookingRepo;
    }

    public LiveData<Resource<SeatMapResponse>> getSeatMap() { return seatMap; }
    public Set<String> getSelectedSeatIds() { return selectedSeatIds; }
    public Map<String, Integer> getSelectedCombos() { return selectedCombos; }

    public void loadSeatMap(String showtimeId) {
        showtimeRepo.getSeatMap(showtimeId).observeForever(seatMap::setValue);
    }

    public boolean toggleSeat(SeatMapResponse.SeatItem seat) {
        if ("BOOKED".equals(seat.getStatus()) || "LOCKED".equals(seat.getStatus())) return false;
        if (selectedSeatIds.contains(seat.getShowtimeSeatId()))
            selectedSeatIds.remove(seat.getShowtimeSeatId());
        else
            selectedSeatIds.add(seat.getShowtimeSeatId());
        return true;
    }

    public double calculateTotal(SeatMapResponse seatMapData) {
        if (seatMapData == null) return 0;
        double total = 0;
        for (SeatMapResponse.SeatItem seat : seatMapData.getSeats()) {
            if (selectedSeatIds.contains(seat.getShowtimeSeatId()))
                total += seat.getPrice();
        }
        return total;
    }
}
