package com.cinema.ticket_booking.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.splashscreen.SplashScreen;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.ActivityMainBinding;
import com.cinema.ticket_booking.util.SnackbarHelper;
import com.cinema.ticket_booking.util.ThemeManager;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.google.firebase.messaging.FirebaseMessaging;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import java.util.Set;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Inject
    TokenManager tokenManager;

    private NavController navController;

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (navController != null) navController.navigate(R.id.scannerFragment);
                } else {
                    SnackbarHelper.showError(binding.getRoot(), "Quyền máy ảnh bị từ chối. Không thể quét QR!");
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    SnackbarHelper.showInfo(binding.getRoot(), "Bạn đã từ chối nhận thông báo. Hãy bật lại trong cài đặt để nhận mã vé nhé!");
                }
            });

    // Danh sách các màn hình (Destination ID) KHÔNG hiển thị Bottom Navigation
    private static final Set<Integer> NO_BOTTOM_NAV = Set.of(
            R.id.splashFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.movieDetailFragment,
            R.id.selectShowtimeFragment,
            R.id.selectSeatFragment,
            R.id.selectComboFragment,
            R.id.confirmBookingFragment,
            R.id.paymentFragment,
            R.id.bookingDetailFragment,
            R.id.notificationFragment,
            R.id.scannerFragment,
            R.id.chatbotBottomSheet,
            R.id.walletFragment,
            R.id.voucherFragment
    );

    // Danh sách các màn hình cần ẨN nút AI Assistant (Thường là các màn quan trọng hoặc cần tập trung cao)
    private static final Set<Integer> HIDE_AI_FAB = Set.of(
            R.id.splashFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.paymentFragment,
            R.id.selectSeatFragment,
            R.id.selectComboFragment
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);   // ← phải gọi TRƯỚC setContentView để tránh flicker
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost == null) return;

        this.navController = navHost.getNavController();

        // Tự động thiết lập cấu trúc điều hướng phù hợp với Vai trò (Khách hàng / Nhân viên)
        setupNavigationByRole(navController);

        // Xử lý sự kiện khi bấm vào nút Quét mã ở giữa thanh Bottom Navigation
        binding.fabScanner.setOnClickListener(v -> {
            // Nova Optimization: Kiểm tra quyền Camera trước khi mở Scanner để tránh Crash
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Integer current = navController.getCurrentDestination() != null ? navController.getCurrentDestination().getId() : null;
                if (current == null || current != R.id.scannerFragment) {
                    navController.navigate(R.id.scannerFragment);
                }
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Xử lý sự kiện mở trợ lý ảo CineAI khi bấm vào nút Chat nổi
        binding.aiFab.setOnClickListener(v -> {
            navController.navigate(R.id.chatbotBottomSheet);
        });

        // Xử lý các liên kết sâu (Deep Links) nếu có
        handleDeepLink(getIntent(), navController);

        // Lắng nghe sự thay đổi màn hình để ẩn/hiện thanh công cụ và nút AI phù hợp
        navController.addOnDestinationChangedListener((ctrl, dest, args) -> {
            // Kiểm tra xem màn hình hiện tại có thuộc danh sách cần ẩn Bottom Nav không
            if (NO_BOTTOM_NAV.contains(dest.getId())) {
                binding.bottomAppBar.setVisibility(View.GONE);
                binding.bottomNav.setVisibility(View.GONE);
                binding.fabScanner.hide();
                // Loại bỏ padding để nội dung tràn toàn màn hình
                binding.navHostFragment.setPadding(0, 0, 0, 0);
            } else {
                binding.bottomAppBar.setVisibility(View.VISIBLE);
                binding.bottomNav.setVisibility(View.VISIBLE);
                binding.fabScanner.show();
                // Thêm padding ở dưới để nội dung không bị thanh Bottom Nav che mất
                int paddingBottom = (int) (80 * getResources().getDisplayMetrics().density);
                binding.navHostFragment.setPadding(0, 0, 0, paddingBottom);
            }

            // Kiểm tra xem màn hình hiện tại có cần ẩn nút AI Assistant không
            if (HIDE_AI_FAB.contains(dest.getId())) {
                binding.aiFab.setVisibility(View.GONE);
            } else {
                binding.aiFab.setVisibility(View.VISIBLE);
            }
        });

        // Nova Optimization: Pre-fetch user profile to avoid lag in ProfileFragment
        MainViewModel mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        if (tokenManager.isLoggedIn()) {
            mainViewModel.loadUserProfile();
            requestNotificationPermission();
        }

        // ── Nova Enterprise: Cross-Platform Notification Sync ──
        mainViewModel.getUserProfile().observe(this, resource -> {
            if (resource != null && resource.isSuccess() && resource.data != null) {
                syncFcmTopics(resource.data);
            }
        });
    }

    private void syncFcmTopics(com.cinema.ticket_booking.data.model.response.UserResponse user) {
        if (user.getAllowMarketingNotification()) {
            FirebaseMessaging.getInstance().subscribeToTopic("nova_all_users");
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("nova_all_users");
        }
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Thiết lập biểu đồ điều hướng (NavGraph) và Menu dựa trên vai trò của người dùng.
     * STAFF: Sử dụng biểu đồ nhân viên (Soát vé, Dashboard).
     * CUSTOMER/GUEST: Sử dụng biểu đồ khách hàng (Đặt vé, Khám phá).
     */
    private void setupNavigationByRole(NavController navController) {
        boolean isStaff = "STAFF".equals(tokenManager.getUserRole());
        
        if (isStaff) {
            // Nạp biểu đồ điều hướng dành riêng cho nhân viên
            navController.setGraph(R.navigation.nav_graph_staff);
            // Xóa menu cũ và nạp menu quản lý dành cho nhân viên
            binding.bottomNav.getMenu().clear();
            binding.bottomNav.inflateMenu(R.menu.staff_nav_menu);
        } else {
            // Nạp biểu đồ điều hướng dành cho khách hàng (mặc định)
            navController.setGraph(R.navigation.nav_graph_customer);
        }

        // Kết nối NavController với BottomNavigationView để tự động xử lý chuyển màn hình
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        
        // Vô hiệu hóa Item ở giữa (Dummy) để tạo khoảng trống cho nút FAB Quét mã
        if (binding.bottomNav.getMenu().size() > 2) {
            binding.bottomNav.getMenu().getItem(2).setEnabled(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            handleDeepLink(intent, navHost.getNavController());
        }
    }

    private void handleDeepLink(Intent intent, NavController navController) {
        if (intent == null) return;

        // 1. Handle URI deep links: novaticket://movies/{movieId}
        Uri uri = intent.getData();
        if (uri != null && "novaticket".equals(uri.getScheme()) && "movies".equals(uri.getHost())) {
            String movieId = uri.getLastPathSegment();
            if (movieId != null && !movieId.isEmpty()) {
                Bundle args = new Bundle();
                args.putString("movieId", movieId);
                navController.navigate(R.id.movieDetailFragment, args);
                return;
            }
        }

        // Handle URI deep links: cinema://cancel-confirm?token={token}&bookingId={id}
        if (uri != null && "cinema".equals(uri.getScheme()) && "cancel-confirm".equals(uri.getHost())) {
            String token = uri.getQueryParameter("token");
            String bookingId = uri.getQueryParameter("bookingId");
            if (token != null && bookingId != null) {
                Bundle args = new Bundle();
                args.putString("token", token);
                args.putString("bookingId", bookingId);
                navController.navigate(R.id.cancelConfirmFragment, args);
                return;
            }
        }

        // 2. Handle FCM notification taps: type + targetId from data payload
        String type = intent.getStringExtra("type");
        String targetId = intent.getStringExtra("targetId");
        if (type != null && targetId != null && !targetId.isEmpty()) {
            if ("BOOKING_REMINDER".equals(type)) {
                Bundle args = new Bundle();
                args.putString("bookingId", targetId);
                navController.navigate(R.id.bookingDetailFragment, args);
            }
            // Clear extras so it doesn't re-trigger
            intent.removeExtra("type");
            intent.removeExtra("targetId");
        }
    }
}

