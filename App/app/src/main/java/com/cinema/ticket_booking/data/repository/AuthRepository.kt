package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.data.model.request.*
import com.cinema.ticket_booking.data.model.response.*
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    // ── Register ──────────────────────────────────────────────────────────

    fun register(request: RegisterRequest): LiveData<Resource<AuthResponse>> {
        val result = MutableLiveData<Resource<AuthResponse>>()
        result.value = Resource.loading()

        apiService.register(request).enqueue(object : Callback<ApiResponse<AuthResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<AuthResponse>>,
                response: Response<ApiResponse<AuthResponse>>
            ) {
                handleAuthResponse(response, result)
            }

            override fun onFailure(call: Call<ApiResponse<AuthResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    // ── Login LOCAL ───────────────────────────────────────────────────────

    fun login(request: LoginRequest): LiveData<Resource<AuthResponse>> {
        val result = MutableLiveData<Resource<AuthResponse>>()
        result.value = Resource.loading()

        apiService.login(request).enqueue(object : Callback<ApiResponse<AuthResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<AuthResponse>>,
                response: Response<ApiResponse<AuthResponse>>
            ) {
                handleAuthResponse(response, result)
            }

            override fun onFailure(call: Call<ApiResponse<AuthResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    // ── Social Login ──────────────────────────────────────────────────────

    fun socialLogin(request: SocialLoginRequest): LiveData<Resource<AuthResponse>> {
        val result = MutableLiveData<Resource<AuthResponse>>()
        result.value = Resource.loading()

        apiService.socialLogin(request).enqueue(object : Callback<ApiResponse<AuthResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<AuthResponse>>,
                response: Response<ApiResponse<AuthResponse>>
            ) {
                handleAuthResponse(response, result)
            }

            override fun onFailure(call: Call<ApiResponse<AuthResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    // ── Logout ────────────────────────────────────────────────────────────

    fun logout() {
        val refreshToken = tokenManager.refreshToken
        if (refreshToken != null) {
            apiService.logout(RefreshTokenRequest(refreshToken)).enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {}
                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {}
            })
        }
        tokenManager.clearAll()
    }

    // ── Forgot/Reset Password ─────────────────────────────────────────────

    fun forgotPassword(email: String): LiveData<Resource<Void>> {
        val result = MutableLiveData<Resource<Void>>()
        result.value = Resource.loading()

        apiService.forgotPassword(ForgotPasswordRequest(email)).enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(null)
                } else {
                    result.value = Resource.error(getErrorMessage(response))
                }
            }

            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun verifyOtp(email: String, otp: String): LiveData<Resource<String>> {
        val result = MutableLiveData<Resource<String>>()
        result.value = Resource.loading()

        val body = HashMap<String, String>().apply {
            put("email", email)
            put("otp", otp)
        }

        apiService.verifyOtp(body).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data) // Returns resetToken
                } else {
                    result.value = Resource.error(getErrorMessage(response))
                }
            }

            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun resetPassword(token: String, newPassword: String): LiveData<Resource<Void>> {
        val result = MutableLiveData<Resource<Void>>()
        result.value = Resource.loading()

        apiService.resetPassword(ResetPasswordRequest(token, newPassword)).enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(null)
                } else {
                    result.value = Resource.error(getErrorMessage(response))
                }
            }

            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private fun handleAuthResponse(
        response: Response<ApiResponse<AuthResponse>>,
        result: MutableLiveData<Resource<AuthResponse>>
    ) {
        if (response.isSuccessful && response.body() != null && response.body()!!.success) {
            val auth = response.body()!!.data
            if (auth != null) {
                // Lưu token và thông tin user vào EncryptedSharedPreferences
                tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                val user = auth.user
                if (user != null) {
                    tokenManager.saveUserInfo(
                        user.id,
                        user.email,
                        user.fullName,
                        user.role,
                        user.avatarUrl
                    )
                }
                result.value = Resource.success(auth)
            } else {
                result.value = Resource.error("Không nhận được dữ liệu xác thực")
            }
        } else {
            result.value = Resource.error(getErrorMessage(response))
        }
    }

    private fun getErrorMessage(response: Response<*>): String {
        val errorBody = response.errorBody()
        if (errorBody != null) {
            try {
                val errorResponse = Gson().fromJson(errorBody.charStream(), ApiResponse::class.java)
                if (errorResponse?.message != null) {
                    return errorResponse.message
                }
            } catch (e: Exception) {
                return "Lỗi hệ thống (${response.code()})"
            }
        }
        val body = response.body()
        if (body is ApiResponse<*>) {
            return body.message ?: "Đã xảy ra lỗi, vui lòng thử lại"
        }
        return "Đã xảy ra lỗi, vui lòng thử lại"
    }
}
