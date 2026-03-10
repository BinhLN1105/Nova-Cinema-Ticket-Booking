package com.cinema.ticket_booking.ui.booking;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.request.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.data.repository.*;
import com.cinema.ticket_booking.util.Resource;
import java.util.*;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ConfirmBookingViewModel extends ViewModel {
    private final BookingRepository bookingRepo;
    private final VoucherRepository voucherRepo;
    private final ComboRepository comboRepo;

    private final MutableLiveData<Resource<List<ComboResponse>>> combos = new MutableLiveData<>();
    private final MutableLiveData<Resource<VoucherSummary>> voucher = new MutableLiveData<>();
    private final MutableLiveData<Resource<BookingResponse>> bookingResult = new MutableLiveData<>();

    private VoucherSummary appliedVoucher;
    private final Map<String, Integer> selectedCombos = new LinkedHashMap<>();

    @Inject
    public ConfirmBookingViewModel(BookingRepository bookingRepo, VoucherRepository voucherRepo, ComboRepository comboRepo) {
        this.bookingRepo = bookingRepo;
        this.voucherRepo = voucherRepo;
        this.comboRepo = comboRepo;
        loadCombos();
    }

    public LiveData<Resource<List<ComboResponse>>> getCombos() { return combos; }
    public LiveData<Resource<VoucherSummary>> getVoucher() { return voucher; }
    public LiveData<Resource<BookingResponse>> getBookingResult() { return bookingResult; }
    public Map<String, Integer> getSelectedCombos() { return selectedCombos; }

    private void loadCombos() {
        comboRepo.getCombos().observeForever(combos::setValue);
    }

    public void validateVoucher(String code) {
        voucherRepo.validateVoucher(code).observeForever(r -> {
            voucher.setValue(r);
            if (r.isSuccess()) appliedVoucher = r.data;
        });
    }

    public void clearVoucher() { appliedVoucher = null; voucher.setValue(null); }

    public void addCombo(String comboId) {
        selectedCombos.merge(comboId, 1, Integer::sum);
    }

    public void removeCombo(String comboId) {
        selectedCombos.computeIfPresent(comboId, (k, v) -> v > 1 ? v - 1 : null);
    }

    public double calculateDiscount(double subtotal) {
        if (appliedVoucher == null) return 0;
        if ("PERCENTAGE".equals(appliedVoucher.getDiscountType()))
            return Math.min(subtotal * appliedVoucher.getDiscountValue() / 100, appliedVoucher.getMinOrder());
        return appliedVoucher.getDiscountValue();
    }

    public void confirmBooking(String showtimeId) {
        List<BookingRequest.ComboItem> comboItems = new ArrayList<>();
        for (Map.Entry<String, Integer> e : selectedCombos.entrySet())
            comboItems.add(new BookingRequest.ComboItem(e.getKey(), e.getValue()));

        String voucherCode = appliedVoucher != null ? appliedVoucher.getCode() : null;
        BookingRequest req = new BookingRequest(showtimeId, SelectSeatViewModel.pendingSeatIds, comboItems, voucherCode);
        bookingRepo.createBooking(req).observeForever(bookingResult::setValue);
    }
}
