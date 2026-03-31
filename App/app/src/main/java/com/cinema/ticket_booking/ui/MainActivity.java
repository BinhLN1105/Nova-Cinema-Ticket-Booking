package com.cinema.ticket_booking.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.core.splashscreen.SplashScreen;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.ActivityMainBinding;
import com.cinema.ticket_booking.util.ThemeManager;
import com.cinema.ticket_booking.data.local.TokenManager;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import java.util.Set;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Inject
    TokenManager tokenManager;

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

        NavController navController = navHost.getNavController();

        // Tự động thiết lập cấu trúc điều hướng phù hợp với Vai trò (Khách hàng / Nhân viên)
        setupNavigationByRole(navController);

        // Xử lý sự kiện khi bấm vào nút Quét mã ở giữa thanh Bottom Navigation
        binding.fabScanner.setOnClickListener(v -> {
            Integer current = navController.getCurrentDestination() != null ? navController.getCurrentDestination().getId() : null;
            if (current == null || current != R.id.scannerFragment) {
                navController.navigate(R.id.scannerFragment);
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
