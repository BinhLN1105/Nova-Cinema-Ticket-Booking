package com.cinema.ticket_booking.ui.wallet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.model.response.VoucherSummary;
import com.cinema.ticket_booking.data.repository.VoucherRepository;
import com.cinema.ticket_booking.util.Resource;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VoucherViewModel extends ViewModel {

    private final VoucherRepository voucherRepo;
    private final MutableLiveData<Resource<List<VoucherSummary>>> vouchers = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> claimResult = new MutableLiveData<>();

    @Inject
    public VoucherViewModel(VoucherRepository voucherRepo) {
        this.voucherRepo = voucherRepo;
    }

    public LiveData<Resource<List<VoucherSummary>>> getVouchers() {
        return vouchers;
    }

    public LiveData<Resource<Void>> getClaimResult() {
        return claimResult;
    }

    public void loadVouchers() {
        voucherRepo.getMyVouchers().observeForever(vouchers::setValue);
    }

    public void claimVoucher(String code) {
        voucherRepo.claimVoucher(code).observeForever(res -> {
            claimResult.setValue(res);
            if (res != null && res.isSuccess()) {
                loadVouchers();
            }
        });
    }

    public void clearClaimResult() {
        claimResult.setValue(null);
    }
}
