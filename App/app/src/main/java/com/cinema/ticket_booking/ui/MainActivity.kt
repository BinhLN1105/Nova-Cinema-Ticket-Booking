package com.cinema.ticket_booking.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.data.model.response.UserResponse
import com.cinema.ticket_booking.databinding.ActivityMainBinding
import com.cinema.ticket_booking.util.SnackbarHelper
import com.cinema.ticket_booking.util.ThemeManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var tokenManager: TokenManager

    private var navController: NavController? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController?.navigate(R.id.scannerFragment)
        } else {
            SnackbarHelper.showError(binding.root, "Quyền máy ảnh bị từ chối. Không thể quét QR!")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            SnackbarHelper.showInfo(
                binding.root,
                "Bạn đã từ chối nhận thông báo. Hãy bật lại trong cài đặt để nhận mã vé nhé!"
            )
        }
    }

    companion object {
        // Danh sách các màn hình (Destination ID) KHÔNG hiển thị Bottom Navigation
        private val NO_BOTTOM_NAV = setOf(
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
            R.id.voucherFragment,
            R.id.promotionListFragment
        )

        // Danh sách các màn hình cần ẨN nút AI Assistant (Thường là các màn quan trọng hoặc cần tập trung cao)
        private val HIDE_AI_FAB = setOf(
            R.id.splashFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.paymentFragment,
            R.id.selectSeatFragment,
            R.id.selectComboFragment,
            R.id.confirmBookingFragment
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this) // ← phải gọi TRƯỚC setContentView để tránh flicker
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val controller = navHost.navController
        this.navController = controller

        // Tự động thiết lập cấu trúc điều hướng phù hợp với Vai trò (Khách hàng / Nhân viên)
        setupNavigationByRole(controller)

        // Xử lý sự kiện khi bấm vào nút Quét mã ở giữa thanh Bottom Navigation
        binding.fabScanner.setOnClickListener {
            // Nova Optimization: Kiểm tra quyền Camera trước khi mở Scanner để tránh Crash
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val current = controller.currentDestination?.id
                if (current == null || current != R.id.scannerFragment) {
                    controller.navigate(R.id.scannerFragment)
                }
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Xử lý sự kiện mở trợ lý ảo CineAI khi bấm vào nút Chat nổi
        binding.aiFab.setOnClickListener {
            controller.navigate(R.id.chatbotBottomSheet)
        }

        // Xử lý các liên kết sâu (Deep Links) nếu có
        handleDeepLink(intent, controller)

        // Lắng nghe sự thay đổi màn hình để ẩn/hiện thanh công cụ và nút AI phù hợp
        controller.addOnDestinationChangedListener { _, dest, _ ->
            // Kiểm tra xem màn hình hiện tại có thuộc danh sách cần ẩn Bottom Nav không
            if (NO_BOTTOM_NAV.contains(dest.id)) {
                binding.bottomAppBar.visibility = View.GONE
                binding.bottomNav.visibility = View.GONE
                binding.fabScanner.hide()
                // Loại bỏ padding để nội dung tràn toàn màn hình
                binding.navHostFragment.setPadding(0, 0, 0, 0)
            } else {
                binding.bottomAppBar.visibility = View.VISIBLE
                binding.bottomNav.visibility = View.VISIBLE
                binding.fabScanner.show()
                // Thêm padding ở dưới để nội dung không bị thanh Bottom Nav che mất
                val paddingBottom = (80 * resources.displayMetrics.density).toInt()
                binding.navHostFragment.setPadding(0, 0, 0, paddingBottom)
            }

            // Kiểm tra xem màn hình hiện tại có cần ẩn nút AI Assistant không
            if (HIDE_AI_FAB.contains(dest.id) || "STAFF" == tokenManager.userRole) {
                binding.aiFab.visibility = View.GONE
            } else {
                binding.aiFab.visibility = View.VISIBLE
            }
        }

        // Tải hồ sơ người dùng
        val mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        if (tokenManager.isLoggedIn) {
            mainViewModel.loadUserProfile()
            requestNotificationPermission()
        }

        // đồng bộ chủ đề FCM đa nền tảng
        mainViewModel.getUserProfile().observe(this) { resource ->
            if (resource != null && resource.isSuccess && resource.data != null) {
                syncFcmTopics(resource.data)
            }
        }
    }

    private fun syncFcmTopics(user: UserResponse) {
        if (user.allowMarketingNotification) {
            FirebaseMessaging.getInstance().subscribeToTopic("nova_all_users")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("nova_all_users")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Thiết lập biểu đồ điều hướng (NavGraph) và Menu dựa trên vai trò của người dùng.
     * STAFF: Sử dụng biểu đồ nhân viên (Soát vé, Dashboard).
     * CUSTOMER/GUEST: Sử dụng biểu đồ khách hàng (Đặt vé, Khám phá).
     */
    private fun setupNavigationByRole(navController: NavController) {
        val isStaff = "STAFF" == tokenManager.userRole

        if (isStaff) {
            // Nạp biểu đồ điều hướng dành riêng cho nhân viên
            navController.setGraph(R.navigation.nav_graph_staff)
            // Xóa menu cũ và nạp menu quản lý dành cho nhân viên
            binding.bottomNav.menu.clear()
            binding.bottomNav.inflateMenu(R.menu.staff_nav_menu)
        } else {
            // Nạp biểu đồ điều hướng dành cho khách hàng (mặc định)
            navController.setGraph(R.navigation.nav_graph_customer)
        }

        // Kết nối NavController với BottomNavigationView để tự động xử lý chuyển màn hình
        NavigationUI.setupWithNavController(binding.bottomNav, navController)

        // Vô hiệu hóa Item ở giữa (Dummy) để tạo khoảng trống cho nút FAB Quét mã
        if (binding.bottomNav.menu.size() > 2) {
            binding.bottomNav.menu.getItem(2).isEnabled = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navHost?.let {
            handleDeepLink(intent, it.navController)
        }
    }

    private fun handleDeepLink(intent: Intent?, navController: NavController) {
        if (intent == null) return

        // Xử lý liên kết sâu URI: novaticket://movies/{movieId}
        val uri = intent.data
        if (uri != null && "novaticket" == uri.scheme && "movies" == uri.host) {
            val movieId = uri.lastPathSegment
            if (!movieId.isNullOrEmpty()) {
                val args = Bundle().apply {
                    putString("movieId", movieId)
                }
                navController.navigate(R.id.movieDetailFragment, args)
                return
            }
        }

        // Xử lý liên kết sâu URI: cinema://cancel-confirm?token={token}&bookingId={id}
        if (uri != null && "cinema" == uri.scheme && "cancel-confirm" == uri.host) {
            val token = uri.getQueryParameter("token")
            val bookingId = uri.getQueryParameter("bookingId")
            if (token != null && bookingId != null) {
                val args = Bundle().apply {
                    putString("token", token)
                    putString("bookingId", bookingId)
                }
                navController.navigate(R.id.cancelConfirmFragment, args)
                return
            }
        }

        // Xử lý FCM notification taps: type + targetId from data payload
        val type = intent.getStringExtra("type")
        val targetId = intent.getStringExtra("targetId")
        if (type != null && !targetId.isNullOrEmpty()) {
            if ("BOOKING_REMINDER" == type) {
                val args = Bundle().apply {
                    putString("bookingId", targetId)
                }
                navController.navigate(R.id.bookingDetailFragment, args)
            }
            // Xóa Extras để không kích hoạt lại
            intent.removeExtra("type")
            intent.removeExtra("targetId")
        }
    }
}
