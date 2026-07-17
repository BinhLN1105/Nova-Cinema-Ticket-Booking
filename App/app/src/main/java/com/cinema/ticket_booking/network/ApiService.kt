package com.cinema.ticket_booking.network

import com.cinema.ticket_booking.data.model.request.*
import com.cinema.ticket_booking.data.model.response.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────────
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<ApiResponse<AuthResponse>>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<AuthResponse>>

    @POST("auth/social-login")
    fun socialLogin(@Body request: SocialLoginRequest): Call<ApiResponse<AuthResponse>>

    @POST("auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<ApiResponse<AuthResponse>>

    @POST("auth/logout")
    fun logout(@Body request: RefreshTokenRequest): Call<ApiResponse<Void>>

    @POST("auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<ApiResponse<Void>>

    @POST("auth/verify-otp")
    fun verifyOtp(@Body body: Map<String, String>): Call<ApiResponse<String>>

    @POST("auth/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<ApiResponse<Void>>

    // ── User ─────────────────────────────────────────────────────────────
    @GET("users/me")
    fun getMyProfile(): Call<ApiResponse<UserResponse>>

    @PATCH("users/me")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<ApiResponse<UserResponse>>

    @PATCH("users/me/notifications")
    fun updateNotificationSettings(@Body request: NotificationSettingsRequest): Call<ApiResponse<UserResponse>>

    @Multipart
    @POST("users/me/avatar")
    fun uploadAvatar(@Part file: MultipartBody.Part): Call<ApiResponse<UserResponse>>

    @PUT("users/me/password")
    fun changePassword(@Body body: Map<String, String>): Call<ApiResponse<Void>>

    @PATCH("users/me/fcm-token")
    fun updateFcmToken(@Query("token") token: String): Call<ApiResponse<Void>>

    // ── Movie ─────────────────────────────────────────────────────────────
    @GET("movies")
    fun getMovies(
        @Query("status") status: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<PageResponse<MovieSummary>>>

    @GET("movies/search")
    fun searchMovies(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<PageResponse<MovieSummary>>>

    @GET("movies/{id}")
    fun getMovieDetail(@Path("id") id: String): Call<ApiResponse<MovieDetail>>

    @GET("movies/{id}/can-review")
    fun canReview(@Path("id") id: String): Call<ApiResponse<CanReviewResponse>>

    @GET("movies/cinema/{cinemaId}")
    fun getNowShowingByCinema(@Path("cinemaId") cinemaId: String): Call<ApiResponse<List<MovieSummary>>>

    @GET("movies/genres")
    fun getAllGenres(): Call<ApiResponse<List<Genre>>>

    // ── Cinema ────────────────────────────────────────────────────────────
    @GET("cinemas")
    fun getCinemas(@Query("city") city: String?): Call<ApiResponse<List<CinemaResponse>>>

    @GET("cinemas/{id}")
    fun getCinemaDetail(@Path("id") id: String): Call<ApiResponse<CinemaResponse>>

    // ── Showtime ──────────────────────────────────────────────────────────
    @GET("showtimes")
    fun getShowtimes(
        @Query("movieId") movieId: String?,
        @Query("cinemaId") cinemaId: String?,
        @Query("date") date: String?
    ): Call<ApiResponse<List<ShowtimeResponse>>>

    @GET("showtimes/{id}/seats")
    fun getSeatMap(@Path("id") showtimeId: String): Call<ApiResponse<SeatMapResponse>>

    // ── Booking ───────────────────────────────────────────────────────────
    @POST("bookings")
    fun createBooking(@Body request: BookingRequest): Call<ApiResponse<BookingResponse>>

    @POST("bookings/quote")
    fun getBookingQuote(@Body request: BookingRequest): Call<ApiResponse<BookingResponse>>

    @GET("bookings/me")
    fun getMyBookings(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<PageResponse<BookingSummary>>>

    @GET("bookings/{id}")
    fun getBookingDetail(@Path("id") id: String): Call<ApiResponse<BookingResponse>>

    @POST("bookings/check-in")
    fun checkIn(@Query("qrCode") qrCode: String): Call<ApiResponse<CheckInResponse>>

    @GET("bookings/cancel-policy")
    fun getCancelPolicy(): Call<ApiResponse<Map<String, Any>>>

    @POST("bookings/{id}/cancel-request")
    fun cancelRequest(@Path("id") id: String): Call<ApiResponse<Void>>

    @POST("bookings/cancel-confirm")
    fun cancelConfirm(
        @Query("token") token: String,
        @Query("bookingId") bookingId: String
    ): Call<ApiResponse<Void>>

    // ── Payment ───────────────────────────────────────────────────────────
    @POST("payments")
    fun createPayment(@Body request: PaymentRequest): Call<ApiResponse<PaymentResponse>>

    @GET("payments/booking/{bookingId}")
    fun getPaymentStatus(@Path("bookingId") bookingId: String): Call<ApiResponse<PaymentResponse>>

    @POST("payments/wallet/{bookingId}")
    fun payWithWallet(@Path("bookingId") bookingId: String): Call<ApiResponse<PaymentResponse>>

    // ── Voucher ───────────────────────────────────────────────────────────
    @GET("vouchers/validate")
    fun validateVoucher(@Query("code") code: String): Call<ApiResponse<VoucherSummary>>

    @GET("users/me/vouchers")
    fun getMyVouchers(): Call<ApiResponse<List<VoucherSummary>>>

    @POST("users/me/vouchers/claim")
    fun claimVoucher(@Body request: ClaimVoucherRequest): Call<ApiResponse<Void>>

    @GET("vouchers/active")
    fun getActiveVouchers(): Call<ApiResponse<List<VoucherSyncResponse>>>

    // ── Combo ─────────────────────────────────────────────────────────────
    @GET("combos")
    fun getCombos(): Call<ApiResponse<List<ComboResponse>>>

    @GET("reviews")
    fun getReviews(
        @Query("movieId") movieId: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("rating") rating: Int?
    ): Call<ApiResponse<PageResponse<ReviewResponse>>>

    @POST("reviews")
    fun createReview(@Body request: ReviewRequest): Call<ApiResponse<ReviewResponse>>

    @PUT("reviews/{id}")
    fun updateReview(
        @Path("id") id: String,
        @Body request: ReviewRequest
    ): Call<ApiResponse<ReviewResponse>>

    // ── GiftCard ─────────────────────────────────────────────────────
    @POST("gift-cards/redeem")
    fun redeemGiftCard(@Body body: Map<String, String>): Call<ApiResponse<GiftCardResponse>>

    @GET("gift-cards/me")
    fun getMyGiftCards(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<PageResponse<GiftCardResponse>>>

    // ── Notification ──────────────────────────────────────────────────────
    @GET("notifications")
    fun getNotifications(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<PageResponse<NotificationResponse>>>

    @GET("notifications/unread-count")
    fun getUnreadCount(): Call<ApiResponse<UnreadCountResponse>>

    @PATCH("notifications/read-all")
    fun markAllAsRead(): Call<ApiResponse<Void>>

    @DELETE("notifications/{id}")
    fun deleteNotification(@Path("id") id: String): Call<ApiResponse<Void>>

    // ── Promotion ─────────────────────────────────────────────────────────
    @GET("promotions/active")
    fun getActivePromotions(): Call<ApiResponse<List<PromotionResponse>>>

    // ── Chatbot ───────────────────────────────────────────────────────────
    @POST("chatbot/chat")
    fun chatWithAi(@Body request: ChatRequest): Call<ApiResponse<Map<String, String>>>

    @POST("chatbot/session/clear")
    fun clearChatSession(): Call<ApiResponse<Map<String, String>>>

    // ── Home Layout ──────────────────────────────────────────────────────────
    @GET("home/featured-movies")
    fun getFeaturedMovies(): Call<ApiResponse<List<MovieSummary>>>

    @GET("home/popup-promotion")
    fun getPopupPromotion(): Call<ApiResponse<PromotionResponse>>

    // ── Staff Dashboard ───────────────────────────────────────────────────
    @GET("staff/dashboard/stats")
    fun getStaffDashboardStats(): Call<ApiResponse<StaffDashboardStatsResponse>>

    @GET("staff/dashboard/upcoming-showtimes")
    fun getUpcomingShowtimes(): Call<ApiResponse<List<UpcomingShowtimeResponse>>>

    @GET("staff/check-in-history")
    fun getCheckInHistory(
        @Query("filter") filter: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<PageResponse<CheckInHistoryItemResponse>>>
}
