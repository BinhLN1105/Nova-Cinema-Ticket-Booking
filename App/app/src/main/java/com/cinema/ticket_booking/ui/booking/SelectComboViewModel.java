package com.cinema.ticket_booking.ui.booking;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.ComboResponse;
import com.cinema.ticket_booking.data.repository.ComboRepository;
import com.cinema.ticket_booking.util.Resource;
import com.cinema.ticket_booking.data.repository.BookingRepository;
import com.cinema.ticket_booking.data.model.response.BookingResponse;
import com.cinema.ticket_booking.data.model.request.BookingRequest;

import java.util.*;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class SelectComboViewModel extends ViewModel {
    private final ComboRepository comboRepo;
    private final BookingRepository bookingRepo;
    private final MutableLiveData<Resource<List<ComboResponse>>> combos = new MutableLiveData<>();
    private final MutableLiveData<Double> localTotal = new MutableLiveData<>(0.0);
    private final Map<String, Integer> selectedCombos = new LinkedHashMap<>();
    private final Map<String, Double> comboPriceMap = new HashMap<>();

    public static final Map<String, Integer> pendingCombos = new HashMap<>();

    @Inject
    public SelectComboViewModel(ComboRepository comboRepo, BookingRepository bookingRepo) {
        this.comboRepo = comboRepo;
        this.bookingRepo = bookingRepo;
        loadCombos();
    }

    public LiveData<Resource<List<ComboResponse>>> getCombos() {
        return combos;
    }

    public LiveData<Double> getLocalTotal() {
        return localTotal;
    }

    public Map<String, Integer> getSelectedCombos() {
        return selectedCombos;
    }

    private void loadCombos() {
        comboRepo.getCombos().observeForever(resource -> {
            combos.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                for (ComboResponse combo : resource.data) {
                    comboPriceMap.put(combo.getId(), combo.getPrice());
                }
                updateLocalTotal();
            }
        });
    }

    public void addCombo(String comboId) {
        selectedCombos.merge(comboId, 1, Integer::sum);
        pendingCombos.clear();
        pendingCombos.putAll(selectedCombos);
        updateLocalTotal();
    }

    public void removeCombo(String comboId) {
        if (selectedCombos.containsKey(comboId)) {
            int count = selectedCombos.get(comboId);
            if (count > 1) {
                selectedCombos.put(comboId, count - 1);
            } else {
                selectedCombos.remove(comboId);
            }
        }
        pendingCombos.clear();
        pendingCombos.putAll(selectedCombos);
        updateLocalTotal();
    }

    private void updateLocalTotal() {
        double total = SelectSeatViewModel.pendingTotalAmount;
        for (Map.Entry<String, Integer> entry : selectedCombos.entrySet()) {
            Double price = comboPriceMap.get(entry.getKey());
            if (price != null) {
                total += price * entry.getValue();
            }
        }
        localTotal.setValue(total);
    }

    public LiveData<Resource<BookingResponse>> getFinalQuote() {
        String showtimeId = SelectShowtimeViewModel.pendingShowtimeId;
        if (showtimeId == null) {
            MutableLiveData<Resource<BookingResponse>> error = new MutableLiveData<>();
            error.setValue(Resource.error("Lỗi: Không tìm thấy thông tin suất chiếu", null));
            return error;
        }

        List<BookingRequest.ComboItem> comboItems = new ArrayList<>();
        for (Map.Entry<String, Integer> e : selectedCombos.entrySet()) {
            if (e.getValue() > 0) {
                comboItems.add(new BookingRequest.ComboItem(e.getKey(), e.getValue()));
            }
        }

        BookingRequest req = new BookingRequest(showtimeId, SelectSeatViewModel.pendingSeatIds, comboItems, null);
        return bookingRepo.getBookingQuote(req);
    }
}

