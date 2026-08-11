package com.cinema.ticket_booking.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.request.BookingRequest
import com.cinema.ticket_booking.data.model.response.BookingResponse
import com.cinema.ticket_booking.data.model.response.ComboResponse
import com.cinema.ticket_booking.data.repository.BookingRepository
import com.cinema.ticket_booking.data.repository.ComboRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.HashMap
import java.util.LinkedHashMap
import javax.inject.Inject

@HiltViewModel
class SelectComboViewModel @Inject constructor(
    private val comboRepo: ComboRepository,
    private val bookingRepo: BookingRepository
) : ViewModel() {

    private val _combos = MutableLiveData<Resource<List<ComboResponse>>>()
    val combos: LiveData<Resource<List<ComboResponse>>> = _combos

    private val _localTotal = MutableLiveData(0.0)
    val localTotal: LiveData<Double> = _localTotal

    val selectedCombos: MutableMap<String, Int> = LinkedHashMap()
    private val comboPriceMap = HashMap<String, Double>()

    companion object {
        @JvmField
        val pendingCombos = HashMap<String, Int>()
    }

    init {
        loadCombos()
    }

    private fun loadCombos() {
        comboRepo.getCombos().observeForever { resource ->
            _combos.value = resource
            if (resource.isSuccess && resource.data != null) {
                for (combo in resource.data) {
                    combo.id?.let { id ->
                        comboPriceMap[id] = combo.price
                    }
                }
                updateLocalTotal()
            }
        }
    }

    fun addCombo(comboId: String) {
        selectedCombos[comboId] = (selectedCombos[comboId] ?: 0) + 1
        pendingCombos.clear()
        pendingCombos.putAll(selectedCombos)
        updateLocalTotal()
    }

    fun removeCombo(comboId: String) {
        if (selectedCombos.containsKey(comboId)) {
            val count = selectedCombos[comboId] ?: 0
            if (count > 1) {
                selectedCombos[comboId] = count - 1
            } else {
                selectedCombos.remove(comboId)
            }
        }
        pendingCombos.clear()
        pendingCombos.putAll(selectedCombos)
        updateLocalTotal()
    }

    private fun updateLocalTotal() {
        var total = SelectSeatViewModel.pendingTotalAmount
        for ((key, value) in selectedCombos) {
            val price = comboPriceMap[key]
            if (price != null) {
                total += price * value
            }
        }
        _localTotal.value = total
    }

    fun getFinalQuote(): LiveData<Resource<BookingResponse>> {
        val showtimeId = SelectShowtimeViewModel.pendingShowtimeId
        if (showtimeId == null) {
            val error = MutableLiveData<Resource<BookingResponse>>()
            error.value = Resource.error("Lỗi: Không tìm thấy thông tin suất chiếu", null)
            return error
        }

        val comboItems = ArrayList<BookingRequest.ComboItem>()
        for ((key, value) in selectedCombos) {
            if (value > 0) {
                comboItems.add(BookingRequest.ComboItem(key, value))
            }
        }

        val req = BookingRequest(showtimeId, SelectSeatViewModel.pendingSeatIds, comboItems, null)
        return bookingRepo.getBookingQuote(req)
    }
}
