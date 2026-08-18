package com.cinema.ticket_booking.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.BookingSummary
import com.cinema.ticket_booking.data.model.response.PageResponse
import com.cinema.ticket_booking.data.repository.BookingRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.ArrayList
import javax.inject.Inject

@HiltViewModel
class BookingHistoryViewModel @Inject constructor(
    private val repo: BookingRepository
) : ViewModel() {

    private val _bookings = MutableLiveData<Resource<PageResponse<BookingSummary>>>()
    val bookings: LiveData<Resource<PageResponse<BookingSummary>>> = _bookings

    private val accumulatedBookings = ArrayList<BookingSummary>()

    var isUpcomingTab = true
    private var currentPage = 0
    private val pageSize = 15
    private var isLastPage = false
    private var isLoading = false

    init {
        loadPage(0)
    }



    fun refresh() {
        if (isLoading) return
        accumulatedBookings.clear()
        currentPage = 0
        isLastPage = false
        loadPage(currentPage)
    }

    fun loadMore() {
        if (isLoading || isLastPage) return
        loadPage(currentPage + 1)
    }

    fun isLoadingMore(): Boolean {
        return isLoading && currentPage > 0
    }

    private fun loadPage(page: Int) {
        isLoading = true
        repo.getMyBookings(page, pageSize).observeForever { resource ->
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                isLoading = false
                currentPage = page
                isLastPage = resource.data.last
                if (page == 0) accumulatedBookings.clear()
                resource.data.content?.let { accumulatedBookings.addAll(it) }

                val newPageResp = PageResponse<BookingSummary>().apply {
                    content = accumulatedBookings
                    last = isLastPage
                    totalElements = resource.data.totalElements
                }

                _bookings.value = Resource.success(newPageResp)
            } else if (resource.status == Resource.Status.ERROR) {
                isLoading = false
                _bookings.value = resource
            } else if (resource.status == Resource.Status.LOADING && page == 0) {
                _bookings.value = Resource.loading()
            }
        }
    }
}
