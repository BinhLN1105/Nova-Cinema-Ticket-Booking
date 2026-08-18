package com.cinema.ticket_booking.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.VoucherSummary
import com.cinema.ticket_booking.databinding.FragmentVoucherBinding
import com.cinema.ticket_booking.util.SnackbarHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VoucherFragment : Fragment() {

    private var _binding: FragmentVoucherBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: VoucherViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVoucherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[VoucherViewModel::class.java]

        binding.btnBack.setOnClickListener { v -> Navigation.findNavController(v).navigateUp() }
        binding.rvVouchers.layoutManager = LinearLayoutManager(requireContext())

        binding.btnApplyVoucher.setOnClickListener {
            val code = if (binding.etVoucherCode.text != null) binding.etVoucherCode.text.toString().trim() else ""
            if (code.isEmpty()) {
                SnackbarHelper.showError(binding.root, "Vui lòng nhập mã voucher")
            } else {
                viewModel.claimVoucher(code)
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadVouchers()

        viewModel.vouchers.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.visibility = View.GONE
            if (resource.isSuccess && resource.data != null) {
                val vouchers = resource.data
                if (vouchers.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvVouchers.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvVouchers.visibility = View.VISIBLE
                    binding.rvVouchers.adapter = VoucherAdapter(vouchers)
                }
            } else if (resource.isError) {
                SnackbarHelper.showError(binding.root, resource.message ?: "Không thể tải danh sách voucher")
            }
        }

        viewModel.claimResult.observe(viewLifecycleOwner) { resource ->
            if (resource == null) return@observe
            if (resource.isLoading) {
                binding.btnApplyVoucher.isEnabled = false
                binding.btnApplyVoucher.text = "Đang xử lý..."
            } else {
                binding.btnApplyVoucher.isEnabled = true
                binding.btnApplyVoucher.text = "Lưu mã"
                if (resource.isSuccess) {
                    SnackbarHelper.showSuccess(binding.root, "Lưu mã ưu đãi thành công!")
                    binding.etVoucherCode.setText("")
                    viewModel.clearClaimResult()
                } else {
                    SnackbarHelper.showError(binding.root, resource.message ?: "Đổi mã thất bại")
                    viewModel.clearClaimResult()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Inner Adapter ────────────────────────────────────────────────────────
    class VoucherAdapter(private val items: List<VoucherSummary>) : RecyclerView.Adapter<VoucherAdapter.VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_voucher, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val v = items[position]
            holder.tvCode.text = v.code
            holder.tvDescription.text = v.description
            holder.tvValidTo.text = "HSD: " + (if (v.endDate != null) v.endDate.substring(0, 10) else "N/A")

            val discount = if ("PERCENTAGE".equals(v.discountType, ignoreCase = true)) {
                "-" + v.discountValue.toInt() + "%"
            } else {
                String.format("-%,.0f₫", v.discountValue)
            }
            holder.tvDiscount.text = discount

            // ── Status Visualization ─────────────────────────────────────────
            val status = v.status ?: "AVAILABLE"
            when (status) {
                "USED" -> {
                    holder.tvStatus.text = "Đã sử dụng"
                    holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(0x33888888.toInt())
                    holder.tvStatus.setTextColor(0xFF888888.toInt())
                    holder.itemView.alpha = 0.5f
                }
                "PENDING" -> {
                    holder.tvStatus.text = "Đang xử lý"
                    holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(0x33FF9800.toInt())
                    holder.tvStatus.setTextColor(0xFFFF9800.toInt())
                    holder.itemView.alpha = 1.0f
                }
                "AVAILABLE" -> {
                    holder.tvStatus.text = "Sẵn sàng"
                    holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(0x33669dff.toInt())
                    holder.tvStatus.setTextColor(0xFF669DFF.toInt())
                    holder.itemView.alpha = 1.0f
                }
                else -> {
                    holder.tvStatus.text = "Sẵn sàng"
                    holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(0x33669dff.toInt())
                    holder.tvStatus.setTextColor(0xFF669DFF.toInt())
                    holder.itemView.alpha = 1.0f
                }
            }
        }

        override fun getItemCount(): Int = items.size

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvCode: TextView = itemView.findViewById(R.id.tvCode)
            val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
            val tvValidTo: TextView = itemView.findViewById(R.id.tvValidTo)
            val tvDiscount: TextView = itemView.findViewById(R.id.tvDiscount)
            val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        }
    }
}
