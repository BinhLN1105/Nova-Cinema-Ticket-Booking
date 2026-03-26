package com.cinema.ticket_booking.network;

import com.cinema.ticket_booking.BuildConfig;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.data.model.request.RefreshTokenRequest;
import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.data.model.response.AuthResponse;

import java.io.IOException;
import javax.inject.Inject;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * OkHttp Authenticator — tự động refresh token khi nhận 401.
 * Sử dụng một Retrofit instance riêng (không có AuthInterceptor)
 * để gọi refresh, tránh vòng lặp vô hạn.
 */
public class TokenAuthenticator implements Authenticator {

    private final TokenManager tokenManager;

    @Inject
    public TokenAuthenticator(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public synchronized Request authenticate(Route route, Response response) throws IOException {
        // Nếu đã retry 1 lần rồi mà vẫn 401 thì dừng (tránh loop)
        if (responseCount(response) >= 2) {
            tokenManager.clearAll();
            return null;
        }

        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            tokenManager.clearAll();
            return null; // Không có refresh token → bắt đăng nhập lại
        }

        // Gọi API refresh bằng Retrofit instance riêng (không gắn token)
        try {
            ApiService refreshApi = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);

            retrofit2.Response<ApiResponse<AuthResponse>> refreshResponse =
                    refreshApi.refreshToken(new RefreshTokenRequest(refreshToken)).execute();

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null
                    && refreshResponse.body().isSuccess()) {
                AuthResponse auth = refreshResponse.body().getData();
                tokenManager.saveTokens(auth.getAccessToken(), auth.getRefreshToken());

                // Retry request gốc với token mới
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + auth.getAccessToken())
                        .build();
            } else {
                // Refresh thất bại → clear session
                tokenManager.clearAll();
                return null;
            }
        } catch (Exception e) {
            tokenManager.clearAll();
            return null;
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
