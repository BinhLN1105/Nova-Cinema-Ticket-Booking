package com.cinema.ticket_booking.ui.splash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.local.TokenManager;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class SplashFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded())
                return;
            int action;
            if (tokenManager.isLoggedIn() && tokenManager.isStaffOrAdmin()) {
                action = R.id.action_splash_to_scanner;
            } else {
                action = R.id.action_splash_to_home;
            }
            Navigation.findNavController(view).navigate(action);
        }, 1500);
    }
}
