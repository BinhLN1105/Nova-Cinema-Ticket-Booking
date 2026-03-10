package com.cinema.ticket_booking.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.data.model.request.*;
import com.cinema.ticket_booking.data.model.response.AuthResponse;
import com.cinema.ticket_booking.data.repository.AuthRepository;
import com.cinema.ticket_booking.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final TokenManager   tokenManager;

    // LiveData các màn hình observe
    private final MutableLiveData<Resource<AuthResponse>> authResult = new MutableLiveData<>();

    @Inject
    public AuthViewModel(AuthRepository authRepository, TokenManager tokenManager) {
        this.authRepository = authRepository;
        this.tokenManager   = tokenManager;
    }

    public LiveData<Resource<AuthResponse>> getAuthResult() {
        return authResult;
    }

    // ── Actions ───────────────────────────────────────────────────────────

    public void register(String email, String password, String fullName, String phone) {
        RegisterRequest request = new RegisterRequest(email, password, fullName, phone);
        authRepository.register(request).observeForever(result ->
                authResult.setValue(result));
    }

    public void login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        authRepository.login(request).observeForever(result ->
                authResult.setValue(result));
    }

    public void loginWithGoogle(String idToken) {
        SocialLoginRequest request = new SocialLoginRequest(idToken, "GOOGLE");
        authRepository.socialLogin(request).observeForever(result ->
                authResult.setValue(result));
    }

    public void loginWithFacebook(String accessToken) {
        SocialLoginRequest request = new SocialLoginRequest(accessToken, "FACEBOOK");
        authRepository.socialLogin(request).observeForever(result ->
                authResult.setValue(result));
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
