package com.cinema.ticket_booking.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.FragmentLoginBinding;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel        authViewModel;

    // Google Sign-In
    private GoogleSignInClient          googleSignInClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    // Facebook Login
    private CallbackManager facebookCallbackManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Nếu đã login rồi → chuyển thẳng vào Home
        if (authViewModel.isLoggedIn()) {
            navigateToHome();
            return;
        }

        setupGoogleSignIn();
        setupFacebookLogin();
        setupObservers();
        setupClickListeners();
    }

    // ── Google Sign-In ────────────────────────────────────────────────────

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))  // Web Client ID từ Google Cloud Console
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(
                            result.getData());
                    handleGoogleSignInResult(task);
                });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken != null) {
                authViewModel.loginWithGoogle(idToken);
            } else {
                showError("Không lấy được Google token");
            }
        } catch (ApiException e) {
            showError("Google Sign-In thất bại: " + e.getStatusCode());
        }
    }

    // ── Facebook Login ────────────────────────────────────────────────────

    private void setupFacebookLogin() {
        facebookCallbackManager = CallbackManager.Factory.create();

        binding.btnFacebook.setPermissions("email", "public_profile");
        binding.btnFacebook.registerCallback(facebookCallbackManager,
                new FacebookCallback<>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        String accessToken = loginResult.getAccessToken().getToken();
                        authViewModel.loginWithFacebook(accessToken);
                    }

                    @Override
                    public void onCancel() {
                        showError("Đăng nhập Facebook bị huỷ");
                    }

                    @Override
                    public void onError(@NonNull FacebookException error) {
                        showError("Facebook Login thất bại: " + error.getMessage());
                    }
                });
    }

    // ── Click Listeners ───────────────────────────────────────────────────

    private void setupClickListeners() {
        // Đăng nhập LOCAL
        binding.btnLogin.setOnClickListener(v -> {
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                showError("Vui lòng nhập đầy đủ email và mật khẩu");
                return;
            }
            authViewModel.login(email, password);
        });

        // Google Sign-In
        binding.btnGoogle.setOnClickListener(v ->
                googleLauncher.launch(googleSignInClient.getSignInIntent()));

        // Chuyển sang màn đăng ký
        binding.tvRegister.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_login_to_register));

        // Quên mật khẩu
        binding.tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Tính năng đang phát triển", Toast.LENGTH_SHORT).show());
    }

    // ── Observers ─────────────────────────────────────────────────────────

    private void setupObservers() {
        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING -> showLoading(true);
                case SUCCESS -> {
                    showLoading(false);
                    navigateToHome();
                }
                case ERROR -> {
                    showLoading(false);
                    showError(resource.message);
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
        binding.btnGoogle.setEnabled(!show);
        binding.btnFacebook.setEnabled(!show);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void navigateToHome() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_login_to_home);
    }

    // Facebook cần override onActivityResult
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
