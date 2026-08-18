package com.cinema.ticket_booking.network

import com.cinema.ticket_booking.BuildConfig
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.data.model.request.RefreshTokenRequest
import java.io.IOException
import javax.inject.Inject
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * OkHttp Authenticator — tự động refresh token khi nhận 401.
 * Sử dụng một Retrofit instance riêng (không có AuthInterceptor)
 * để gọi refresh, tránh vòng lặp vô hạn.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    @Synchronized
    @Throws(IOException::class)
    override fun authenticate(route: Route?, response: Response): Request? {
        // Nếu đã retry 1 lần rồi mà vẫn 401 thì dừng (tránh loop)
        if (responseCount(response) >= 2) {
            tokenManager.clearAll()
            return null
        }

        val refreshToken = tokenManager.refreshToken
        if (refreshToken.isNullOrEmpty()) {
            tokenManager.clearAll()
            return null // Không có refresh token → bắt đăng nhập lại
        }

        // Gọi API refresh bằng Retrofit instance riêng (không gắn token)
        return try {
            val refreshApi = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)

            val refreshResponse = refreshApi.refreshToken(RefreshTokenRequest(refreshToken)).execute()

            if (refreshResponse.isSuccessful && refreshResponse.body() != null && refreshResponse.body()!!.success) {
                val auth = refreshResponse.body()!!.data
                if (auth != null) {
                    tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                    // Retry request gốc với token mới
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${auth.accessToken}")
                        .build()
                } else {
                    tokenManager.clearAll()
                    null
                }
            } else {
                // Refresh thất bại → clear session
                tokenManager.clearAll()
                null
            }
        } catch (e: Exception) {
            tokenManager.clearAll()
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
