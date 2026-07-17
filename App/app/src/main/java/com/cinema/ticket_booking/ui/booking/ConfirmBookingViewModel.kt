package com.cinema.ticket_booking.ui.booking

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.request.BookingRequest
import com.cinema.ticket_booking.data.model.response.BookingResponse
import com.cinema.ticket_booking.data.model.response.ComboResponse
import com.cinema.ticket_booking.data.model.response.PaymentResponse
import com.cinema.ticket_booking.data.model.response.UserResponse
import com.cinema.ticket_booking.data.model.response.VoucherSummary
import com.cinema.ticket_booking.data.repository.BookingRepository
import com.cinema.ticket_booking.data.repository.ComboRepository
import com.cinema.ticket_booking.data.repository.UserRepository
import com.cinema.ticket_booking.data.repository.VoucherRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.ArrayList
import javax.inject.Inject

@HiltViewModel
class ConfirmBookingViewModel @Inject constructor(
    private val bookingRepo: BookingRepository,
    private val voucherRepo: VoucherRepository,
    private val comboRepo: ComboRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _combos = MutableLiveData<Resource<List<ComboResponse>>>()
    val combos: LiveData<Resource<List<ComboResponse>>> = _combos

    private val _voucher = MutableLiveData<Resource<VoucherSummary>?>()
    val voucher: LiveData<Resource<VoucherSummary>?> = _voucher

    private val _bookingResult = MutableLiveData<Resource<BookingResponse>?>()
    val bookingResult: LiveData<Resource<BookingResponse>?> = _bookingResult

    private val _quoteResult = MutableLiveData<Resource<BookingResponse>>()
    val quoteResult: LiveData<Resource<BookingResponse>> = _quoteResult

    private val _userProfile = MutableLiveData<Resource<UserResponse>>()
    val userProfile: LiveData<Resource<UserResponse>> = _userProfile

    private val _walletPaymentResult = MutableLiveData<Resource<PaymentResponse>?>()
    val walletPaymentResult: LiveData<Resource<PaymentResponse>?> = _walletPaymentResult

    var appliedVoucher: VoucherSummary? = null
        private set
    private var myVouchers: List<VoucherSummary> = ArrayList()

    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    init {
        loadCombos()
        loadMyVouchers()
        loadUserProfile()
    }

    fun getMyVouchers(): List<VoucherSummary> = myVouchers

    fun resetBookingResult() {
        _bookingResult.value = null
    }

    private fun loadUserProfile() {
        userRepo.getMyProfile().observeForever { _userProfile.value = it }
    }

    fun setInitialQuote(quote: BookingResponse?) {
        if (quote != null) {
            _quoteResult.value = Resource.success(quote)
        }
    }

    private fun loadCombos() {
        comboRepo.getCombos().observeForever { _combos.value = it }
    }

    private fun loadMyVouchers() {
        voucherRepo.getMyVouchers().observeForever { r ->
            if (r.isSuccess && r.data != null) {
                myVouchers = r.data
                autoApplyBestVoucher()
            }
        }
    }

    private fun autoApplyBestVoucher() {
        if (appliedVoucher != null || _quoteResult.value == null || _quoteResult.value?.data == null) return

        val subtotal = _quoteResult.value?.data?.subtotal ?: 0.0
        if (subtotal <= 0) return

        var maxDiscount = 0.0
        var best: VoucherSummary? = null

        for (v in myVouchers) {
            if ("AVAILABLE" != v.status) continue
            if (v.minOrder > 0 && subtotal < v.minOrder) continue

            var discount = 0.0
            if ("PERCENTAGE" == v.discountType) {
                discount = (subtotal * v.discountValue) / 100.0
            } else {
                discount = v.discountValue
            }

            if (v.maxDiscount > 0 && discount > v.maxDiscount) {
                discount = v.maxDiscount
            }

            if (discount > maxDiscount) {
                maxDiscount = discount
                best = v
            }
        }

        if (best != null) {
            appliedVoucher = best
            refreshQuote()
        }
    }

    fun validateVoucher(code: String) {
        voucherRepo.validateVoucher(code).observeForever { r ->
            _voucher.value = r
            if (r.isSuccess) {
                appliedVoucher = r.data
                refreshQuote()
            }
        }
    }

    fun applyVoucherDirectly(v: VoucherSummary) {
        this.appliedVoucher = v
        refreshQuote()
    }

    fun clearVoucher() {
        appliedVoucher = null
        _voucher.value = null
        refreshQuote()
    }

    fun onComboChanged() {
        refreshQuote()
    }

    fun refreshQuote() {
        debounceRunnable?.let { debounceHandler.removeCallbacks(it) }

        debounceRunnable = Runnable {
            val showtimeId = SelectShowtimeViewModel.pendingShowtimeId ?: return@Runnable

            val comboItems = ArrayList<BookingRequest.ComboItem>()
            for ((key, value) in SelectComboViewModel.pendingCombos) {
                if (value > 0) {
                    comboItems.add(BookingRequest.ComboItem(key, value))
                }
            }

            val voucherCode = appliedVoucher?.code
            val req = BookingRequest(showtimeId, SelectSeatViewModel.pendingSeatIds, comboItems, voucherCode)

            bookingRepo.getBookingQuote(req).observeForever { r ->
                _quoteResult.postValue(r)
                if (r.isSuccess) {
                    autoApplyBestVoucher()
                }
            }
        }

        debounceRunnable?.let { debounceHandler.postDelayed(it, 500) }
    }

    fun confirmBooking() {
        val showtimeId = SelectShowtimeViewModel.pendingShowtimeId ?: return

        val comboItems = ArrayList<BookingRequest.ComboItem>()
        for ((key, value) in SelectComboViewModel.pendingCombos) {
            if (value > 0) {
                comboItems.add(BookingRequest.ComboItem(key, value))
            }
        }

        val voucherCode = appliedVoucher?.code
        val req = BookingRequest(showtimeId, SelectSeatViewModel.pendingSeatIds, comboItems, voucherCode)

        bookingRepo.createBooking(req).observeForever { _bookingResult.value = it }
    }

    fun confirmBookingAndPayWithWallet() {
        val showtimeId = SelectShowtimeViewModel.pendingShowtimeId ?: return

        val comboItems = ArrayList<BookingRequest.ComboItem>()
        for ((key, value) in SelectComboViewModel.pendingCombos) {
            if (value > 0) {
                comboItems.add(BookingRequest.ComboItem(key, value))
            }
        }

        val voucherCode = appliedVoucher?.code
        val req = BookingRequest(showtimeId, SelectSeatViewModel.pendingSeatIds, comboItems, voucherCode)

        // Chu trình: Create Booking -> Pay with Wallet
        _bookingResult.value = Resource.loading()
        bookingRepo.createBooking(req).observeForever { res ->
            if (res.isSuccess && res.data != null) {
                val bookingId = res.data.id
                if (bookingId != null) {
                    bookingRepo.payWithWallet(bookingId).observeForever { _walletPaymentResult.value = it }
                } else {
                    _walletPaymentResult.value = Resource.error("Không tìm thấy ID đơn hàng", null)
                }
            } else if (res.isError) {
                _bookingResult.value = Resource.error(res.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
    }
}
