package com.cinema.ticket_booking.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.ComboResponse
import com.cinema.ticket_booking.data.model.response.SeatMapResponse
import com.cinema.ticket_booking.data.repository.BookingRepository
import com.cinema.ticket_booking.data.repository.ShowtimeRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.LinkedHashMap
import javax.inject.Inject

@HiltViewModel
class SelectSeatViewModel @Inject constructor(
    private val showtimeRepo: ShowtimeRepository,
    private val bookingRepo: BookingRepository
) : ViewModel() {

    private val _seatMap = MutableLiveData<Resource<SeatMapResponse>>()
    val seatMap: LiveData<Resource<SeatMapResponse>> = _seatMap

    private val _seatRefresh = MutableLiveData<SeatMapResponse>()
    val seatRefresh: LiveData<SeatMapResponse> = _seatRefresh

    private val _combos = MutableLiveData<Resource<List<ComboResponse>>>()
    val combos: LiveData<Resource<List<ComboResponse>>> = _combos

    val selectedSeatIds: MutableSet<String> = LinkedHashSet()
    val selectedCombos: MutableMap<String, Int> = LinkedHashMap()
    private var currentShowtimeId: String? = null

    companion object {
        @JvmField
        var pendingSeatIds: List<String> = ArrayList()
        @JvmField
        var pendingCombos: Map<String, Int> = LinkedHashMap()
        @JvmField
        var pendingTotalAmount: Double = 0.0
    }

    fun loadSeatMap(showtimeId: String) {
        currentShowtimeId = showtimeId
        showtimeRepo.getSeatMap(showtimeId).observeForever { _seatMap.value = it }
    }

    /** Silent background refresh — does not trigger loading indicator. */
    fun refreshSeatStatuses() {
        val showtimeId = currentShowtimeId ?: return
        showtimeRepo.getSeatMap(showtimeId).observeForever { r ->
            if (r != null && r.isSuccess && r.data != null) {
                _seatRefresh.postValue(r.data)
            }
        }
    }

    fun toggleSeat(seat: SeatMapResponse.SeatItem): Boolean {
        if ("BOOKED" == seat.status || "LOCKED" == seat.status) return false
        val seatId = seat.showtimeSeatId ?: return false
        if (selectedSeatIds.contains(seatId)) {
            selectedSeatIds.remove(seatId)
        } else {
            selectedSeatIds.add(seatId)
        }
        return true
    }

    fun calculateTotal(seatMapData: SeatMapResponse?): Double {
        if (seatMapData?.seats == null) return 0.0
        var total = 0.0
        for (seat in seatMapData.seats) {
            if (selectedSeatIds.contains(seat.showtimeSeatId)) {
                total += seat.price
            }
        }
        return total
    }
}
