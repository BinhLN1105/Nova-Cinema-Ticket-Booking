package com.cinema.ticket_booking.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.data.model.request.*;
import com.cinema.ticket_booking.data.model.response.*;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.util.Resource;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.HashMap;

@Singleton
public class AuthRepository {

    private final ApiService apiService;
    private final TokenManager tokenManager;

    @Inject
    public AuthRepository(ApiService apiService, TokenManager tokenManager) {
        this.apiService = apiService;
        this.tokenManager = tokenManager;
    }

    // ── Register ──────────────────────────────────────────────────────────

    public LiveData<Resource<AuthResponse>> register(RegisterRequest request) {
        MutableLiveData<Resource<AuthResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        apiService.register(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                    Response<ApiResponse<AuthResponse>> response) {
                handleAuthResponse(response, result);
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    // ── Login LOCAL ───────────────────────────────────────────────────────

    public LiveData<Resource<AuthResponse>> login(LoginRequest request) {
        MutableLiveData<Resource<AuthResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        apiService.login(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                    Response<ApiResponse<AuthResponse>> response) {
                handleAuthResponse(response, result);
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    // ── Social Login ──────────────────────────────────────────────────────

    public LiveData<Resource<AuthResponse>> socialLogin(SocialLoginRequest request) {
        MutableLiveData<Resource<AuthResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        apiService.socialLogin(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                    Response<ApiResponse<AuthResponse>> response) {
                handleAuthResponse(response, result);
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    // ── Logout ────────────────────────────────────────────────────────────

    public void logout() {
        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken != null) {
            apiService.logout(new RefreshTokenRequest(refreshToken)).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call,
                        Response<ApiResponse<Void>> response) {
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                }
            });
        }
        tokenManager.clearAll();
    }

    // ── Forgot/Reset Password ─────────────────────────────────────────────

    public LiveData<Resource<Void>> forgotPassword(String email) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        apiService.forgotPassword(new ForgotPasswordRequest(email)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(null));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Gửi email thất bại";
                    result.setValue(Resource.error(msg));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<String>> verifyOtp(String email, String otp) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("otp", otp);

        apiService.verifyOtp(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData())); // Returns resetToken
                } else {
                    String msg = "Xác thực OTP thất bại";
                    try {
                        if (response.errorBody() != null) {
                            String errorMsg = response.errorBody().string();
                            if (errorMsg.contains("message")) {
                                msg = errorMsg.substring(errorMsg.indexOf("message") + 10);
                                msg = msg.substring(0, msg.indexOf("\""));
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    result.setValue(Resource.error(msg));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<Void>> resetPassword(String token, String newPassword) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        apiService.resetPassword(new ResetPasswordRequest(token, newPassword)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(null));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Đặt lại mật khẩu thất bại";
                    result.setValue(Resource.error(msg));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
        return result;
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private void handleAuthResponse(Response<ApiResponse<AuthResponse>> response,
            MutableLiveData<Resource<AuthResponse>> result) {
        if (response.isSuccessful() && response.body() != null
                && response.body().isSuccess()) {
            AuthResponse auth = response.body().getData();
            // Lưu token và thông tin user vào EncryptedSharedPreferences
            tokenManager.saveTokens(auth.getAccessToken(), auth.getRefreshToken());
            tokenManager.saveUserInfo(
                    auth.getUser().getId(),
                    auth.getUser().getEmail(),
                    auth.getUser().getFullName(),
                    auth.getUser().getRole(),
                    auth.getUser().getAvatarUrl());
            result.setValue(Resource.success(auth));
        } else {
            String msg = response.body() != null
                    ? response.body().getMessage()
                    : "Đã xảy ra lỗi, vui lòng thử lại";
            result.setValue(Resource.error(msg));
        }
    }
}
