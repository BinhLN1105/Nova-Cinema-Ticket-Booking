package com.cinema.ticket_booking.ui.home;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.MovieSummary;
import com.cinema.ticket_booking.data.model.response.PageResponse;
import com.cinema.ticket_booking.data.repository.MovieRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final MovieRepository movieRepository;
    private final MutableLiveData<Resource<PageResponse<MovieSummary>>> nowShowing = new MutableLiveData<>();
    private final MutableLiveData<Resource<PageResponse<MovieSummary>>> comingSoon = new MutableLiveData<>();

    @Inject
    public HomeViewModel(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
        loadMovies();
    }

    public LiveData<Resource<PageResponse<MovieSummary>>> getNowShowing() {
        return nowShowing;
    }

    public LiveData<Resource<PageResponse<MovieSummary>>> getComingSoon() {
        return comingSoon;
    }

    public void refresh() {
        loadMovies();
    }

    public void loadHomeData() {
        loadMovies();
    }

    private void loadMovies() {
        movieRepository.getMovies("NOW_SHOWING", 0, 20)
                .observeForever(nowShowing::setValue);
        movieRepository.getMovies("COMING_SOON", 0, 20)
                .observeForever(comingSoon::setValue);
    }
}
