package com.cinema.ticket_booking.ui.movie;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.request.ReviewRequest;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.data.repository.MovieRepository;
import com.cinema.ticket_booking.data.repository.ReviewRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class MovieDetailViewModel extends ViewModel {
    private final MovieRepository movieRepo;
    private final ReviewRepository reviewRepo;
    private final MutableLiveData<Resource<MovieDetail>> movie = new MutableLiveData<>();
    private final MutableLiveData<Resource<PageResponse<ReviewResponse>>> reviews = new MutableLiveData<>();
    private final MutableLiveData<Resource<ReviewResponse>> createReviewResult = new MutableLiveData<>();

    @Inject
    public MovieDetailViewModel(MovieRepository movieRepo, ReviewRepository reviewRepo) {
        this.movieRepo = movieRepo;
        this.reviewRepo = reviewRepo;
    }

    public LiveData<Resource<MovieDetail>> getMovie() {
        return movie;
    }

    public LiveData<Resource<PageResponse<ReviewResponse>>> getReviews() {
        return reviews;
    }

    public LiveData<Resource<ReviewResponse>> getCreateReviewResult() {
        return createReviewResult;
    }

    public LiveData<Resource<CanReviewResponse>> checkReviewEligibility(String movieId) {
        return movieRepo.canReview(movieId);
    }

    public void loadMovie(String id) {
        movieRepo.getMovieDetail(id).observeForever(movie::setValue);
    }

    public void loadReviews(String movieId) {
        reviewRepo.getReviews(movieId, 0, 3).observeForever(reviews::setValue);
    }

    public void loadReviewsPaged(String movieId, int page, int size) {
        reviewRepo.getReviews(movieId, page, size).observeForever(reviews::setValue);
    }

    public void loadReviewsFiltered(String movieId, int page, int size, Integer rating) {
        reviewRepo.getReviews(movieId, page, size, rating).observeForever(reviews::setValue);
    }

    public void submitReview(String movieId, String bookingId, int rating, String comment) {
        reviewRepo.createReview(new ReviewRequest(movieId, bookingId, rating, comment))
                .observeForever(createReviewResult::setValue);
    }
    public LiveData<Resource<ReviewResponse>> getReviewResult() {
        return createReviewResult;
    }

    public void submitReview(ReviewRequest request) {
        // Reset về null trước để tránh observer nhận lại giá trị cũ khi mở sheet mới
        createReviewResult.setValue(null);
        reviewRepo.createReview(request).observeForever(createReviewResult::setValue);
    }

    public LiveData<Resource<ReviewResponse>> createReview(ReviewRequest request) {
        return reviewRepo.createReview(request);
    }
}

