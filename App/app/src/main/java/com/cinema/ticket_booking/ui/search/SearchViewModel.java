package com.cinema.ticket_booking.ui.search;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.data.repository.MovieRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class SearchViewModel extends ViewModel {
    private final MovieRepository repo;
    private final MutableLiveData<Resource<PageResponse<MovieSummary>>> results = new MutableLiveData<>();
    @Inject public SearchViewModel(MovieRepository repo) { this.repo = repo; }
    public LiveData<Resource<PageResponse<MovieSummary>>> getResults() { return results; }
    public void search(String q) { repo.searchMovies(q, 0, 20).observeForever(results::setValue); }
    public void clearResults() { results.setValue(null); }
}
