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
    private final MutableLiveData<Resource<BookingResponse>> quoteResult = new MutableLiveData<>();

    private VoucherSummary appliedVoucher;
    private final android.os.Handler debounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable debounceRunnable;

    @Inject
    public ConfirmBookingViewModel(BookingRepository bookingRepo, VoucherRepository voucherRepo,
                                   ComboRepository comboRepo) {
        this.bookingRepo = bookingRepo;
        this.voucherRepo = voucherRepo;
        this.comboRepo = comboRepo;
        loadCombos();
    }

    public LiveData<Resource<List<ComboResponse>>> getCombos() {
        return combos;
    }

    public LiveData<Resource<VoucherSummary>> getVoucher() {
        return voucher;
    }

    public LiveData<Resource<BookingResponse>> getBookingResult() {
        return bookingResult;
    }

    public void resetBookingResult() {
        bookingResult.setValue(null);
    }

    public LiveData<Resource<BookingResponse>> getQuoteResult() {
        return quoteResult;
    }

    public void setInitialQuote(BookingResponse quote) {
        if (quote != null) {
            quoteResult.setValue(Resource.success(quote));
            if (quote.getAppliedPromotionName() != null) {
                // Nếu có promo/voucher, cập nhật local state để đồng bộ
                // Giả lập voucher summary nếu cần, hoặc chỉ để quote là đủ
            }
        }
    }

    private void loadCombos() {
        comboRepo.getCombos().observeForever(combos::setValue);
    }

    public void validateVoucher(String code) {
        voucherRepo.validateVoucher(code).observeForever(r -> {
            voucher.setValue(r);
            if (r.isSuccess()) {
                appliedVoucher = r.data;
                refreshQuote();
            }
        });
    }

    public void clearVoucher() {
        appliedVoucher = null;
        voucher.setValue(null);
        refreshQuote();
    }

    public void onComboChanged() {
        refreshQuote();
    }

    public void refreshQuote() {
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }

        debounceRunnable = () -> {
            String showtimeId = SelectShowtimeViewModel.pendingShowtimeId;
            if (showtimeId == null) return;

            List<BookingRequest.ComboItem> comboItems = new ArrayList<>();
            for (Map.Entry<String, Integer> e : SelectComboViewModel.pendingCombos.entrySet()) {
                if (e.getValue() > 0) {
                    comboItems.add(new BookingRequest.ComboItem(e.getKey(), e.getValue()));
                }
            }

            String voucherCode = appliedVoucher != null ? appliedVoucher.getCode() : null;
            BookingRequest req = new BookingRequest(showtimeId, SelectSeatViewModel.pendingSeatIds, comboItems,
                    voucherCode);

            bookingRepo.getBookingQuote(req).observeForever(quoteResult::postValue);
        };

        debounceHandler.postDelayed(debounceRunnable, 500);
    }

    public void confirmBooking() {
        String showtimeId = SelectShowtimeViewModel.pendingShowtimeId;
        if (showtimeId == null) return;

        List<BookingRequest.ComboItem> comboItems = new ArrayList<>();
        for (Map.Entry<String, Integer> e : SelectComboViewModel.pendingCombos.entrySet()) {
            if (e.getValue() > 0) {
                comboItems.add(new BookingRequest.ComboItem(e.getKey(), e.getValue()));
            }
        }

        String voucherCode = appliedVoucher != null ? appliedVoucher.getCode() : null;
        BookingRequest req = new BookingRequest(showtimeId, SelectSeatViewModel.pendingSeatIds, comboItems,
                voucherCode);

        bookingRepo.createBooking(req).observeForever(bookingResult::setValue);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
    }
}

