package com.cinema.ticket_booking.ui.search;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.data.repository.CinemaRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import java.util.List;

@HiltViewModel
public class SearchViewModel extends ViewModel {
    private final CinemaRepository repo;
    private final MutableLiveData<Resource<List<CinemaResponse>>> cinemas = new MutableLiveData<>();

    @Inject
    public SearchViewModel(CinemaRepository repo) {
        this.repo = repo;
    }

    public LiveData<Resource<List<CinemaResponse>>> getCinemas() {
        return cinemas;
    }

    public void loadCinemas(String city) {
        repo.getCinemas(city).observeForever(cinemas::setValue);
    }
}

