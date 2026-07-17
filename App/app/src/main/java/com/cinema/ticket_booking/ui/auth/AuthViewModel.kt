package com.cinema.ticket_booking.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.data.model.request.*
import com.cinema.ticket_booking.data.model.response.AuthResponse
import com.cinema.ticket_booking.data.repository.AuthRepository
import com.cinema.ticket_booking.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // LiveData các màn hình observe
    private val authResult = MediatorLiveData<Resource<AuthResponse>>()
    private val passwordResult = MediatorLiveData<Resource<Void>>()
    private val verifyOtpResult = MediatorLiveData<Resource<String>>()

    fun getAuthResult(): LiveData<Resource<AuthResponse>> = authResult
    fun getPasswordResult(): LiveData<Resource<Void>> = passwordResult
    fun getVerifyOtpResult(): LiveData<Resource<String>> = verifyOtpResult

    // ── Actions ───────────────────────────────────────────────────────────

    fun forgotPassword(email: String) {
        val source = authRepository.forgotPassword(email)
        passwordResult.addSource(source) { result ->
            passwordResult.value = result
            if (result.status != Resource.Status.LOADING) {
                passwordResult.removeSource(source)
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        val source = authRepository.verifyOtp(email, otp)
        verifyOtpResult.addSource(source) { result ->
            verifyOtpResult.value = result
            if (result.status != Resource.Status.LOADING) {
                verifyOtpResult.removeSource(source)
            }
        }
    }

    fun resetPassword(token: String, newPassword: String) {
        val source = authRepository.resetPassword(token, newPassword)
        passwordResult.addSource(source) { result ->
            passwordResult.value = result
            if (result.status != Resource.Status.LOADING) {
                passwordResult.removeSource(source)
            }
        }
    }

    fun register(email: String, password: String, fullName: String) {
        val request = RegisterRequest(email, password, fullName)
        val source = authRepository.register(request)
        authResult.addSource(source) { result ->
            authResult.value = result
            if (result.status != Resource.Status.LOADING) {
                authResult.removeSource(source)
            }
        }
    }

    fun login(email: String, password: String) {
        val request = LoginRequest(email, password)
        val source = authRepository.login(request)
        authResult.addSource(source) { result ->
            authResult.value = result
            if (result.status != Resource.Status.LOADING) {
                authResult.removeSource(source)
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        val request = SocialLoginRequest(idToken, "GOOGLE")
        val source = authRepository.socialLogin(request)
        authResult.addSource(source) { result ->
            authResult.value = result
            if (result.status != Resource.Status.LOADING) {
                authResult.removeSource(source)
            }
        }
    }

    fun loginWithFacebook(accessToken: String) {
        val request = SocialLoginRequest(accessToken, "FACEBOOK")
        val source = authRepository.socialLogin(request)
        authResult.addSource(source) { result ->
            authResult.value = result
            if (result.status != Resource.Status.LOADING) {
                authResult.removeSource(source)
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    // ── State helpers ─────────────────────────────────────────────────────

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn

    fun getCurrentUserName(): String? = tokenManager.userName
}
