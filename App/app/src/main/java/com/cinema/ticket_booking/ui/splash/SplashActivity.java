package com.cinema.ticket_booking.ui.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.ui.MainActivity;
import com.cinema.ticket_booking.R;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Inject
    TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the system splash screen transition
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_splash);

        // Giữ màn hình Splash trong khoảng 2 giây để người dùng đọc Slogan
        new Handler(Looper.getMainLooper()).postDelayed(this::startNextActivity, 2000);
    }

    private void startNextActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        
        // Truyền role vào bundle nếu cần (MainActivity sẽ tự check lại từ TokenManager nên ở đây chỉ cần start)
        startActivity(intent);
        finish();
        
        // Hiệu ứng chuyển động mượt mà khi vào trang chủ
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
