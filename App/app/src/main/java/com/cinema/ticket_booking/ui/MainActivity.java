package com.cinema.ticket_booking.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.databinding.ActivityMainBinding;
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
            R.id.notificationFragment
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost == null) return;

        NavController navController = navHost.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        navController.addOnDestinationChangedListener((ctrl, dest, args) -> {
            if (NO_BOTTOM_NAV.contains(dest.getId())) {
                binding.bottomNav.setVisibility(View.GONE);
            } else {
                binding.bottomNav.setVisibility(View.VISIBLE);
            }
        });
    }
}
