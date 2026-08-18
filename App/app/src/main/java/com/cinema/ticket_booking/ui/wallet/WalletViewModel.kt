package com.cinema.ticket_booking.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.GiftCardResponse
import com.cinema.ticket_booking.data.model.response.UserResponse
import com.cinema.ticket_booking.data.repository.UserRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val userRepo: UserRepository
) : ViewModel() {

    private val _profile = MutableLiveData<Resource<UserResponse>>()
    val profile: LiveData<Resource<UserResponse>> = _profile

    private val _redeemResult = MutableLiveData<Resource<GiftCardResponse>>()
    val redeemResult: LiveData<Resource<GiftCardResponse>> = _redeemResult

    fun loadProfile() {
        userRepo.getMyProfile().observeForever { _profile.value = it }
    }

    fun redeemGiftCard(code: String) {
        userRepo.redeemGiftCard(code).observeForever { _redeemResult.value = it }
    }
}
