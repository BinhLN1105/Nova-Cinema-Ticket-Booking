package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.request.BookingRequest;
import com.cinema.ticket_booking.data.model.request.PaymentRequest;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;
import com.cinema.ticket_booking.BuildConfig;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BookingRepository {

    private final ApiService apiService;

    @Inject
    public BookingRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<Resource<BookingResponse>> createBooking(BookingRequest request) {
        MutableLiveData<Resource<BookingResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.createBooking(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<BookingResponse>> call,
                    Response<ApiResponse<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error(getErrorMessage(response, "Đặt vé thất bại")));
            }

            @Override
            public void onFailure(Call<ApiResponse<BookingResponse>> call, Throwable t) {
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
            public void onResponse(Call<ApiResponse<BookingResponse>> call,
                    Response<ApiResponse<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error(getErrorMessage(response, "Tính toán giá thất bại")));
            }

            @Override
            public void onFailure(Call<ApiResponse<BookingResponse>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<PageResponse<BookingSummary>>> getMyBookings(int page, int size) {
        MutableLiveData<Resource<PageResponse<BookingSummary>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.getMyBookings(page, size).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<BookingSummary>>> call,
                    Response<ApiResponse<PageResponse<BookingSummary>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error(getErrorMessage(response, "Tải lịch sử vé thất bại")));
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<BookingSummary>>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<BookingResponse>> getBookingDetail(String id) {
        MutableLiveData<Resource<BookingResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.getBookingDetail(id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<BookingResponse>> call,
                    Response<ApiResponse<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error(getErrorMessage(response, "Không tìm thấy vé")));
            }

            @Override
            public void onFailure(Call<ApiResponse<BookingResponse>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<CheckInResponse>> checkIn(String qrCode) {
        MutableLiveData<Resource<CheckInResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        apiService.checkIn(qrCode).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<CheckInResponse>> call,
                    Response<ApiResponse<CheckInResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error(getErrorMessage(response, "Kiểm tra vé thất bại")));
            }

            @Override
            public void onFailure(Call<ApiResponse<CheckInResponse>> call, Throwable t) {
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
            public void onResponse(Call<ApiResponse<PaymentResponse>> call,
                    Response<ApiResponse<PaymentResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error(getErrorMessage(response, "Tạo thanh toán thất bại")));
            }

            @Override
            public void onFailure(Call<ApiResponse<PaymentResponse>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
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
