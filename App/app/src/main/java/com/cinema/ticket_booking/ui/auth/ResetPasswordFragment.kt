package com.cinema.ticket_booking.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.databinding.FragmentResetPasswordBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AuthViewModel
    private var resetToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            resetToken = ResetPasswordFragmentArgs.fromBundle(it).resetToken
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.btnBack.setOnClickListener {
            Navigation.findNavController(view).popBackStack()
        }

        binding.btnReset.setOnClickListener {
            val newPass = binding.etNewPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            if (newPass.length < 6) {
                SnackbarHelper.showError(binding.getRoot(), "Mật khẩu phải có ít nhất 6 ký tự")
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                SnackbarHelper.showError(binding.getRoot(), "Mật khẩu xác nhận không khớp")
                return@setOnClickListener
            }

            resetToken?.let { token ->
                viewModel.resetPassword(token, newPass)
            }
        }

        viewModel.getPasswordResult().observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> {
                    setLoadingState(true)
                }
                Resource.Status.SUCCESS -> {
                    setLoadingState(false)
                    SnackbarHelper.showSuccess(binding.getRoot(), "Đổi mật khẩu thành công! Vui lòng đăng nhập.")

                    // Use SafeArgs and clear backstack
                    Navigation.findNavController(requireView()).navigate(
                        ResetPasswordFragmentDirections.actionResetPasswordToLogin()
                    )
                }
                Resource.Status.ERROR -> {
                    setLoadingState(false)
                    SnackbarHelper.showError(binding.getRoot(), resource.message ?: "Thay đổi mật khẩu thất bại")
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnReset.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
