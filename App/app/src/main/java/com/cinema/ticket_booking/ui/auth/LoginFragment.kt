package com.cinema.ticket_booking.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.databinding.FragmentLoginBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var authViewModel: AuthViewModel

    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleLauncher: ActivityResultLauncher<Intent>

    // Facebook Login
    private lateinit var facebookCallbackManager: CallbackManager

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Nếu đã login rồi → chuyển thẳng vào Home
        if (authViewModel.isLoggedIn()) {
            navigateToHome()
            return
        }

        setupGoogleSignIn()
        setupFacebookLogin()
        setupObservers()
        setupClickListeners()
    }

    // ── Google Sign-In ────────────────────────────────────────────────────

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id)) // Web Client ID từ Google Cloud Console
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        googleLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(task: com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                authViewModel.loginWithGoogle(idToken)
            } else {
                SnackbarHelper.showError(binding.getRoot(), "Không lấy được Google token")
            }
        } catch (e: ApiException) {
            SnackbarHelper.showError(binding.getRoot(), "Google Sign-In thất bại: ${e.statusCode}")
        }
    }

    // ── Facebook Login ────────────────────────────────────────────────────

    private fun setupFacebookLogin() {
        facebookCallbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(
            facebookCallbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val accessToken = result.accessToken.token
                    authViewModel.loginWithFacebook(accessToken)
                }

                override fun onCancel() {
                    SnackbarHelper.showError(binding.getRoot(), "Đăng nhập Facebook bị huỷ")
                }

                override fun onError(error: FacebookException) {
                    SnackbarHelper.showError(binding.getRoot(), "Facebook Login thất bại: ${error.message}")
                }
            }
        )
    }

    // ── Click Listeners ───────────────────────────────────────────────────

    private fun setupClickListeners() {
        // Đăng nhập LOCAL
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập đầy đủ email và mật khẩu")
                return@setOnClickListener
            }
            authViewModel.login(email, password)
        }

        // Google Sign-In
        binding.btnGoogle.setOnClickListener {
            googleLauncher.launch(googleSignInClient.signInIntent)
        }

        // Facebook Login
        binding.btnFacebook.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                listOf("email", "public_profile")
            )
        }

        // Chuyển sang màn đăng ký
        binding.tvRegister.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_login_to_register)
        }

        // Quên mật khẩu
        binding.tvForgotPassword.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_login_to_forgotPassword)
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────

    private fun setupObservers() {
        authViewModel.getAuthResult().observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.LOADING -> showLoading(true)
                Resource.Status.SUCCESS -> {
                    showLoading(false)
                    navigateToHome()
                }
                Resource.Status.ERROR -> {
                    showLoading(false)
                    showError(resource.message ?: "Đăng nhập thất bại")
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnGoogle.isEnabled = !show
        binding.btnFacebook.isEnabled = !show
    }

    private fun showError(message: String) {
        SnackbarHelper.showError(binding.getRoot(), message)
    }

    private fun navigateToHome() {
        // Làm mới (Restart) lại Activity để MainActivity thiết lập lại NavGraph dựa trên Vai trò mới.
        // Điều này đảm bảo dọn sạch các State cũ và tải đúng Menu/Giao diện cho Nhân viên hoặc Khách hàng.
        requireActivity().finish()
        startActivity(Intent(requireContext(), com.cinema.ticket_booking.ui.MainActivity::class.java))
    }

    // Facebook cần override onActivityResult (Deprecated, but needed since API relies on callbackManager)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
