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
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Set;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    // Các destination KHÔNG hiện bottom nav
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
            R.id.chatbotFragment,
            R.id.walletFragment,
            R.id.voucherFragment
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        ThemeManager.applyTheme(this);   // ← phải gọi TRƯỚC setContentView để tránh flicker
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost == null) return;

        NavController navController = navHost.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        // Disable dummy item to prevent selection
        binding.bottomNav.getMenu().getItem(2).setEnabled(false);

        // FAB navigates to Tickets (History)
        binding.fabTickets.setOnClickListener(v -> {
            Integer current = navController.getCurrentDestination() != null ? navController.getCurrentDestination().getId() : null;
            if (current == null || current != R.id.bookingHistoryFragment) {
                navController.navigate(R.id.bookingHistoryFragment);
            }
        });

        // ── Handle Deep Links ──
        handleDeepLink(getIntent(), navController);

        navController.addOnDestinationChangedListener((ctrl, dest, args) -> {
            if (NO_BOTTOM_NAV.contains(dest.getId())) {
                binding.bottomAppBar.setVisibility(View.GONE);
                binding.bottomNav.setVisibility(View.GONE);
                binding.fabTickets.hide();
                binding.navHostFragment.setPadding(0, 0, 0, 0);
            } else {
                binding.bottomAppBar.setVisibility(View.VISIBLE);
                binding.bottomNav.setVisibility(View.VISIBLE);
                binding.fabTickets.show();
                int paddingBottom = (int) (80 * getResources().getDisplayMetrics().density);
                binding.navHostFragment.setPadding(0, 0, 0, paddingBottom);
            }
        });
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
