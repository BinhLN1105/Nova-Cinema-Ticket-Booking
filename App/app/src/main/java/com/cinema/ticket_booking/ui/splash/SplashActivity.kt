package com.cinema.ticket_booking.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.databinding.ActivitySplashBinding
import com.cinema.ticket_booking.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var binding: ActivitySplashBinding
    private var sprocketWaveAnimator: AnimatorSet? = null
    private val handler = Handler(Looper.getMainLooper())
    private var nextActivityRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Khởi tạo splash screen hệ thống với dark theme
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCinematicSplashAnimations()
    }

    private fun startCinematicSplashAnimations() {
        // ── Giai đoạn 1 (0 - 400ms): Ambient Halo Fade-in (Nền tĩnh mềm mại) ──
        binding.viewHalo.alpha = 0f
        binding.viewHalo.animate()
            .alpha(1f)
            .setDuration(400)
            .start()

        // ── Giai đoạn 2 (200 - 600ms): Logo Scale & Fade-in (Decelerate Interpolator) ──
        binding.ivLogo.alpha = 0f
        binding.ivLogo.scaleX = 0.85f
        binding.ivLogo.scaleY = 0.85f
        binding.ivLogo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .setDuration(400)
            .setStartDelay(200)
            .start()

        // ── Giai đoạn 3 (500 - 900ms): Slogan & Brand Slide-up + Fade-in ──
        binding.layoutTextGroup.alpha = 0f
        binding.layoutTextGroup.translationY = 24f
        binding.layoutTextGroup.animate()
            .translationY(0f)
            .alpha(1f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(400)
            .setStartDelay(500)
            .start()

        // ── Giai đoạn 4 (600ms+): Film Sprocket Wave Loading Indicator ──
        binding.layoutFilmSprockets.alpha = 0f
        binding.layoutFilmSprockets.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(600)
            .withEndAction {
                startFilmSprocketWave()
            }
            .start()

        // Điều hướng sang MainActivity khi hoàn tất chuẩn bị (thời lượng hiển thị 2.1s chuẩn điện ảnh)
        nextActivityRunnable = Runnable {
            startNextActivity()
        }
        handler.postDelayed(nextActivityRunnable!!, 2100)
    }

    /**
     * Hiệu ứng sóng lượn phát sáng của 5 đốm đục lỗ cuộn phim 35mm (Film Sprocket Wave Pulse).
     */
    private fun startFilmSprocketWave() {
        val sprockets = listOf(
            binding.sprocket1,
            binding.sprocket2,
            binding.sprocket3,
            binding.sprocket4,
            binding.sprocket5
        )

        val animators = ArrayList<ValueAnimator>()
        for (i in sprockets.indices) {
            val view = sprockets[i]
            val alphaAnim = ObjectAnimator.ofFloat(view, View.ALPHA, 0.25f, 1f, 0.25f).apply {
                duration = 750
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                startDelay = (i * 120).toLong()
            }
            val scaleAnim = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.35f, 1f).apply {
                duration = 750
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                startDelay = (i * 120).toLong()
            }
            animators.add(alphaAnim)
            animators.add(scaleAnim)
        }

        sprocketWaveAnimator = AnimatorSet().apply {
            playTogether(animators.toList())
            start()
        }
    }

    private fun startNextActivity() {
        if (isFinishing || isDestroyed) return
        sprocketWaveAnimator?.cancel()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        nextActivityRunnable?.let { handler.removeCallbacks(it) }
        sprocketWaveAnimator?.cancel()
        sprocketWaveAnimator = null
    }
}
