package com.cinema.ticket_booking.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.VoucherSummary
import com.cinema.ticket_booking.data.repository.VoucherRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VoucherViewModel @Inject constructor(
    private val voucherRepo: VoucherRepository
) : ViewModel() {

    private val _vouchers = MutableLiveData<Resource<List<VoucherSummary>>>()
    val vouchers: LiveData<Resource<List<VoucherSummary>>> = _vouchers

    private val _claimResult = MutableLiveData<Resource<Void>>()
    val claimResult: LiveData<Resource<Void>> = _claimResult

    fun loadVouchers() {
        voucherRepo.getMyVouchers().observeForever { _vouchers.value = it }
    }

    fun claimVoucher(code: String) {
        voucherRepo.claimVoucher(code).observeForever { res ->
            _claimResult.value = res
            if (res != null && res.isSuccess) {
                loadVouchers()
            }
        }
    }

    fun clearClaimResult() {
        _claimResult.value = null
    }
}
