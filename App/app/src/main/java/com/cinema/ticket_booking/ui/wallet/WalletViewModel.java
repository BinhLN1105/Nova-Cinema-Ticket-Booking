package com.cinema.ticket_booking.ui.wallet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cinema.ticket_booking.data.model.response.GiftCardResponse;
import com.cinema.ticket_booking.data.model.response.UserResponse;
import com.cinema.ticket_booking.data.repository.UserRepository;
import com.cinema.ticket_booking.util.Resource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class WalletViewModel extends ViewModel {

    private final UserRepository userRepo;
    private final MutableLiveData<Resource<UserResponse>> profile = new MutableLiveData<>();
    private final MutableLiveData<Resource<GiftCardResponse>> redeemResult = new MutableLiveData<>();

    @Inject
    public WalletViewModel(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public LiveData<Resource<UserResponse>> getProfile() {
        return profile;
    }

    public LiveData<Resource<GiftCardResponse>> getRedeemResult() {
        return redeemResult;
    }

    public void loadProfile() {
        userRepo.getMyProfile().observeForever(profile::setValue);
    }

    public void redeemGiftCard(String code) {
        userRepo.redeemGiftCard(code).observeForever(redeemResult::setValue);
    }
}
