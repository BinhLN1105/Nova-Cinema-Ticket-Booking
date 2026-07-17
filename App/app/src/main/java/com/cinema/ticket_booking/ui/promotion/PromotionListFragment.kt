package com.cinema.ticket_booking.ui.promotion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.data.model.response.PromotionResponse
import com.cinema.ticket_booking.databinding.FragmentPromotionListBinding
import com.cinema.ticket_booking.ui.home.HomeViewModel
import com.cinema.ticket_booking.util.SnackbarHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PromotionListFragment : Fragment() {

    private var _binding: FragmentPromotionListBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel: HomeViewModel

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPromotionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Share the HomeViewModel from the Activity to avoid redundant API calls
        sharedViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener { Navigation.findNavController(it).navigateUp() }

        binding.rvPromotions.layoutManager = LinearLayoutManager(requireContext())

        // Observe the active promotions already fetched by HomeFragment
        sharedViewModel.getActivePromotions().observe(viewLifecycleOwner) { resource ->
            if (resource == null) return@observe

            if (resource.isSuccess && resource.data != null) {
                if (resource.data.isEmpty()) {
                    binding.rvPromotions.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvPromotions.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE

                    val adapter = PromotionListAdapter(
                        resource.data,
                        object : PromotionListAdapter.OnPromotionClickListener {
                            override fun onPromotionClick(promotion: PromotionResponse) {
                                val url = promotion.targetUrl
                                if (tokenManager.isStaffOrAdmin) {
                                    SnackbarHelper.showInfo(binding.root, "Staff không hỗ trợ chức năng này")
                                    return
                                }
                                if (!url.isNullOrBlank()) {
                                    try {
                                        if (url.startsWith("/")) {
                                            if (url.startsWith("/movie") || url.startsWith("/movies")) {
                                                Navigation.findNavController(binding.root).navigate(R.id.homeFragment)
                                            } else if (url.startsWith("/cinema")) {
                                                val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
                                                bottomNav?.selectedItemId = R.id.searchFragment
                                            } else {
                                                SnackbarHelper.showInfo(
                                                    binding.root,
                                                    "Khuyến mãi được tự động áp dụng khi thanh toán!"
                                                )
                                            }
                                        } else {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            startActivity(intent)
                                        }
                                    } catch (e: Exception) {
                                        SnackbarHelper.showError(binding.root, "Không thể mở liên kết!")
                                    }
                                }
                            }
                        }
                    )
                    binding.rvPromotions.adapter = adapter
                }
            } else if (resource.isError) {
                SnackbarHelper.showError(binding.root, resource.message ?: "Lỗi tải khuyến mãi")
                binding.rvPromotions.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
