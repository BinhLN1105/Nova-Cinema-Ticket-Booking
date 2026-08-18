package com.cinema.ticket_booking.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.databinding.FragmentChangePasswordBinding
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.HashMap
import javax.inject.Inject

@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {
    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { v ->
            Navigation.findNavController(v).popBackStack()
        }

        binding.btnChangePassword.setOnClickListener { attemptChangePassword() }
    }

    private fun attemptChangePassword() {
        val current = binding.edtCurrentPassword.text.toString().trim()
        val newPass = binding.edtNewPassword.text.toString().trim()
        val confirm = binding.edtConfirmPassword.text.toString().trim()

        // Validation
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        if (current.isEmpty()) {
            binding.tilCurrentPassword.error = "Vui lòng nhập mật khẩu hiện tại"
            return
        }
        if (newPass.isEmpty()) {
            binding.tilNewPassword.error = "Vui lòng nhập mật khẩu mới"
            return
        }
        if (newPass.length < 6) {
            binding.tilNewPassword.error = "Mật khẩu mới phải có ít nhất 6 ký tự"
            return
        }
        if (newPass != confirm) {
            binding.tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            return
        }
        if (current == newPass) {
            binding.tilNewPassword.error = "Mật khẩu mới phải khác mật khẩu hiện tại"
            return
        }

        setLoading(true)

        val body = HashMap<String, String>()
        body["currentPassword"] = current
        body["newPassword"] = newPass

        apiService.changePassword(body)
            .enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(
                    call: Call<ApiResponse<Void>>,
                    response: Response<ApiResponse<Void>>
                ) {
                    if (!isAdded) return
                    setLoading(false)

                    if (response.isSuccessful && response.body() != null) {
                        SnackbarHelper.showSuccess(binding.root, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.")
                        // Delay pop to let user see the success message
                        binding.root.postDelayed({
                            if (isAdded) {
                                Navigation.findNavController(requireView()).popBackStack()
                            }
                        }, 1500)
                    } else {
                        var msg = "Đổi mật khẩu thất bại"
                        try {
                            val errorBodyStr = response.errorBody()?.string()
                            if (errorBodyStr != null) {
                                msg = errorBodyStr
                                if (msg.contains("message")) {
                                    msg = msg.substring(msg.indexOf("message") + 10)
                                    msg = msg.substring(0, msg.indexOf("\""))
                                }
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                        SnackbarHelper.showError(binding.root, msg)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                    if (!isAdded) return
                    setLoading(false)
                    SnackbarHelper.showError(binding.root, "Lỗi kết nối: " + t.message)
                }
            })
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnChangePassword.isEnabled = !loading
        binding.edtCurrentPassword.isEnabled = !loading
        binding.edtNewPassword.isEnabled = !loading
        binding.edtConfirmPassword.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
