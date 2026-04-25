package com.cinema.ticket_booking.ui.home;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.MovieSummary;
import com.cinema.ticket_booking.data.model.response.PageResponse;
import com.cinema.ticket_booking.data.model.response.PromotionResponse;
import com.cinema.ticket_booking.data.model.response.VoucherSyncResponse;
import com.cinema.ticket_booking.data.model.response.CinemaResponse;
import com.cinema.ticket_booking.data.repository.MovieRepository;
import com.cinema.ticket_booking.data.repository.PromotionRepository;
import com.cinema.ticket_booking.data.repository.VoucherRepository;
import com.cinema.ticket_booking.data.repository.CinemaRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

import java.util.List;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final MovieRepository movieRepository;
    private final VoucherRepository voucherRepository;
    private final PromotionRepository promotionRepository;
    private final CinemaRepository cinemaRepository;
    
    private final MutableLiveData<Resource<PageResponse<MovieSummary>>> nowShowing = new MutableLiveData<>();
    private final MutableLiveData<Resource<PageResponse<MovieSummary>>> comingSoon = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<VoucherSyncResponse>>> activeVouchers = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<PromotionResponse>>> activePromotions = new MutableLiveData<>();
    private final MutableLiveData<Resource<PageResponse<MovieSummary>>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<MovieSummary>>> featuredMovies = new MutableLiveData<>();
    private final MutableLiveData<Resource<PromotionResponse>> popupPromotion = new MutableLiveData<>();

    // Quick Booking State
    private final MutableLiveData<MovieSummary> quickSelectedMovie = new MutableLiveData<>(null);
    private final MutableLiveData<CinemaResponse> quickSelectedCinema = new MutableLiveData<>(null);
    private final MutableLiveData<String> quickSelectedDate = new MutableLiveData<>(null);

    @Inject
    public HomeViewModel(MovieRepository movieRepository, VoucherRepository voucherRepository, 
            PromotionRepository promotionRepository, CinemaRepository cinemaRepository) {
        this.movieRepository = movieRepository;
        this.voucherRepository = voucherRepository;
        this.promotionRepository = promotionRepository;
        this.cinemaRepository = cinemaRepository;
        loadMovies();
    }

    public LiveData<Resource<PageResponse<MovieSummary>>> getNowShowing() {
        return nowShowing;
    }

    public LiveData<Resource<PageResponse<MovieSummary>>> getComingSoon() {
        return comingSoon;
    }

    public LiveData<Resource<List<VoucherSyncResponse>>> getActiveVouchers() {
        return activeVouchers;
    }

    public LiveData<Resource<List<PromotionResponse>>> getActivePromotions() {
        return activePromotions;
    }

    public LiveData<Resource<PageResponse<MovieSummary>>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Resource<List<MovieSummary>>> getFeaturedMovies() {
        return featuredMovies;
    }

    public LiveData<Resource<PromotionResponse>> getPopupPromotion() {
        return popupPromotion;
    }

    public void searchMovies(String query) {
        movieRepository.searchMovies(query, 0, 10).observeForever(searchResults::setValue);
    }

    public void clearSearch() {
        searchResults.setValue(null);
    }

    public LiveData<Resource<List<CinemaResponse>>> getCinemas(String city) {
        return cinemaRepository.getCinemas(city);
    }

    // Quick Booking Getters
    public MutableLiveData<MovieSummary> getQuickSelectedMovie() { return quickSelectedMovie; }
    public MutableLiveData<CinemaResponse> getQuickSelectedCinema() { return quickSelectedCinema; }
    public MutableLiveData<String> getQuickSelectedDate() { return quickSelectedDate; }

    public void refresh() {
        loadMovies();
    }

    public void loadHomeData() {
        loadMovies();
    }

    private void loadMovies() {
        movieRepository.getMovies("NOW_SHOWING", 0, 20)
                .observeForever(nowShowing::setValue);
                
        movieRepository.getFeaturedMovies()
                .observeForever(featuredMovies::setValue);

        movieRepository.getMovies("COMING_SOON", 0, 20)
                .observeForever(comingSoon::setValue);
        voucherRepository.getActiveVouchers()
                .observeForever(activeVouchers::setValue);
        promotionRepository.getActivePromotions()
                .observeForever(activePromotions::setValue);
        promotionRepository.getPopupPromotion()
                .observeForever(popupPromotion::setValue);
    }
}

