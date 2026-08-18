package com.cinema.ticket_booking.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.data.model.response.UserResponse
import com.cinema.ticket_booking.databinding.FragmentProfileBinding
import com.cinema.ticket_booking.ui.MainActivity
import com.cinema.ticket_booking.ui.MainViewModel
import com.cinema.ticket_booking.util.SnackbarHelper
import com.cinema.ticket_booking.util.ThemeManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel // Nova: Use activity-shared ViewModel

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nova: Connect to the Activity-level ViewModel to share pre-fetched user data
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        updateUI(tokenManager.isLoggedIn)

        // Nova Optimization: Instant UI update if data is already in RAM
        viewModel.getUserProfile().observe(viewLifecycleOwner) { resource ->
            if (resource == null) return@observe

            val isLoggedIn = tokenManager.isLoggedIn
            if (resource.isSuccess && resource.data != null && isLoggedIn) {
                val user = resource.data
                binding.tvName.text = user.fullName
                binding.tvEmail.text = user.email
                // Professional Membership Logic (Nova Algorithm - Refined for EXP/CP)
                val exp = user.availableExp
                val cp = user.cinePoints

                val currentMin = user.currentTierMinPoints ?: 0
                val nextMin = user.nextTierMinPoints ?: 500

                val tier = user.rank ?: "BRONZE"
                var nextTier = "SILVER"
                if (tier.equals("SILVER", ignoreCase = true)) {
                    nextTier = "GOLD"
                } else if (tier.equals("GOLD", ignoreCase = true)) {
                    nextTier = "DIAMOND"
                } else if (tier.equals("DIAMOND", ignoreCase = true)) {
                    nextTier = "MAX"
                }

                var nextSub = nextMin - exp
                if (nextSub < 0 || tier.equals("DIAMOND", ignoreCase = true)) {
                    nextSub = 0
                }

                var progress = 100
                val totalRange = nextMin - currentMin
                if (totalRange > 0 && !tier.equals("DIAMOND", ignoreCase = true)) {
                    progress = (((exp - currentMin) * 100) / totalRange).toInt()
                    if (progress < 0) progress = 0
                    if (progress > 100) progress = 100
                }

                binding.tvTierBadge.text = "⭐ ${tier.uppercase()}"
                binding.tvCurrentTier.text = "CẤP ĐỘ HIỆN TẠI: ${tier.uppercase()}"
                binding.tvCinePoints.text = cp.toString()
                binding.tvPointsToNext.text = if (nextSub > 0) "$nextSub EXP ĐỂ LÊN $nextTier" else "BẠN ĐÃ ĐẠT CẤP TỐI ĐA"
                binding.progressBarRank.progress = progress

                // Hiển thị ảnh đại diện (avatar)
                val avatarUrl = user.avatarUrl
                if (!avatarUrl.isNullOrEmpty()) {
                    binding.ivAvatar.clearColorFilter() // Xóa tint để hiện đúng màu ảnh thật
                    Glide.with(this@ProfileFragment)
                        .load(avatarUrl)
                        .transform(com.bumptech.glide.load.resource.bitmap.CircleCrop())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(binding.ivAvatar)
                } else {
                    Glide.with(this@ProfileFragment).clear(binding.ivAvatar)
                    binding.ivAvatar.setImageResource(R.drawable.ic_profile)
                }

                // ── Granular Notification Settings (Nova Enterprise Logic) ──
                setupNotificationSwitches(user)

            } else if (resource.isError && isLoggedIn) {
                val message = resource.message
                if (message != null && message.contains("401")) {
                    tokenManager.clearAll()
                    updateUI(false)
                } else {
                    // Mạng lỗi (Offline) -> Lấy dữ liệu đệm từ TokenManager
                    binding.tvName.text = tokenManager.userName ?: "Không rõ"
                    binding.tvEmail.text = tokenManager.userEmail ?: "Đang offline"

                    binding.tvTierBadge.text = "⭐ TIER"
                    binding.tvCurrentTier.text = "CẤP ĐỘ: OFFLINE"
                    binding.tvCinePoints.text = "-"
                    binding.tvPointsToNext.text = "KHÔNG THỂ TẢI EXP"
                    binding.progressBarRank.progress = 0

                    val avatarUrl = tokenManager.avatarUrl
                    if (!avatarUrl.isNullOrEmpty()) {
                        binding.ivAvatar.clearColorFilter()
                        Glide.with(this@ProfileFragment)
                            .load(avatarUrl)
                            .transform(com.bumptech.glide.load.resource.bitmap.CircleCrop())
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(binding.ivAvatar)
                    } else {
                        Glide.with(this@ProfileFragment).clear(binding.ivAvatar)
                        binding.ivAvatar.setImageResource(R.drawable.ic_profile)
                    }

                    if (!message.isNullOrEmpty()) {
                        SnackbarHelper.showError(binding.root, "Đang xem ở chế độ Offline")
                    }
                }
            }
        }

        // ── Dark/light mode toggle ──
        val isDark = ThemeManager.isDarkMode(requireContext())
        binding.switchDarkMode.setOnCheckedChangeListener(null)
        binding.switchDarkMode.isChecked = isDark
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkMode(requireContext(), isChecked)
        }
    }

    private fun updateUI(loggedIn: Boolean) {
        if (loggedIn) {
            viewModel.loadUserProfile()
            binding.btnLogout.text = "ĐĂNG XUẤT"
            binding.btnLogout.setTextColor(resources.getColor(R.color.error, null))
            binding.btnLogout.setStrokeColorResource(R.color.error)
            binding.btnLogout.setOnClickListener {
                viewModel.logout()
                // Khởi động lại ứng dụng để đưa người dùng về biểu đồ điều hướng (NavGraph) mặc định (Dành cho khách)
                requireActivity().finish()
                startActivity(Intent(requireActivity(), MainActivity::class.java))
            }

            binding.btnNavReviews.setOnClickListener {
                if (tokenManager.isStaffOrAdmin) {
                    SnackbarHelper.showInfo(binding.root, "Staff không hỗ trợ chức năng này")
                    return@setOnClickListener
                }
                // Nova: Set pending tab in ViewModel first
                viewModel.requestBookingTab("HISTORY")

                // Then trigger BottomNav selection (this performs the tab switch cleanly)
                val nav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
                if (nav != null) {
                    nav.selectedItemId = R.id.bookingHistoryFragment
                } else {
                    // Fallback to manual navigation if nav is missing
                    Navigation.findNavController(requireView()).navigate(R.id.bookingHistoryFragment)
                }
            }
            binding.btnNavVouchers.setOnClickListener {
                if (tokenManager.isStaffOrAdmin) {
                    SnackbarHelper.showInfo(binding.root, "Staff không hỗ trợ chức năng này")
                    return@setOnClickListener
                }
                Navigation.findNavController(requireView()).navigate(R.id.voucherFragment)
            }
            binding.btnNavGiftCards.setOnClickListener {
                if (tokenManager.isStaffOrAdmin) {
                    SnackbarHelper.showInfo(binding.root, "Staff không hỗ trợ chức năng này")
                    return@setOnClickListener
                }
                Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_wallet)
            }
            binding.btnNavPromotions.setOnClickListener {
                Navigation.findNavController(requireView()).navigate(R.id.action_global_promotionList)
            }

            binding.btnRedeem.setOnClickListener {
                if (tokenManager.isStaffOrAdmin) {
                    SnackbarHelper.showInfo(binding.root, "Staff không hỗ trợ chức năng này")
                } else {
                    Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_wallet)
                }
            }
            binding.btnEditProfile.setOnClickListener {
                if (tokenManager.isStaffOrAdmin) {
                    SnackbarHelper.showInfo(binding.root, "Hãy liên hệ Admin / Cinema Manager để cập nhật thông tin")
                } else {
                    Navigation.findNavController(requireView()).navigate(R.id.editProfileFragment)
                }
            }

        } else {
            binding.tvName.text = "Chưa đăng nhập"
            binding.tvEmail.text = "Vui lòng đăng nhập để sử dụng tính năng"
            binding.tvCinePoints.text = "-"
            binding.tvPointsToNext.text = "0 PTS TO NEXT TIER"
            binding.progressBarRank.progress = 0

            binding.btnLogout.text = "ĐĂNG NHẬP"
            binding.btnLogout.setTextColor(resources.getColor(R.color.primary, null))
            binding.btnLogout.setStrokeColorResource(R.color.primary)
            binding.btnLogout.setOnClickListener {
                Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_login)
            }

            val loginListener = View.OnClickListener {
                Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_login)
            }
            binding.btnNavGiftCards.setOnClickListener(loginListener)
            binding.btnRedeem.setOnClickListener(loginListener)
            binding.btnNavVouchers.setOnClickListener(loginListener)
            binding.btnNavReviews.setOnClickListener(loginListener)
            binding.btnNavPromotions.setOnClickListener(loginListener)
            binding.rowChangePassword.setOnClickListener(loginListener)
            binding.btnEditProfile.setOnClickListener(loginListener)

            // Disable switches if not logged in
            binding.switchTransactionNotify.isEnabled = false
            binding.switchMarketingNotify.setEnabled(false)
        }
    }

    private fun setupNotificationSwitches(user: UserResponse) {
        // Initial State
        binding.switchTransactionNotify.setOnCheckedChangeListener(null)
        binding.switchMarketingNotify.setOnCheckedChangeListener(null)

        binding.switchTransactionNotify.isChecked = user.allowTransactionNotification ?: false
        binding.switchMarketingNotify.isChecked = user.allowMarketingNotification ?: false

        // Marketing Logic (Simple Topic Sync)
        binding.switchMarketingNotify.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("nova_all_users")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("nova_all_users")
            }
            // Sync to Backend
            viewModel.updateNotificationSettings(isChecked, binding.switchTransactionNotify.isChecked)
                .observe(viewLifecycleOwner) { res ->
                    if (res.isSuccess) {
                        SnackbarHelper.showRaw(binding.root, "Đã cập nhật tùy chọn khuyến mãi")
                    }
                }
        }

        // Transactional Logic (With Warning Dialog)
        binding.switchTransactionNotify.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                // Show Warning Dialog before allowing to turn OFF
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("⚠️ Tắt thông báo vé?")
                    .setMessage("Nếu tắt tính năng này, bạn sẽ không nhận được mã QR soát vé tự động và các thông báo thay đổi lịch chiếu khẩn cấp. Bạn có chắc chắn muốn tắt không?")
                    .setPositiveButton("VẪN TẮT") { _, _ ->
                        syncTransactionSetting(false)
                    }
                    .setNegativeButton("GIỮ BẬT") { _, _ ->
                        binding.switchTransactionNotify.setOnCheckedChangeListener(null)
                        binding.switchTransactionNotify.isChecked = true
                        setupNotificationSwitches(user) // Re-attach listener
                    }
                    .setCancelable(false)
                    .show()
            } else {
                syncTransactionSetting(true)
            }
        }
    }

    private fun syncTransactionSetting(isEnabled: Boolean) {
        viewModel.updateNotificationSettings(binding.switchMarketingNotify.isChecked, isEnabled)
            .observe(viewLifecycleOwner) { res ->
                if (res.isSuccess) {
                    SnackbarHelper.showRaw(binding.root, "Đã cập nhật tùy chọn giao dịch")
                }
            }
    }

    override fun onResume() {
        super.onResume();
        // Mỗi lần quay lại Profile, reload dữ liệu mới nhất (CP, EXP, tier...)
        // Dùng refreshUserProfile() thay vì loadUserProfile() vì load sẽ skip nếu đã có data
        if (tokenManager.isLoggedIn) {
            viewModel.refreshUserProfile()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
