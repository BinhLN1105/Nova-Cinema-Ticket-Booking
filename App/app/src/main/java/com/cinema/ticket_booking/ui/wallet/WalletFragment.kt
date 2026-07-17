package com.cinema.ticket_booking.ui.wallet

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.databinding.FragmentWalletBinding
import com.cinema.ticket_booking.util.Resource
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: WalletViewModel

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[WalletViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener { v ->
            Navigation.findNavController(v).navigateUp()
        }

        // Show current balance from profile already in TokenManager (or load profile)
        viewModel.loadProfile()

        binding.btnRedeem.setOnClickListener {
            val code = binding.etGiftCode.text.toString().trim()
            if (TextUtils.isEmpty(code)) {
                SnackbarHelper.showError(binding.root, "Vui lòng nhập mã thẻ quà tặng")
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            binding.tvRedeemMessage.visibility = View.GONE
            viewModel.redeemGiftCard(code)
        }

        viewModel.profile.observe(viewLifecycleOwner) { resource ->
            if (resource.isSuccess && resource.data != null) {
                binding.tvBalance.text = resource.data.cinePoints.toString()
            }
        }

        viewModel.redeemResult.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.visibility = View.GONE
            binding.tvRedeemMessage.visibility = View.VISIBLE
            if (resource.status == Resource.Status.SUCCESS) {
                binding.etGiftCode.setText("")
                binding.tvRedeemMessage.text = "✅ Đổi thẻ thành công! Điểm đã được cộng vào tài khoản."
                binding.tvRedeemMessage.setTextColor(resources.getColor(R.color.success, null))
                viewModel.loadProfile() // refresh balance
            } else if (resource.status == Resource.Status.ERROR) {
                binding.tvRedeemMessage.text = "❌ " + resource.message
                binding.tvRedeemMessage.setTextColor(resources.getColor(R.color.error, null))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
