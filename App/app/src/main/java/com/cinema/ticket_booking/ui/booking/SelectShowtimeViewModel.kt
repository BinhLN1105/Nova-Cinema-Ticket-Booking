package com.cinema.ticket_booking.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.response.CinemaResponse
import com.cinema.ticket_booking.data.model.response.ShowtimeResponse
import com.cinema.ticket_booking.data.repository.CinemaRepository
import com.cinema.ticket_booking.data.repository.ShowtimeRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SelectShowtimeViewModel @Inject constructor(
    private val showtimeRepo: ShowtimeRepository,
    private val cinemaRepo: CinemaRepository
) : ViewModel() {

    private val _showtimes = MutableLiveData<Resource<List<ShowtimeResponse>>>()
    val showtimes: LiveData<Resource<List<ShowtimeResponse>>> = _showtimes

    private val _cinemas = MutableLiveData<Resource<List<CinemaResponse>>>()
    val cinemas: LiveData<Resource<List<CinemaResponse>>> = _cinemas

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private val _selectedCinemaId = MutableLiveData<String?>()
    val selectedCinemaId: LiveData<String?> = _selectedCinemaId

    private var movieId: String? = null

    companion object {
        @JvmField
        var pendingShowtimeId: String? = null
        @JvmField
        var pendingMovieTitle: String? = null
        @JvmField
        var pendingShowtimeTime: String? = null
        @JvmField
        var pendingCinemaName: String? = null
        @JvmField
        var pendingMoviePoster: String? = null
        @JvmField
        var pendingShowDate: String? = null
    }

    init {
        _selectedDate.value = todayDate()
    }

    fun getSelectedCinemaId(): String? {
        return _selectedCinemaId.value
    }

    fun loadCinemas() {
        cinemaRepo.getCinemas(null).observeForever { _cinemas.value = it }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        loadShowtimes(movieId)
    }

    fun selectCinema(cinemaId: String?) {
        _selectedCinemaId.value = cinemaId
        loadShowtimes(movieId)
    }

    fun loadShowtimes(movieId: String?) {
        if (movieId != null) {
            this.movieId = movieId
        }
        val date = _selectedDate.value
        val cinema = _selectedCinemaId.value
        // Cần ít nhất movieId hoặc cinemaId để gọi API
        if (this.movieId == null && cinema == null) return
        showtimeRepo.getShowtimes(this.movieId, cinema, date).observeForever { _showtimes.value = it }
    }

    val next7Days: List<String>
        get() {
            val dates = ArrayList<String>()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            for (i in 0..6) {
                dates.add(sdf.format(cal.time))
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            return dates
        }

    private fun todayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
