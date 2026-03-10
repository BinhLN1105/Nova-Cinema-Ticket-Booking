package com.cinema.ticket_booking.network;

import com.cinema.ticket_booking.data.local.TokenManager;
import java.io.IOException;
import javax.inject.Inject;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor — tự động gắn "Authorization: Bearer {token}"
 * vào mỗi request gửi lên backend.
 */
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    @Inject
    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = tokenManager.getAccessToken();

        Request original = chain.request();

        // Không gắn token cho các endpoint public (auth/*)
        if (token == null || original.url().encodedPath().contains("/auth/")) {
            return chain.proceed(original);
        }

        Request authenticated = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(authenticated);
    }
}
