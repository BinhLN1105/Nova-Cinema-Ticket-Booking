package com.cinema.ticket_booking.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.CinemaResponse
import com.cinema.ticket_booking.data.model.response.MovieSummary
import com.cinema.ticket_booking.data.model.response.PageResponse
import com.cinema.ticket_booking.data.model.response.PromotionResponse
import com.cinema.ticket_booking.data.model.response.VoucherSyncResponse
import com.cinema.ticket_booking.data.repository.CinemaRepository
import com.cinema.ticket_booking.data.repository.MovieRepository
import com.cinema.ticket_booking.data.repository.PromotionRepository
import com.cinema.ticket_booking.data.repository.VoucherRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val voucherRepository: VoucherRepository,
    private val promotionRepository: PromotionRepository,
    private val cinemaRepository: CinemaRepository
) : ViewModel() {

    private val nowShowing = MutableLiveData<Resource<PageResponse<MovieSummary>>>()
    private val comingSoon = MutableLiveData<Resource<PageResponse<MovieSummary>>>()
    private val activeVouchers = MutableLiveData<Resource<List<VoucherSyncResponse>>>()
    private val activePromotions = MutableLiveData<Resource<List<PromotionResponse>>>()
    private val searchResults = MutableLiveData<Resource<PageResponse<MovieSummary>>?>()
    private val featuredMovies = MutableLiveData<Resource<List<MovieSummary>>>()
    private val popupPromotion = MutableLiveData<Resource<PromotionResponse>>()

    // Quick Booking State
    private val quickSelectedMovie = MutableLiveData<MovieSummary?> (null)
    private val quickSelectedCinema = MutableLiveData<CinemaResponse?> (null)
    private val quickSelectedDate = MutableLiveData<String?> (null)

    init {
        loadMovies()
    }

    fun getNowShowing(): LiveData<Resource<PageResponse<MovieSummary>>> = nowShowing
    fun getComingSoon(): LiveData<Resource<PageResponse<MovieSummary>>> = comingSoon
    fun getActiveVouchers(): LiveData<Resource<List<VoucherSyncResponse>>> = activeVouchers
    fun getActivePromotions(): LiveData<Resource<List<PromotionResponse>>> = activePromotions
    fun getSearchResults(): LiveData<Resource<PageResponse<MovieSummary>>?> = searchResults
    fun getFeaturedMovies(): LiveData<Resource<List<MovieSummary>>> = featuredMovies
    fun getPopupPromotion(): LiveData<Resource<PromotionResponse>> = popupPromotion

    fun searchMovies(query: String) {
        movieRepository.searchMovies(query, 0, 10).observeForever { resource ->
            searchResults.value = resource
        }
    }

    fun clearSearch() {
        searchResults.value = null
    }

    fun getCinemas(city: String): LiveData<Resource<List<CinemaResponse>>> {
        return cinemaRepository.getCinemas(city)
    }

    // Quick Booking Getters
    fun getQuickSelectedMovie(): MutableLiveData<MovieSummary?> = quickSelectedMovie
    fun getQuickSelectedCinema(): MutableLiveData<CinemaResponse?> = quickSelectedCinema
    fun getQuickSelectedDate(): MutableLiveData<String?> = quickSelectedDate

    fun refresh() {
        loadMovies()
    }

    fun loadHomeData() {
        loadMovies()
    }

    private fun loadMovies() {
        movieRepository.getMovies("NOW_SHOWING", 0, 20).observeForever { resource ->
            nowShowing.value = resource
        }
        movieRepository.getFeaturedMovies().observeForever { resource ->
            featuredMovies.value = resource
        }
        movieRepository.getMovies("COMING_SOON", 0, 20).observeForever { resource ->
            comingSoon.value = resource
        }
        voucherRepository.getActiveVouchers().observeForever { resource ->
            activeVouchers.value = resource
        }
        promotionRepository.getActivePromotions().observeForever { resource ->
            activePromotions.value = resource
        }
        promotionRepository.getPopupPromotion().observeForever { resource ->
            popupPromotion.value = resource
        }
    }
}
