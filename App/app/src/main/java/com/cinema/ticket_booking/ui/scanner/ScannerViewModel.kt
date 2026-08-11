package com.cinema.ticket_booking.ui.scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.CheckInResponse
import com.cinema.ticket_booking.data.repository.AuthRepository
import com.cinema.ticket_booking.data.repository.BookingRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val bookingRepo: BookingRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _checkInResult = MutableLiveData<Resource<CheckInResponse>>()
    val checkInResult: LiveData<Resource<CheckInResponse>> = _checkInResult

    fun logout() {
        authRepo.logout()
    }

    fun checkInTicket(qrCode: String) {
        bookingRepo.checkIn(qrCode).observeForever { _checkInResult.value = it }
    }
}
