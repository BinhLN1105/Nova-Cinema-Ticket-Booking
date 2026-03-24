package com.cinema.ticket_booking.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
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

        navController.addOnDestinationChangedListener((ctrl, dest, args) -> {
            if (NO_BOTTOM_NAV.contains(dest.getId())) {
                binding.bottomAppBar.setVisibility(View.GONE);
                binding.bottomNav.setVisibility(View.GONE);
                binding.fabTickets.hide();
            } else {
                binding.bottomAppBar.setVisibility(View.VISIBLE);
                binding.bottomNav.setVisibility(View.VISIBLE);
                binding.fabTickets.show();
            }
        });
    }
}
