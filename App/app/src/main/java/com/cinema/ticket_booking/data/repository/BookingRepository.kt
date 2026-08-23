package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.BuildConfig
import com.cinema.ticket_booking.data.local.dao.BookingDao
import com.cinema.ticket_booking.data.local.entity.BookingEntity
import com.cinema.ticket_booking.data.model.request.BookingRequest
import com.cinema.ticket_booking.data.model.request.PaymentRequest
import com.cinema.ticket_booking.data.model.response.*
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class BookingRepository @Inject constructor(
    private val apiService: ApiService,
    private val bookingDao: BookingDao
) {
    private val gson = Gson()

    // Method to clear expired offline tickets
    fun cleanupExpiredBookings() {
        Executors.newSingleThreadExecutor().execute {
            bookingDao.deleteExpiredBookings()
        }
    }

    fun createBooking(request: BookingRequest): LiveData<Resource<BookingResponse>> {
        val result = MutableLiveData<Resource<BookingResponse>>()
        result.value = Resource.loading()
        apiService.createBooking(request).enqueue(object : Callback<ApiResponse<BookingResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<BookingResponse>>,
                response: Response<ApiResponse<BookingResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    val data = response.body()!!.data
                    if (data != null) {
                        cacheBookingAsync(data) // Auto cache
                        result.value = Resource.success(data)
                    } else {
                        result.value = Resource.error("Không nhận được dữ liệu đặt vé")
                    }
                } else {
                    result.value = Resource.error(getErrorMessage(response, "Đặt vé thất bại"))
                }
            }

            override fun onFailure(call: Call<ApiResponse<BookingResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun getBookingQuote(request: BookingRequest): LiveData<Resource<BookingResponse>> {
        val result = MutableLiveData<Resource<BookingResponse>>()
        result.value = Resource.loading()
        apiService.getBookingQuote(request).enqueue(object : Callback<ApiResponse<BookingResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<BookingResponse>>,
                response: Response<ApiResponse<BookingResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error(getErrorMessage(response, "Tính toán giá thất bại"))
                }
            }

            override fun onFailure(call: Call<ApiResponse<BookingResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun getDraftBooking(draftId: String): LiveData<Resource<DraftBookingResponse>> {
        val result = MutableLiveData<Resource<DraftBookingResponse>>()
        result.value = Resource.loading()
        apiService.getDraftBooking(draftId).enqueue(object : Callback<ApiResponse<DraftBookingResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<DraftBookingResponse>>,
                response: Response<ApiResponse<DraftBookingResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    val data = response.body()!!.data
                    if (data != null) {
                        result.value = Resource.success(data)
                    } else {
                        result.value = Resource.error("Không tìm thấy thông tin vé nháp")
                    }
                } else {
                    result.value = Resource.error(getErrorMessage(response, "Đơn vé nháp không tồn tại hoặc đã hết hạn (tối đa 10 phút)"))
                }
            }

            override fun onFailure(call: Call<ApiResponse<DraftBookingResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun getMyBookings(page: Int, size: Int): LiveData<Resource<PageResponse<BookingSummary>>> {
        val result = MediatorLiveData<Resource<PageResponse<BookingSummary>>>()
        result.value = Resource.loading()

        // 1. Fetch from Cache immediately
        Executors.newSingleThreadExecutor().execute { this.cleanupExpiredBookings() }
        val dbSource = bookingDao.getAllBookings()

        result.addSource(dbSource) { entities ->
            if (entities != null && entities.isNotEmpty()) {
                val summaries = ArrayList<BookingSummary>()
                for (e in entities) {
                    val summary = BookingSummary().apply {
                        id = e.id
                        bookingCode = e.bookingCode
                        movieTitle = e.movieTitle
                        moviePosterUrl = e.moviePosterUrl
                        startTime = e.startTime
                        cinemaName = e.cinemaName
                        screenName = e.screenName
                        screenType = e.screenType
                        totalAmount = e.totalAmount
                        status = e.status
                        createdAt = e.createdAt
                        expiresAt = e.expiresAt
                    }
                    summaries.add(summary)
                }
                val pageResp = PageResponse<BookingSummary>().apply {
                    content = summaries
                    last = true // Offline only shows what's cached
                }

                // If we haven't successfully loaded from network yet, post DB data
                val current = result.value
                if (current == null || !current.isSuccess) {
                    result.value = Resource.success(pageResp)
                }
            }
        }

        // 2. Fetch from Network
        apiService.getMyBookings(page, size).enqueue(object : Callback<ApiResponse<PageResponse<BookingSummary>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<BookingSummary>>>,
                response: Response<ApiResponse<PageResponse<BookingSummary>>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    val current = result.value
                    if (current?.data?.content.isNullOrEmpty()) {
                        result.value = Resource.error(getErrorMessage(response, "Tải lịch sử thất bại"))
                    } else {
                        // Network error but we have local cache
                        result.value = Resource.error("Lỗi mạng, dùng dữ liệu đã lưu", current?.data)
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<BookingSummary>>>, t: Throwable) {
                val current = result.value
                if (current?.data?.content.isNullOrEmpty()) {
                    result.value = Resource.error("Bạn đang xem ở chế độ Offline.")
                } else {
                    result.value = Resource.error("Lỗi kết nối, đang xem dữ liệu Offline", current?.data)
                }
            }
        })
        return result
    }

    fun getBookingDetail(id: String): LiveData<Resource<BookingResponse>> {
        val result = MediatorLiveData<Resource<BookingResponse>>()
        result.value = Resource.loading()

        val dbSource = bookingDao.getBookingById(id)
        result.addSource(dbSource) { e ->
            if (e != null) {
                val mapped = mapToResponse(e)
                val current = result.value
                if (current == null || !current.isSuccess) {
                    result.value = Resource.success(mapped)
                }
            }
        }

        apiService.getBookingDetail(id).enqueue(object : Callback<ApiResponse<BookingResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<BookingResponse>>,
                response: Response<ApiResponse<BookingResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    val data = response.body()!!.data
                    if (data != null) {
                        cacheBookingAsync(data) // update cache with fresh data
                        result.value = Resource.success(data)
                    } else {
                        result.value = Resource.error("Không nhận được dữ liệu chi tiết vé")
                    }
                } else {
                    val current = result.value
                    if (current == null || !current.isSuccess || current.data == null) {
                        result.value = Resource.error(getErrorMessage(response, "Không tìm thấy vé"))
                    } else {
                        result.value = Resource.error("Lỗi mạng, đang xem dữ liệu chi tiết đã lưu", current.data)
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<BookingResponse>>, t: Throwable) {
                val current = result.value
                if (current == null || current.data == null) {
                    result.value = Resource.error("Mất kết nối mạng. Không tìm thấy chi tiết vé đã lưu.")
                } else {
                    result.value = Resource.error("Đang xem dữ liệu Offline", current.data)
                }
            }
        })
        return result
    }

    fun checkIn(qrCode: String): LiveData<Resource<CheckInResponse>> {
        val result = MutableLiveData<Resource<CheckInResponse>>()
        result.value = Resource.loading()
        apiService.checkIn(qrCode).enqueue(object : Callback<ApiResponse<CheckInResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<CheckInResponse>>,
                response: Response<ApiResponse<CheckInResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                } else {
                    result.value = Resource.error(getErrorMessage(response, "Kiểm tra vé thất bại"))
                }
            }

            override fun onFailure(call: Call<ApiResponse<CheckInResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun createPayment(bookingId: String): LiveData<Resource<PaymentResponse>> {
        val result = MutableLiveData<Resource<PaymentResponse>>()
        result.value = Resource.loading()
        val req = PaymentRequest(
            bookingId, "${BuildConfig.BASE_URL}payments/vnpay/callback?source=mobile"
        )
        apiService.createPayment(req).enqueue(object : Callback<ApiResponse<PaymentResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<PaymentResponse>>,
                response: Response<ApiResponse<PaymentResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                    // Start fetching detail from API to cache right after payment success!
                    getBookingDetail(bookingId) // Fire and forget to cache
                } else {
                    result.value = Resource.error(getErrorMessage(response, "Tạo thanh toán thất bại"))
                }
            }

            override fun onFailure(call: Call<ApiResponse<PaymentResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun payWithWallet(bookingId: String): LiveData<Resource<PaymentResponse>> {
        val result = MutableLiveData<Resource<PaymentResponse>>()
        result.value = Resource.loading()
        apiService.payWithWallet(bookingId).enqueue(object : Callback<ApiResponse<PaymentResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<PaymentResponse>>,
                response: Response<ApiResponse<PaymentResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(response.body()!!.data)
                    getBookingDetail(bookingId)
                } else {
                    result.value = Resource.error(getErrorMessage(response, "Thanh toán ví thất bại"))
                }
            }

            override fun onFailure(call: Call<ApiResponse<PaymentResponse>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    fun cancelConfirm(token: String, bookingId: String): LiveData<Resource<Void>> {
        val result = MutableLiveData<Resource<Void>>()
        result.value = Resource.loading()
        apiService.cancelConfirm(token, bookingId).enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                    result.value = Resource.success(null)
                    // Refresh detail to update status
                    getBookingDetail(bookingId)
                } else {
                    result.value = Resource.error(getErrorMessage(response, "Xác nhận huỷ vé thất bại"))
                }
            }

            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                result.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return result
    }

    private fun cacheBookingAsync(res: BookingResponse) {
        Executors.newSingleThreadExecutor().execute {
            val e = BookingEntity().apply {
                id = res.id ?: ""
                bookingCode = res.bookingCode
                status = res.status
                movieTitle = res.movieTitle
                moviePosterUrl = res.moviePosterUrl
                startTime = res.startTime
                cinemaName = res.cinemaName
                cinemaAddress = res.cinemaAddress
                screenName = res.screenName
                screenType = res.screenType
                subtotal = res.subtotal ?: 0.0
                discountAmount = res.discountAmount ?: 0.0
                totalAmount = res.totalAmount ?: 0.0
                totalOriginalAmount = res.totalOriginalAmount ?: 0.0
                promotionDiscountAmount = res.promotionDiscountAmount ?: 0.0
                appliedPromotionName = res.appliedPromotionName
                warningMessage = res.warningMessage
                qrCode = res.qrCode
                expiresAt = res.expiresAt

                if (res.seats != null) seatsJson = gson.toJson(res.seats)
                if (res.combos != null) combosJson = gson.toJson(res.combos)
            }

            // Generate a createdAt if none exists (though usually fetched from History)
            bookingDao.insert(e)
        }
    }

    private fun mapToResponse(e: BookingEntity): BookingResponse {
        return BookingResponse().apply {
            id = e.id
            bookingCode = e.bookingCode
            status = e.status
            movieTitle = e.movieTitle
            moviePosterUrl = e.moviePosterUrl
            startTime = e.startTime
            cinemaName = e.cinemaName
            cinemaAddress = e.cinemaAddress
            screenName = e.screenName
            screenType = e.screenType
            subtotal = e.subtotal
            discountAmount = e.discountAmount
            totalAmount = e.totalAmount
            totalOriginalAmount = e.totalOriginalAmount
            promotionDiscountAmount = e.promotionDiscountAmount
            appliedPromotionName = e.appliedPromotionName
            warningMessage = e.warningMessage
            qrCode = e.qrCode
            expiresAt = e.expiresAt

            if (e.seatsJson != null) {
                val seatsList = gson.fromJson<List<BookingResponse.SeatItem>>(
                    e.seatsJson,
                    object : TypeToken<List<BookingResponse.SeatItem>>() {}.type
                )
                this.seats = seatsList
            }
            if (e.combosJson != null) {
                val combosList = gson.fromJson<List<BookingResponse.ComboItem>>(
                    e.combosJson,
                    object : TypeToken<List<BookingResponse.ComboItem>>() {}.type
                )
                this.combos = combosList
            }
        }
    }

    private fun getErrorMessage(response: Response<*>, defaultMessage: String): String {
        if (response.body() != null && response.body() is ApiResponse<*>) {
            val msg = (response.body() as ApiResponse<*>).message
            if (!msg.isNullOrEmpty()) return msg
        }
        try {
            val errorBody = response.errorBody()
            if (errorBody != null) {
                val errStr = errorBody.string()
                val obj = JSONObject(errStr)
                if (obj.has("message")) return obj.getString("message")
            }
        } catch (e: Exception) {
            // Ignore parse error
        }
        return defaultMessage
    }
}
