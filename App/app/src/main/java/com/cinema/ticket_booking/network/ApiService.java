package com.cinema.ticket_booking.network;

import com.cinema.ticket_booking.data.model.request.*;
import com.cinema.ticket_booking.data.model.response.*;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.*;
import okhttp3.MultipartBody;

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

        @POST("auth/forgot-password")
        Call<ApiResponse<Void>> forgotPassword(@Body ForgotPasswordRequest request);

        @POST("auth/verify-otp")
        Call<ApiResponse<String>> verifyOtp(@Body Map<String, String> body);

        @POST("auth/reset-password")
        Call<ApiResponse<Void>> resetPassword(@Body ResetPasswordRequest request);

        // ── User ─────────────────────────────────────────────────────────────
        @GET("users/me")
        Call<ApiResponse<UserResponse>> getMyProfile();

        @PATCH("users/me")
        Call<ApiResponse<UserResponse>> updateProfile(@Body UpdateProfileRequest request);

        @PATCH("users/me/notifications")
        Call<ApiResponse<UserResponse>> updateNotificationSettings(@Body NotificationSettingsRequest request);

        @Multipart
        @POST("users/me/avatar")
        Call<ApiResponse<UserResponse>> uploadAvatar(@Part MultipartBody.Part file);

        @PUT("users/me/password")
        Call<ApiResponse<Void>> changePassword(@Body Map<String, String> body);

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

        @GET("movies/{id}/can-review")
        Call<ApiResponse<CanReviewResponse>> canReview(@Path("id") String id);

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

        @POST("bookings/quote")
        Call<ApiResponse<BookingResponse>> getBookingQuote(@Body BookingRequest request);

        @GET("bookings/me")
        Call<ApiResponse<PageResponse<BookingSummary>>> getMyBookings(
                        @Query("page") int page,
                        @Query("size") int size);

        @GET("bookings/{id}")
        Call<ApiResponse<BookingResponse>> getBookingDetail(@Path("id") String id);

        @POST("bookings/check-in")
        Call<ApiResponse<CheckInResponse>> checkIn(@Query("qrCode") String qrCode);

        @GET("bookings/cancel-policy")
        Call<ApiResponse<Map<String, Object>>> getCancelPolicy();

        @POST("bookings/{id}/cancel-request")
        Call<ApiResponse<Void>> cancelRequest(@Path("id") String id);

        @POST("bookings/cancel-confirm")
        Call<ApiResponse<Void>> cancelConfirm(@Query("token") String token, @Query("bookingId") String bookingId);

        // ── Payment ───────────────────────────────────────────────────────────
        @POST("payments")
        Call<ApiResponse<PaymentResponse>> createPayment(@Body PaymentRequest request);

        @GET("payments/booking/{bookingId}")
        Call<ApiResponse<PaymentResponse>> getPaymentStatus(@Path("bookingId") String bookingId);

        @POST("payments/wallet/{bookingId}")
        Call<ApiResponse<PaymentResponse>> payWithWallet(@Path("bookingId") String bookingId);

        // ── Voucher ───────────────────────────────────────────────────────────
        @GET("vouchers/validate")
        Call<ApiResponse<VoucherSummary>> validateVoucher(@Query("code") String code);

        @GET("users/me/vouchers")
        Call<ApiResponse<List<VoucherSummary>>> getMyVouchers();

        @POST("users/me/vouchers/claim")
        Call<ApiResponse<Void>> claimVoucher(@Body ClaimVoucherRequest request);

        @GET("vouchers/active")
        Call<ApiResponse<List<VoucherSyncResponse>>> getActiveVouchers();

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
        Call<ApiResponse<GiftCardResponse>> redeemGiftCard(@Body Map<String, String> body);

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

        // ── Promotion ─────────────────────────────────────────────────────────
        @GET("promotions/active")
        Call<ApiResponse<List<PromotionResponse>>> getActivePromotions();

        // ── Chatbot ───────────────────────────────────────────────────────────
        @POST("chatbot/chat")
        Call<ApiResponse<Map<String, String>>> chatWithAi(@Body ChatRequest request);

        @POST("chatbot/session/clear")
        Call<ApiResponse<Map<String, String>>> clearChatSession();

        // ── Home Layout ──────────────────────────────────────────────────────────
        @GET("home/featured-movies")
        Call<ApiResponse<List<MovieSummary>>> getFeaturedMovies();

        @GET("home/popup-promotion")
        Call<ApiResponse<PromotionResponse>> getPopupPromotion();

        // ── Staff Dashboard ───────────────────────────────────────────────────
        @GET("staff/dashboard/stats")
        Call<ApiResponse<StaffDashboardStatsResponse>> getStaffDashboardStats();

        @GET("staff/dashboard/upcoming-showtimes")
        Call<ApiResponse<List<UpcomingShowtimeResponse>>> getUpcomingShowtimes();

        @GET("staff/check-in-history")
        Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>> getCheckInHistory(
                        @Query("page") int page,
                        @Query("size") int size);
}
