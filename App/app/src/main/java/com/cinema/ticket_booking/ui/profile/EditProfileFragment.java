package com.cinema.ticket_booking.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.request.UpdateProfileRequest;
import com.cinema.ticket_booking.data.model.response.ApiResponse;
import com.cinema.ticket_booking.data.model.response.UserResponse;
import com.cinema.ticket_booking.databinding.FragmentEditProfileBinding;
import com.cinema.ticket_booking.network.ApiService;
import com.cinema.ticket_booking.ui.MainViewModel;
import com.cinema.ticket_booking.util.ImageUtils;
import com.cinema.ticket_booking.util.Resource;
import com.cinema.ticket_booking.util.SnackbarHelper;

import java.io.File;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class EditProfileFragment extends Fragment {
    private FragmentEditProfileBinding binding;
    private MainViewModel mainViewModel;

    @Inject
    ApiService apiService;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Load current data
        mainViewModel.getUserProfile().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.status == Resource.Status.SUCCESS && resource.data != null) {
                UserResponse user = resource.data;
                if (binding.etFullName.getText().toString().isEmpty()) {
                    binding.etFullName.setText(user.getFullName());
                }
                if (binding.etPhone.getText().toString().isEmpty()) {
                    binding.etPhone.setText(user.getPhone());
                }
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(this).load(user.getAvatarUrl()).transform(new CircleCrop()).into(binding.ivAvatar);
                }
            }
        });

        // Setup text watchers for validation
        binding.etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tlPhone.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        binding.btnSave.setOnClickListener(v -> saveProfile(view));

        // Setup image picker
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadAvatar(uri);
            }
        });

        binding.btnChangeAvatar.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });
    }

    private void uploadAvatar(Uri uri) {
        binding.progressBar.setVisibility(View.VISIBLE);
        try {
            File compressedFile = ImageUtils.compressImage(requireContext(), uri);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), compressedFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", compressedFile.getName(), requestFile);

            apiService.uploadAvatar(body).enqueue(new Callback<ApiResponse<UserResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        SnackbarHelper.showSuccess(binding.getRoot(), response.body().getMessage());
                        mainViewModel.refreshUserProfile(); // Buộc fetch lại data mới sau khi upload
                        Glide.with(EditProfileFragment.this).load(uri).transform(new CircleCrop()).into(binding.ivAvatar);
                    } else {
                        SnackbarHelper.showError(binding.getRoot(), "Cập nhật ảnh thất bại!");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarHelper.showError(binding.getRoot(), "Lỗi mạng kết nối máy chủ");
                }
            });
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            SnackbarHelper.showError(binding.getRoot(), "Lỗi xử lý ảnh: " + e.getMessage());
        }
    }

    private void saveProfile(View view) {
        String fullName = binding.etFullName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        if (fullName.isEmpty()) {
            binding.etFullName.setError("Họ tên không được để trống");
            return;
        }

        if (!phone.matches("^(03|05|07|08|09|01[2|6|8|9])+([0-9]{8})$")) {
            binding.tlPhone.setError("Số điện thoại không hợp lệ");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        UpdateProfileRequest request = new UpdateProfileRequest(fullName, phone);
        apiService.updateProfile(request).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    SnackbarHelper.showSuccess(view, "Cập nhật tài khoản thành công!");
                    mainViewModel.loadUserProfile();
                    Navigation.findNavController(view).popBackStack();
                } else {
                    SnackbarHelper.showError(view, "Cập nhật thất bại!");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);
                SnackbarHelper.showError(view, "Lỗi kết nối máy chủ");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
