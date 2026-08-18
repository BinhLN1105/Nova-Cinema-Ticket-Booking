package com.cinema.ticket_booking.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.BookingResponse
import com.cinema.ticket_booking.data.repository.BookingRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val repo: BookingRepository
) : ViewModel() {

    private val _booking = MutableLiveData<Resource<BookingResponse>>()
    val booking: LiveData<Resource<BookingResponse>> = _booking

    fun loadBooking(id: String) {
        repo.getBookingDetail(id).observeForever { _booking.value = it }
    }

    fun cancelConfirm(token: String, bookingId: String): LiveData<Resource<Void>> {
        return repo.cancelConfirm(token, bookingId)
    }
}
