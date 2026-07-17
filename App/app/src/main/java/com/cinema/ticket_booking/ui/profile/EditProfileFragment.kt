package com.cinema.ticket_booking.ui.profile

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.cinema.ticket_booking.data.model.request.UpdateProfileRequest
import com.cinema.ticket_booking.data.model.response.ApiResponse
import com.cinema.ticket_booking.data.model.response.UserResponse
import com.cinema.ticket_booking.databinding.FragmentEditProfileBinding
import com.cinema.ticket_booking.network.ApiService
import com.cinema.ticket_booking.ui.MainViewModel
import com.cinema.ticket_booking.util.ImageUtils
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainViewModel: MainViewModel

    @Inject
    lateinit var apiService: ApiService

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // Load current data
        mainViewModel.getUserProfile().observe(viewLifecycleOwner) { resource ->
            if (resource != null && resource.status == Resource.Status.SUCCESS && resource.data != null) {
                val user = resource.data
                if (binding.etFullName.text.toString().isEmpty()) {
                    binding.etFullName.setText(user.fullName)
                }
                if (binding.etPhone.text.toString().isEmpty()) {
                    binding.etPhone.setText(user.phone)
                }
                val avatarUrl = user.avatarUrl
                if (!avatarUrl.isNullOrEmpty()) {
                    Glide.with(this).load(avatarUrl).transform(CircleCrop()).into(binding.ivAvatar)
                }
            }
        }

        // Setup text watchers for validation
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tlPhone.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnBack.setOnClickListener { v -> Navigation.findNavController(view).popBackStack() }

        binding.btnSave.setOnClickListener { saveProfile(view) }

        // Setup image picker
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                uploadAvatar(uri)
            }
        }

        binding.btnChangeAvatar.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun uploadAvatar(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        try {
            val compressedFile = ImageUtils.compressImage(requireContext(), uri)
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val requestFile = compressedFile.asRequestBody(mediaType)
            val body = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)

            apiService.uploadAvatar(body).enqueue(object : Callback<ApiResponse<UserResponse>> {
                override fun onResponse(call: Call<ApiResponse<UserResponse>>, response: Response<ApiResponse<UserResponse>>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        SnackbarHelper.showSuccess(binding.root, response.body()!!.message ?: "Cập nhật ảnh thành công!")
                        mainViewModel.refreshUserProfile() // Buộc fetch lại data mới sau khi upload
                        Glide.with(this@EditProfileFragment).load(uri).transform(CircleCrop()).into(binding.ivAvatar)
                    } else {
                        SnackbarHelper.showError(binding.root, "Cập nhật ảnh thất bại!")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<UserResponse>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(binding.root, "Lỗi mạng kết nối máy chủ")
                }
            })
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            SnackbarHelper.showError(binding.root, "Lỗi xử lý ảnh: " + e.message)
        }
    }

    private fun saveProfile(view: View) {
        val fullName = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Họ tên không được để trống"
            return
        }

        if (!phone.matches(Regex("^(03|05|07|08|09|01[2|6|8|9])+([0-9]{8})$"))) {
            binding.tlPhone.error = "Số điện thoại không hợp lệ"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        val request = UpdateProfileRequest(fullName, phone)
        apiService.updateProfile(request).enqueue(object : Callback<ApiResponse<UserResponse>> {
            override fun onResponse(call: Call<ApiResponse<UserResponse>>, response: Response<ApiResponse<UserResponse>>) {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                if (response.isSuccessful && response.body() != null) {
                    SnackbarHelper.showSuccess(view, "Cập nhật tài khoản thành công!")
                    mainViewModel.loadUserProfile()
                    Navigation.findNavController(view).popBackStack()
                } else {
                    SnackbarHelper.showError(view, "Cập nhật thất bại!")
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserResponse>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                SnackbarHelper.showError(view, "Lỗi kết nối máy chủ")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
