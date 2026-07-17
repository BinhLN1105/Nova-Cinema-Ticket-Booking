package com.cinema.ticket_booking.network

import com.cinema.ticket_booking.data.local.TokenManager
import java.io.IOException
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor — tự động gắn "Authorization: Bearer {token}"
 * vào mỗi request gửi lên backend.
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.accessToken
        val original = chain.request()

        // Không gắn token cho các endpoint public (auth/*)
        if (token == null || original.url.encodedPath.contains("/auth/")) {
            return chain.proceed(original)
        }

        val authenticated = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticated)
    }
}
