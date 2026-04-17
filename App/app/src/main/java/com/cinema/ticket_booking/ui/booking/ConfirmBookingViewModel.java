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
    private final UserRepository userRepo;

    private final MutableLiveData<Resource<List<ComboResponse>>> combos = new MutableLiveData<>();
    private final MutableLiveData<Resource<VoucherSummary>> voucher = new MutableLiveData<>();
    private final MutableLiveData<Resource<BookingResponse>> bookingResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<BookingResponse>> quoteResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<UserResponse>> userProfile = new MutableLiveData<>();
    private final MutableLiveData<Resource<PaymentResponse>> walletPaymentResult = new MutableLiveData<>();

    private VoucherSummary appliedVoucher;
    private List<VoucherSummary> myVouchers = new ArrayList<>();
    
    private final android.os.Handler debounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable debounceRunnable;

    @Inject
    public ConfirmBookingViewModel(BookingRepository bookingRepo, VoucherRepository voucherRepo,
                                   ComboRepository comboRepo, UserRepository userRepo) {
        this.bookingRepo = bookingRepo;
        this.voucherRepo = voucherRepo;
        this.comboRepo = comboRepo;
        this.userRepo = userRepo;
        loadCombos();
        loadMyVouchers();
        loadUserProfile();
    }

    public LiveData<Resource<UserResponse>> getUserProfile() {
        return userProfile;
    }

    public LiveData<Resource<PaymentResponse>> getWalletPaymentResult() {
        return walletPaymentResult;
    }

    private void loadUserProfile() {
        userRepo.getMyProfile().observeForever(userProfile::setValue);
    }

    public LiveData<Resource<List<ComboResponse>>> getCombos() {
        return combos;
    }

    public LiveData<Resource<VoucherSummary>> getVoucher() {
        return voucher;
    }

    public List<VoucherSummary> getMyVouchers() {
        return myVouchers;
    }

    public VoucherSummary getAppliedVoucher() {
        return appliedVoucher;
    }

    public LiveData<Resource<BookingResponse>> getBookingResult() {
        return bookingResult;
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

    private void loadMyVouchers() {
        voucherRepo.getMyVouchers().observeForever(r -> {
            if (r.isSuccess() && r.data != null) {
                myVouchers = r.data;
                autoApplyBestVoucher();
            }
        });
    }

    private void autoApplyBestVoucher() {
        if (appliedVoucher != null || quoteResult.getValue() == null || quoteResult.getValue().data == null) return;
        
        Double subtotal = quoteResult.getValue().data.getSubtotal() != null ? quoteResult.getValue().data.getSubtotal() : 0.0;
        if (subtotal <= 0) return;

        double maxDiscount = 0;
        VoucherSummary best = null;

        for (VoucherSummary v : myVouchers) {
            if (!"AVAILABLE".equals(v.getStatus())) continue;
            if (v.getMinOrder() > 0 && subtotal < v.getMinOrder()) continue;
            
            double discount = 0;
            if ("PERCENTAGE".equals(v.getDiscountType())) {
                discount = (subtotal * v.getDiscountValue()) / 100.0;
            } else {
                discount = v.getDiscountValue();
            }

            if (v.getMaxDiscount() > 0 && discount > v.getMaxDiscount()) {
                discount = v.getMaxDiscount();
            }

            if (discount > maxDiscount) {
                maxDiscount = discount;
                best = v;
            }
        }

        if (best != null) {
            appliedVoucher = best;
            refreshQuote();
        }
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

    public void applyVoucherDirectly(VoucherSummary v) {
        this.appliedVoucher = v;
        refreshQuote();
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

            bookingRepo.getBookingQuote(req).observeForever(r -> {
                quoteResult.postValue(r);
                if (r.isSuccess()) {
                    autoApplyBestVoucher();
                }
            });
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

    public void confirmBookingAndPayWithWallet() {
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

        // Chu trình: Create Booking -> Pay with Wallet
        bookingResult.setValue(Resource.loading());
        bookingRepo.createBooking(req).observeForever(res -> {
            if (res.isSuccess() && res.data != null) {
                // Booking created, now pay with wallet
                bookingRepo.payWithWallet(res.data.getId()).observeForever(walletPaymentResult::setValue);
            } else if (res.isError()) {
                bookingResult.setValue(Resource.error(res.message));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
    }
}

