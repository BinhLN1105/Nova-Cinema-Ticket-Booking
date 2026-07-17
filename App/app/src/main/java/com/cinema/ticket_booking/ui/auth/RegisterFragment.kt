package com.cinema.ticket_booking.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.databinding.FragmentRegisterBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val name = binding.etFullName.text.toString().trim()
            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập đầy đủ thông tin")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                SnackbarHelper.showError(binding.getRoot(), "Mật khẩu không khớp")
                return@setOnClickListener
            }
            authViewModel.register(email, password, name)
        }

        binding.tvLogin.setOnClickListener {
            Navigation.findNavController(view).popBackStack()
        }

        authViewModel.getAuthResult().observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> binding.progressBar.visibility = View.VISIBLE
                Resource.Status.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    val navController = Navigation.findNavController(requireView())
                    if (navController.currentDestination?.id == R.id.registerFragment) {
                        navController.navigate(R.id.action_register_to_home)
                    }
                }
                Resource.Status.ERROR -> {
                    binding.progressBar.visibility = View.GONE
                    SnackbarHelper.showError(binding.getRoot(), resource.message ?: "Đăng ký thất bại")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
