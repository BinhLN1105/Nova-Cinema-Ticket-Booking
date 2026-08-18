package com.cinema.ticket_booking.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cinema.ticket_booking.data.model.request.ClaimVoucherRequest
import com.cinema.ticket_booking.data.model.response.*
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.Resource
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class VoucherRepository @Inject constructor(
    private val api: ApiService
) {

    fun validateVoucher(code: String): LiveData<Resource<VoucherSummary>> {
        val r = MutableLiveData<Resource<VoucherSummary>>()
        r.value = Resource.loading()
        api.validateVoucher(code).enqueue(object : Callback<ApiResponse<VoucherSummary>> {
            override fun onResponse(c: Call<ApiResponse<VoucherSummary>>, res: Response<ApiResponse<VoucherSummary>>) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error(
                        if (res.body() != null) res.body()!!.message else "Voucher không hợp lệ"
                    )
                }
            }

            override fun onFailure(c: Call<ApiResponse<VoucherSummary>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }

    fun getActiveVouchers(): LiveData<Resource<List<VoucherSyncResponse>>> {
        val r = MutableLiveData<Resource<List<VoucherSyncResponse>>>()
        r.value = Resource.loading()
        api.getActiveVouchers().enqueue(object : Callback<ApiResponse<List<VoucherSyncResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<VoucherSyncResponse>>>,
                res: Response<ApiResponse<List<VoucherSyncResponse>>>
            ) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error(
                        if (res.body() != null) res.body()!!.message else "Không lấy được danh sách khuyến mãi"
                    )
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<VoucherSyncResponse>>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }

    fun getMyVouchers(): LiveData<Resource<List<VoucherSummary>>> {
        val r = MutableLiveData<Resource<List<VoucherSummary>>>()
        r.value = Resource.loading()
        api.getMyVouchers().enqueue(object : Callback<ApiResponse<List<VoucherSummary>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<VoucherSummary>>>,
                res: Response<ApiResponse<List<VoucherSummary>>>
            ) {
                if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                    r.value = Resource.success(res.body()!!.data)
                } else {
                    r.value = Resource.error(
                        if (res.body() != null) res.body()!!.message else "Không lấy được danh sách khuyến mãi"
                    )
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<VoucherSummary>>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }

    fun claimVoucher(code: String): LiveData<Resource<Void>> {
        val r = MutableLiveData<Resource<Void>>()
        r.value = Resource.loading()
        api.claimVoucher(ClaimVoucherRequest(code)).enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(c: Call<ApiResponse<Void>>, res: Response<ApiResponse<Void>>) {
                if (res.isSuccessful) {
                    r.value = Resource.success(null)
                } else {
                    var errorMsg = "Mã không hợp lệ"
                    try {
                        val errorBody = res.errorBody()
                        if (errorBody != null) {
                            val bodyStr = errorBody.string()
                            val obj = JsonParser.parseString(bodyStr).asJsonObject
                            if (obj.has("message")) {
                                errorMsg = obj.get("message").asString
                            }
                        }
                    } catch (ignored: Exception) {
                    }
                    r.setValue(Resource.error(errorMsg))
                }
            }

            override fun onFailure(c: Call<ApiResponse<Void>>, t: Throwable) {
                r.value = Resource.error("Lỗi kết nối: ${t.message}")
            }
        })
        return r
    }
}
