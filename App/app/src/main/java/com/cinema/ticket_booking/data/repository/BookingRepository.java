package com.cinema.ticket_booking.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.local.dao.BookingDao;
import com.cinema.ticket_booking.data.local.entity.BookingEntity;
import com.cinema.ticket_booking.data.model.request.BookingRequest;
import com.cinema.ticket_booking.data.model.request.PaymentRequest;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;
import com.cinema.ticket_booking.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@Singleton
public class BookingRepository {

    private final ApiService apiService;
    private final BookingDao bookingDao;
    private final Gson gson = new Gson();

    @Inject
    public BookingRepository(ApiService apiService, BookingDao bookingDao) {
        this.apiService = apiService;
        this.bookingDao = bookingDao;
    }
    
    // Method to clear expired offline tickets
    public void cleanupExpiredBookings() {
        Executors.newSingleThreadExecutor().execute(bookingDao::deleteExpiredBookings);
    }

    public LiveData<Resource<BookingResponse>> createBooking(BookingRequest request) {
        MutableLiveData<Resource<BookingResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.createBooking(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<BookingResponse>> call,
                    @NonNull Response<ApiResponse<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BookingResponse data = response.body().getData();
                    cacheBookingAsync(data); // Auto cache
                    result.setValue(Resource.success(data));
                } else {
                    result.setValue(Resource.error(getErrorMessage(response, "Đặt vé thất bại")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<BookingResponse>> call, @NonNull Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<BookingResponse>> getBookingQuote(BookingRequest request) {
        MutableLiveData<Resource<BookingResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.getBookingQuote(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<BookingResponse>> call,
                    @NonNull Response<ApiResponse<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error(getErrorMessage(response, "Tính toán giá thất bại")));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<BookingResponse>> call, @NonNull Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<PageResponse<BookingSummary>>> getMyBookings(int page, int size) {
        MediatorLiveData<Resource<PageResponse<BookingSummary>>> result = new MediatorLiveData<>();
        result.setValue(Resource.loading());

        // 1. Fetch from Cache immediately
        Executors.newSingleThreadExecutor().execute(this::cleanupExpiredBookings);
        LiveData<List<BookingEntity>> dbSource = bookingDao.getAllBookings();
        
        result.addSource(dbSource, entities -> {
            if (entities != null && !entities.isEmpty()) {
                List<BookingSummary> summaries = new ArrayList<>();
                for (BookingEntity e : entities) {
                    BookingSummary summary = new BookingSummary();
                    summary.setId(e.getId());
                    summary.setBookingCode(e.getBookingCode());
                    summary.setMovieTitle(e.getMovieTitle());
                    summary.setMoviePosterUrl(e.getMoviePosterUrl());
                    summary.setStartTime(e.getStartTime());
                    summary.setCinemaName(e.getCinemaName());
                    summary.setScreenName(e.getScreenName());
                    summary.setScreenType(e.getScreenType());
                    summary.setTotalAmount(e.getTotalAmount());
                    summary.setStatus(e.getStatus());
                    summary.setCreatedAt(e.getCreatedAt());
                    summary.setExpiresAt(e.getExpiresAt());
                    summaries.add(summary);
                }
                PageResponse<BookingSummary> pageResp = new PageResponse<>();
                pageResp.setContent(summaries);
                pageResp.setLast(true); // Offline only shows what's cached
                
                // If we haven't successfully loaded from network yet, post DB data
                Resource<PageResponse<BookingSummary>> current = result.getValue();
                if (current == null || !current.isSuccess()) {
                    result.setValue(Resource.success(pageResp));
                }
            }
        });

        // 2. Fetch from Network
        apiService.getMyBookings(page, size).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PageResponse<BookingSummary>>> call,
                    @NonNull Response<ApiResponse<PageResponse<BookingSummary>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData()));
                } else {
                    Resource<PageResponse<BookingSummary>> current = result.getValue();
                    if (current == null || current.data == null || current.data.getContent() == null || current.data.getContent().isEmpty()) {
                        result.setValue(Resource.error(getErrorMessage(response, "Tải lịch sử thất bại")));
                    } else {
                        // Network error but we have local cache
                        result.setValue(Resource.error("Lỗi mạng, dùng dữ liệu đã lưu", current.data));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PageResponse<BookingSummary>>> call, @NonNull Throwable t) {
                Resource<PageResponse<BookingSummary>> current = result.getValue();
                if (current == null || current.data == null || current.data.getContent() == null || current.data.getContent().isEmpty()) {
                    result.setValue(Resource.error("Bạn đang xem ở chế độ Offline."));
                } else {
                    result.setValue(Resource.error("Lỗi kết nối, đang xem dữ liệu Offline", current.data));
                }
            }
        });
        return result;
    }

    public LiveData<Resource<BookingResponse>> getBookingDetail(String id) {
        MediatorLiveData<Resource<BookingResponse>> result = new MediatorLiveData<>();
        result.setValue(Resource.loading());

        LiveData<BookingEntity> dbSource = bookingDao.getBookingById(id);
        result.addSource(dbSource, e -> {
            if (e != null) {
                BookingResponse mapped = mapToResponse(e);
                Resource<BookingResponse> current = result.getValue();
                if (current == null || !current.isSuccess()) {
                    result.setValue(Resource.success(mapped));
                }
            }
        });

        apiService.getBookingDetail(id).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<BookingResponse>> call,
                    @NonNull Response<ApiResponse<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BookingResponse data = response.body().getData();
                    cacheBookingAsync(data); // update cache with fresh data
                    result.setValue(Resource.success(data));
                } else {
                    Resource<BookingResponse> current = result.getValue();
                    if (current == null || !current.isSuccess() || current.data == null) {
                        result.setValue(Resource.error(getErrorMessage(response, "Không tìm thấy vé")));
                    } else {
                        result.setValue(Resource.error("Lỗi mạng, đang xem dữ liệu chi tiết đã lưu", current.data));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<BookingResponse>> call, @NonNull Throwable t) {
                Resource<BookingResponse> current = result.getValue();
                if (current == null || current.data == null) {
                    result.setValue(Resource.error("Mất kết nối mạng. Không tìm thấy chi tiết vé đã lưu."));
                } else {
                    result.setValue(Resource.error("Đang xem dữ liệu Offline", current.data));
                }
            }
        });
        return result;
    }

    public LiveData<Resource<CheckInResponse>> checkIn(String qrCode) {
        MutableLiveData<Resource<CheckInResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.checkIn(qrCode).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<CheckInResponse>> call,
                    @NonNull Response<ApiResponse<CheckInResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error(getErrorMessage(response, "Kiểm tra vé thất bại")));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<CheckInResponse>> call, @NonNull Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<PaymentResponse>> createPayment(String bookingId) {
        MutableLiveData<Resource<PaymentResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        PaymentRequest req = new PaymentRequest(
                bookingId, BuildConfig.BASE_URL + "payments/vnpay/callback?source=mobile");
        apiService.createPayment(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PaymentResponse>> call,
                    @NonNull Response<ApiResponse<PaymentResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData()));
                    // Start fetching detail from API to cache right after payment success!
                    getBookingDetail(bookingId); // Fire and forget to cache
                }
                else
                    result.setValue(Resource.error(getErrorMessage(response, "Tạo thanh toán thất bại")));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PaymentResponse>> call, @NonNull Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<PaymentResponse>> payWithWallet(String bookingId) {
        MutableLiveData<Resource<PaymentResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.payWithWallet(bookingId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PaymentResponse>> call,
                    @NonNull Response<ApiResponse<PaymentResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData()));
                    getBookingDetail(bookingId);
                } else {
                    result.setValue(Resource.error(getErrorMessage(response, "Thanh toán ví thất bại")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PaymentResponse>> call, @NonNull Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<Void>> cancelConfirm(String token, String bookingId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.cancelConfirm(token, bookingId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                    @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(null));
                    // Refresh detail to update status
                    getBookingDetail(bookingId);
                } else {
                    result.setValue(Resource.error(getErrorMessage(response, "Xác nhận huỷ vé thất bại")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    private void cacheBookingAsync(BookingResponse res) {
        Executors.newSingleThreadExecutor().execute(() -> {
            BookingEntity e = new BookingEntity();
            e.setId(res.getId() != null ? res.getId() : "");
            e.setBookingCode(res.getBookingCode());
            e.setStatus(res.getStatus());
            e.setMovieTitle(res.getMovieTitle());
            e.setMoviePosterUrl(res.getMoviePosterUrl());
            e.setStartTime(res.getStartTime());
            e.setCinemaName(res.getCinemaName());
            e.setCinemaAddress(res.getCinemaAddress());
            e.setScreenName(res.getScreenName());
            e.setScreenType(res.getScreenType());
            e.setSubtotal(res.getSubtotal());
            e.setDiscountAmount(res.getDiscountAmount());
            e.setTotalAmount(res.getTotalAmount());
            e.setTotalOriginalAmount(res.getTotalOriginalAmount());
            e.setPromotionDiscountAmount(res.getPromotionDiscountAmount());
            e.setAppliedPromotionName(res.getAppliedPromotionName());
            e.setWarningMessage(res.getWarningMessage());
            e.setQrCode(res.getQrCode());
            e.setExpiresAt(res.getExpiresAt());
            
            if (res.getSeats() != null) e.setSeatsJson(gson.toJson(res.getSeats()));
            if (res.getCombos() != null) e.setCombosJson(gson.toJson(res.getCombos()));
            
            // Generate a createdAt if none exists (though usually fetched from History)
            bookingDao.insert(e);
        });
    }

    private BookingResponse mapToResponse(BookingEntity e) {
        BookingResponse r = new BookingResponse();
        r.setId(e.getId());
        r.setBookingCode(e.getBookingCode());
        r.setStatus(e.getStatus());
        r.setMovieTitle(e.getMovieTitle());
        r.setMoviePosterUrl(e.getMoviePosterUrl());
        r.setStartTime(e.getStartTime());
        r.setCinemaName(e.getCinemaName());
        r.setCinemaAddress(e.getCinemaAddress());
        r.setScreenName(e.getScreenName());
        r.setScreenType(e.getScreenType());
        r.setSubtotal(e.getSubtotal());
        r.setDiscountAmount(e.getDiscountAmount());
        r.setTotalAmount(e.getTotalAmount());
        r.setTotalOriginalAmount(e.getTotalOriginalAmount());
        r.setPromotionDiscountAmount(e.getPromotionDiscountAmount());
        r.setAppliedPromotionName(e.getAppliedPromotionName());
        r.setWarningMessage(e.getWarningMessage());
        r.setQrCode(e.getQrCode());
        r.setExpiresAt(e.getExpiresAt());
        
        if (e.getSeatsJson() != null) {
            List<BookingResponse.SeatItem> seats = gson.fromJson(e.getSeatsJson(), new TypeToken<List<BookingResponse.SeatItem>>(){}.getType());
            r.setSeats(seats);
        }
        if (e.getCombosJson() != null) {
            List<BookingResponse.ComboItem> combos = gson.fromJson(e.getCombosJson(), new TypeToken<List<BookingResponse.ComboItem>>(){}.getType());
            r.setCombos(combos);
        }
        return r;
    }

    private String getErrorMessage(Response<?> response, String defaultMessage) {
        if (response.body() != null && response.body() instanceof ApiResponse) {
            String msg = ((ApiResponse<?>) response.body()).getMessage();
            if (msg != null && !msg.isEmpty()) return msg;
        }
        try {
            if (response.errorBody() != null) {
                String errStr = response.errorBody().string();
                org.json.JSONObject obj = new org.json.JSONObject(errStr);
                if (obj.has("message")) return obj.getString("message");
            }
        } catch (Exception e) {
            // Ignore parse error
        }
        return defaultMessage;
    }
}

