package com.cinema.ticket_booking.ui.booking;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.data.repository.*;
import com.cinema.ticket_booking.util.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class SelectShowtimeViewModel extends ViewModel {
    private final ShowtimeRepository showtimeRepo;
    private final CinemaRepository cinemaRepo;

    private final MutableLiveData<Resource<List<ShowtimeResponse>>> showtimes = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<CinemaResponse>>> cinemas = new MutableLiveData<>();
    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();
    private final MutableLiveData<String> selectedCinemaId = new MutableLiveData<>();
    private String movieId;

    // Booking state shared sang SelectSeat
    public static String pendingShowtimeId;
    public static String pendingMovieTitle;
    public static String pendingShowtimeTime;
    public static String pendingCinemaName;
    public static String pendingMoviePoster;
    public static String pendingShowDate;

    @Inject
    public SelectShowtimeViewModel(ShowtimeRepository showtimeRepo, CinemaRepository cinemaRepo) {
        this.showtimeRepo = showtimeRepo;
        this.cinemaRepo = cinemaRepo;
        selectedDate.setValue(todayDate());
    }

    public LiveData<Resource<List<ShowtimeResponse>>> getShowtimes() {
        return showtimes;
    }

    public LiveData<Resource<List<CinemaResponse>>> getCinemas() {
        return cinemas;
    }

    public LiveData<String> getSelectedDate() {
        return selectedDate;
    }

    public String getSelectedCinemaId() {
        return selectedCinemaId.getValue();
    }

    public void loadCinemas() {
        cinemaRepo.getCinemas(null).observeForever(cinemas::setValue);
    }

    public void selectDate(String date) {
        selectedDate.setValue(date);
        loadShowtimes(movieId);
    }

    public void selectCinema(String cinemaId) {
        selectedCinemaId.setValue(cinemaId);
        loadShowtimes(movieId);
    }

    public void loadShowtimes(String movieId) {
        if (movieId != null) {
            this.movieId = movieId;
        }
        String date = selectedDate.getValue();
        String cinema = selectedCinemaId.getValue();
        // Cần ít nhất movieId hoặc cinemaId để gọi API
        if (this.movieId == null && cinema == null) return;
        showtimeRepo.getShowtimes(this.movieId, cinema, date).observeForever(showtimes::setValue);
    }

    public List<String> getNext7Days() {
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            dates.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;
    }

    private String todayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}
