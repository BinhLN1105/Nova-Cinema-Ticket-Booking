package com.cinema.ticket_booking.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.databinding.FragmentVerifyOtpBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyOtpFragment : Fragment() {

    private var _binding: FragmentVerifyOtpBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AuthViewModel
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = VerifyOtpFragmentArgs.fromBundle(it).email
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyOtpBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.btnBack.setOnClickListener {
            Navigation.findNavController(view).popBackStack()
        }

        binding.etOtp.requestFocus()
        binding.etOtp.postDelayed({
            if (isAdded) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(binding.etOtp, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 200)

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length != 6) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập mã OTP 6 số")
                return@setOnClickListener
            }
            email?.let { e ->
                viewModel.verifyOtp(e, otp)
            }
        }

        binding.tvResend.setOnClickListener {
            email?.let { e ->
                viewModel.forgotPassword(e)
                SnackbarHelper.showSuccess(binding.getRoot(), "Đang gửi lại mã OTP...")
            }
        }

        viewModel.getVerifyOtpResult().observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> {
                    setLoadingState(true)
                }
                Resource.Status.SUCCESS -> {
                    setLoadingState(false)
                    if (resource.data != null) {
                        val navController = Navigation.findNavController(requireView())
                        if (navController.currentDestination?.id == R.id.verifyOtpFragment) {
                            navController.navigate(
                                VerifyOtpFragmentDirections.actionVerifyOtpToResetPassword(resource.data)
                            )
                        }
                    }
                }
                Resource.Status.ERROR -> {
                    setLoadingState(false)
                    SnackbarHelper.showError(binding.getRoot(), resource.message ?: "Xác thực mã OTP thất bại")
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnVerify.isEnabled = !isLoading
        binding.tvResend.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
