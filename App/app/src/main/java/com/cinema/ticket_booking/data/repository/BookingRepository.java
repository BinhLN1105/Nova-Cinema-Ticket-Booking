package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.request.BookingRequest;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;
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
                    result.setValue(Resource.error(response.body() != null
                            ? response.body().getMessage()
                            : "Đặt vé thất bại"));
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
                    result.setValue(Resource.error("Tải lịch sử vé thất bại"));
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
                    result.setValue(Resource.error("Không tìm thấy vé"));
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
                    result.setValue(Resource.error(response.body() != null
                            ? response.body().getMessage()
                            : "Kiểm tra vé thất bại"));
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
        com.cinema.ticket_booking.data.model.request.PaymentRequest req = new com.cinema.ticket_booking.data.model.request.PaymentRequest(
                bookingId, "cinema://payment/result");
        apiService.createPayment(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<PaymentResponse>> call,
                    Response<ApiResponse<PaymentResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                    result.setValue(Resource.success(response.body().getData()));
                else
                    result.setValue(Resource.error("Tạo thanh toán thất bại"));
            }

            @Override
            public void onFailure(Call<ApiResponse<PaymentResponse>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }
}
