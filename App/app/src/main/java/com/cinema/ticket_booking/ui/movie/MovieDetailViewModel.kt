package com.cinema.ticket_booking.ui.movie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.model.request.ReviewRequest
import com.cinema.ticket_booking.data.model.response.CanReviewResponse
import com.cinema.ticket_booking.data.model.response.MovieDetail
import com.cinema.ticket_booking.data.model.response.PageResponse
import com.cinema.ticket_booking.data.model.response.ReviewResponse
import com.cinema.ticket_booking.data.repository.MovieRepository
import com.cinema.ticket_booking.data.repository.ReviewRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val movieRepo: MovieRepository,
    private val reviewRepo: ReviewRepository
) : ViewModel() {
    private val movie = MutableLiveData<Resource<MovieDetail>>()
    private val reviews = MutableLiveData<Resource<PageResponse<ReviewResponse>>>()
    private val createReviewResult = MutableLiveData<Resource<ReviewResponse>?>()

    fun getMovie(): LiveData<Resource<MovieDetail>> = movie

    fun getReviews(): LiveData<Resource<PageResponse<ReviewResponse>>> = reviews

    fun getCreateReviewResult(): LiveData<Resource<ReviewResponse>?> = createReviewResult

    fun checkReviewEligibility(movieId: String): LiveData<Resource<CanReviewResponse>> {
        return movieRepo.canReview(movieId)
    }

    fun loadMovie(id: String) {
        movieRepo.getMovieDetail(id).observeForever { resource ->
            movie.value = resource
        }
    }

    fun loadReviews(movieId: String) {
        reviewRepo.getReviews(movieId, 0, 3).observeForever { resource ->
            reviews.value = resource
        }
    }

    fun loadReviewsPaged(movieId: String, page: Int, size: Int) {
        reviewRepo.getReviews(movieId, page, size).observeForever { resource ->
            reviews.value = resource
        }
    }

    fun loadReviewsFiltered(movieId: String, page: Int, size: Int, rating: Int?) {
        reviewRepo.getReviews(movieId, page, size, rating).observeForever { resource ->
            reviews.value = resource
        }
    }

    fun submitReview(movieId: String, bookingId: String?, rating: Int, comment: String) {
        reviewRepo.createReview(ReviewRequest(movieId, bookingId, rating, comment))
            .observeForever { resource ->
                createReviewResult.value = resource
            }
    }

    fun getReviewResult(): LiveData<Resource<ReviewResponse>?> = createReviewResult

    fun submitReview(request: ReviewRequest) {
        // Reset về null trước để tránh observer nhận lại giá trị cũ khi mở sheet mới
        createReviewResult.value = null
        reviewRepo.createReview(request).observeForever { resource ->
            createReviewResult.value = resource
        }
    }

    fun updateReview(id: String, request: ReviewRequest) {
        createReviewResult.value = null
        reviewRepo.updateReview(id, request).observeForever { resource ->
            createReviewResult.value = resource
        }
    }

    fun createReview(request: ReviewRequest): LiveData<Resource<ReviewResponse>> {
        return reviewRepo.createReview(request)
    }
}
