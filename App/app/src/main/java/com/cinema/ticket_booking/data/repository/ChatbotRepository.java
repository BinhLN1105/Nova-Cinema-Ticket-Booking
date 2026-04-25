package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cinema.ticket_booking.data.model.request.ChatRequest;
import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class ChatbotRepository {
    private final ApiService apiService;

    @Inject
    public ChatbotRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<Resource<String>> sendMessage(String msg) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        apiService.chatWithAi(new ChatRequest(msg)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, String>>> call,
                    Response<ApiResponse<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String botReply = response.body().getData().get("reply");
                    result.setValue(Resource.success(botReply));
                } else {
                    if (response.code() == 401) {
                        result.setValue(Resource.error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để sử dụng Chatbot!"));
                    } else {
                        result.setValue(Resource.error(response.body() != null
                                ? response.body().getMessage()
                                : "AI Chatbot không phản hồi"));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, String>>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });

        return result;
    }
}

