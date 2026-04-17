package com.cinema.ticket_booking.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.data.model.request.*;
import com.cinema.ticket_booking.data.model.response.AuthResponse;
import com.cinema.ticket_booking.data.repository.AuthRepository;
import com.cinema.ticket_booking.util.Resource;
import com.cinema.ticket_booking.util.Resource.Status;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final TokenManager tokenManager;

    // LiveData các màn hình observe
    private final MediatorLiveData<Resource<AuthResponse>> authResult = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<Void>> passwordResult = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<String>> verifyOtpResult = new MediatorLiveData<>();

    @Inject
    public AuthViewModel(AuthRepository authRepository, TokenManager tokenManager) {
        this.authRepository = authRepository;
        this.tokenManager = tokenManager;
    }

    public LiveData<Resource<AuthResponse>> getAuthResult() {
        return authResult;
    }

    public LiveData<Resource<Void>> getPasswordResult() {
        return passwordResult;
    }

    public LiveData<Resource<String>> getVerifyOtpResult() {
        return verifyOtpResult;
    }

    // ── Actions ───────────────────────────────────────────────────────────

    public void forgotPassword(String email) {
        LiveData<Resource<Void>> source = authRepository.forgotPassword(email);
        passwordResult.addSource(source, result -> {
            passwordResult.setValue(result);
            if (result.status != Status.LOADING) {
                passwordResult.removeSource(source);
            }
        });
    }

    public void verifyOtp(String email, String otp) {
        LiveData<Resource<String>> source = authRepository.verifyOtp(email, otp);
        verifyOtpResult.addSource(source, result -> {
            verifyOtpResult.setValue(result);
            if (result.status != Status.LOADING) {
                verifyOtpResult.removeSource(source);
            }
        });
    }

    public void resetPassword(String token, String newPassword) {
        LiveData<Resource<Void>> source = authRepository.resetPassword(token, newPassword);
        passwordResult.addSource(source, result -> {
            passwordResult.setValue(result);
            if (result.status != Status.LOADING) {
                passwordResult.removeSource(source);
            }
        });
    }

    public void register(String email, String password, String fullName) {
        RegisterRequest request = new RegisterRequest(email, password, fullName);
        LiveData<Resource<AuthResponse>> source = authRepository.register(request);
        authResult.addSource(source, result -> {
            authResult.setValue(result);
            if (result.status != Status.LOADING) {
                authResult.removeSource(source);
            }
        });
    }

    public void login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        LiveData<Resource<AuthResponse>> source = authRepository.login(request);
        authResult.addSource(source, result -> {
            authResult.setValue(result);
            if (result.status != Status.LOADING) {
                authResult.removeSource(source);
            }
        });
    }

    public void loginWithGoogle(String idToken) {
        SocialLoginRequest request = new SocialLoginRequest(idToken, "GOOGLE");
        LiveData<Resource<AuthResponse>> source = authRepository.socialLogin(request);
        authResult.addSource(source, result -> {
            authResult.setValue(result);
            if (result.status != Status.LOADING) {
                authResult.removeSource(source);
            }
        });
    }

    public void loginWithFacebook(String accessToken) {
        SocialLoginRequest request = new SocialLoginRequest(accessToken, "FACEBOOK");
        LiveData<Resource<AuthResponse>> source = authRepository.socialLogin(request);
        authResult.addSource(source, result -> {
            authResult.setValue(result);
            if (result.status != Status.LOADING) {
                authResult.removeSource(source);
            }
        });
    }

    public void logout() {
        authRepository.logout();
    }

    // ── State helpers ─────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return tokenManager.isLoggedIn();
    }

    public String getCurrentUserName() {
        return tokenManager.getUserName();
    }
}

