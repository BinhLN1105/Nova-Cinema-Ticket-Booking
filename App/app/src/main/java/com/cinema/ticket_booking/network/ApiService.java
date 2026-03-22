package com.cinema.ticket_booking.network;

import com.cinema.ticket_booking.data.model.request.*;
import com.cinema.ticket_booking.data.model.response.*;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

        // ── Auth ─────────────────────────────────────────────────────────────
        @POST("auth/register")
        Call<ApiResponse<AuthResponse>> register(@Body RegisterRequest request);

        @POST("auth/login")
        Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

        @POST("auth/social-login")
        Call<ApiResponse<AuthResponse>> socialLogin(@Body SocialLoginRequest request);

        @POST("auth/refresh")
        Call<ApiResponse<AuthResponse>> refreshToken(@Body RefreshTokenRequest request);

        @POST("auth/logout")
        Call<ApiResponse<Void>> logout(@Body RefreshTokenRequest request);

        // ── User ─────────────────────────────────────────────────────────────
        @GET("users/me")
        Call<ApiResponse<UserResponse>> getMyProfile();

        @PATCH("users/me")
        Call<ApiResponse<UserResponse>> updateProfile(@Body UpdateProfileRequest request);

        @PATCH("users/me/fcm-token")
        Call<ApiResponse<Void>> updateFcmToken(@Query("token") String token);

        // ── Movie ─────────────────────────────────────────────────────────────
        @GET("movies")
        Call<ApiResponse<PageResponse<MovieSummary>>> getMovies(
                        @Query("status") String status,
                        @Query("page") int page,
                        @Query("size") int size);

        @GET("movies/search")
        Call<ApiResponse<PageResponse<MovieSummary>>> searchMovies(
                        @Query("q") String query,
                        @Query("page") int page,
                        @Query("size") int size);

        @GET("movies/{id}")
        Call<ApiResponse<MovieDetail>> getMovieDetail(@Path("id") String id);

        @GET("movies/cinema/{cinemaId}")
        Call<ApiResponse<List<MovieSummary>>> getNowShowingByCinema(@Path("cinemaId") String cinemaId);

        @GET("movies/genres")
        Call<ApiResponse<List<Genre>>> getAllGenres();

        // ── Cinema ────────────────────────────────────────────────────────────
        @GET("cinemas")
        Call<ApiResponse<List<CinemaResponse>>> getCinemas(@Query("city") String city);

        @GET("cinemas/{id}")
        Call<ApiResponse<CinemaResponse>> getCinemaDetail(@Path("id") String id);

        // ── Showtime ──────────────────────────────────────────────────────────
        @GET("showtimes")
        Call<ApiResponse<List<ShowtimeResponse>>> getShowtimes(
                        @Query("movieId") String movieId,
                        @Query("cinemaId") String cinemaId,
                        @Query("date") String date); // format: yyyy-MM-dd

        @GET("showtimes/{id}/seats")
        Call<ApiResponse<SeatMapResponse>> getSeatMap(@Path("id") String showtimeId);

        // ── Booking ───────────────────────────────────────────────────────────
        @POST("bookings")
        Call<ApiResponse<BookingResponse>> createBooking(@Body BookingRequest request);

        @GET("bookings/me")
        Call<ApiResponse<PageResponse<BookingSummary>>> getMyBookings(
                        @Query("page") int page,
                        @Query("size") int size);

        @GET("bookings/{id}")
        Call<ApiResponse<BookingResponse>> getBookingDetail(@Path("id") String id);

        @POST("bookings/check-in")
        Call<ApiResponse<CheckInResponse>> checkIn(@Query("qrCode") String qrCode);

        @DELETE("bookings/{id}")
        Call<ApiResponse<Void>> cancelBooking(@Path("id") String id);

        // ── Payment ───────────────────────────────────────────────────────────
        @POST("payments")
        Call<ApiResponse<PaymentResponse>> createPayment(@Body PaymentRequest request);

        @GET("payments/booking/{bookingId}")
        Call<ApiResponse<PaymentResponse>> getPaymentStatus(@Path("bookingId") String bookingId);

        // ── Voucher ───────────────────────────────────────────────────────────
        @GET("vouchers/validate")
        Call<ApiResponse<VoucherSummary>> validateVoucher(@Query("code") String code);

        @GET("vouchers/me")
        Call<ApiResponse<PageResponse<VoucherSummary>>> getMyVouchers(
                        @Query("page") int page,
                        @Query("size") int size);

        // ── Combo ─────────────────────────────────────────────────────────────
        @GET("combos")
        Call<ApiResponse<List<ComboResponse>>> getCombos();

        // ── Review ────────────────────────────────────────────────────────────
        @GET("reviews")
        Call<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
                        @Query("movieId") String movieId,
                        @Query("page") int page,
                        @Query("size") int size);

        @POST("reviews")
        Call<ApiResponse<ReviewResponse>> createReview(@Body ReviewRequest request);

        // ── GiftCard ─────────────────────────────────────────────────────
        @POST("gift-cards/redeem")
        Call<ApiResponse<GiftCardResponse>> redeemGiftCard(@Body java.util.Map<String, String> body);

        @GET("gift-cards/me")
        Call<ApiResponse<PageResponse<GiftCardResponse>>> getMyGiftCards(
                        @Query("page") int page,
                        @Query("size") int size);

        // ── Notification ──────────────────────────────────────────────────────
        @GET("notifications")
        Call<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
                        @Query("page") int page,
                        @Query("size") int size);

        @GET("notifications/unread-count")
        Call<ApiResponse<UnreadCountResponse>> getUnreadCount();

        @PATCH("notifications/read-all")
        Call<ApiResponse<Void>> markAllAsRead();

        // ── Chatbot ───────────────────────────────────────────────────────────
        @POST("chatbot/chat")
        Call<ApiResponse<java.util.Map<String, String>>> chatWithAi(@Body ChatRequest request);

        @POST("chatbot/session/clear")
        Call<ApiResponse<java.util.Map<String, String>>> clearChatSession();
}
