package com.cinema.ticket_booking.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.cinema.ticket_booking.data.model.response.CinemaResponse
import com.cinema.ticket_booking.data.repository.CinemaRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: CinemaRepository
) : ViewModel() {
    private val cityTrigger = MutableLiveData<String>()
    val cinemas: LiveData<Resource<List<CinemaResponse>>> = cityTrigger.switchMap { city ->
        repo.getCinemas(city)
    }

    fun loadCinemas(city: String) {
        cityTrigger.value = city
    }
}
