package com.cinema.ticket_booking.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.cinema.ticket_booking.databinding.FragmentForgotPasswordBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.btnBack.setOnClickListener {
            Navigation.findNavController(view).popBackStack()
        }

        binding.btnSubmit.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập Email")
                return@setOnClickListener
            }
            viewModel.forgotPassword(email)
        }

        viewModel.getPasswordResult().observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> {
                    setLoadingState(true)
                }
                Resource.Status.SUCCESS -> {
                    setLoadingState(false)
                    val email = binding.etEmail.text.toString().trim()
                    val navController = Navigation.findNavController(requireView())
                    if (navController.currentDestination?.id == com.cinema.ticket_booking.R.id.forgotPasswordFragment) {
                        navController.navigate(
                            ForgotPasswordFragmentDirections.actionForgotToVerifyOtp(email)
                        )
                    }
                    SnackbarHelper.showSuccess(binding.getRoot(), "Mã OTP đã được gửi đến email của bạn")
                }
                Resource.Status.ERROR -> {
                    setLoadingState(false)
                    SnackbarHelper.showError(binding.getRoot(), resource.message ?: "Yêu cầu thất bại")
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
