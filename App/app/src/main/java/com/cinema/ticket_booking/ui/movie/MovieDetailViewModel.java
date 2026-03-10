package com.cinema.ticket_booking.ui.movie;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.MovieDetail;
import com.cinema.ticket_booking.data.repository.MovieRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class MovieDetailViewModel extends ViewModel {
    private final MovieRepository repo;
    private final MutableLiveData<Resource<MovieDetail>> movie = new MutableLiveData<>();

    @Inject
    public MovieDetailViewModel(MovieRepository repo) { this.repo = repo; }

    public LiveData<Resource<MovieDetail>> getMovie() { return movie; }

    public void loadMovie(String id) {
        repo.getMovieDetail(id).observeForever(movie::setValue);
    }
}
