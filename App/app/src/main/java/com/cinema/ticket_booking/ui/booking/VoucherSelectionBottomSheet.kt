package com.cinema.ticket_booking.ui.booking

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.VoucherSummary
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.ArrayList
import java.util.Collections
import java.util.Locale

class VoucherSelectionBottomSheet : BottomSheetDialogFragment() {

    interface OnVoucherSelectedListener {
        fun onVoucherSelected(voucher: VoucherSummary)
        fun onManualVoucherApplied(code: String)
    }

    private var allVouchers: List<VoucherSummary> = ArrayList()
    private var cartTotal: Double = 0.0
    private var listener: OnVoucherSelectedListener? = null

    private lateinit var rvVouchers: RecyclerView
    private lateinit var etManualCode: EditText
    private lateinit var btnApplyManual: Button

    companion object {
        fun newInstance(vouchers: List<VoucherSummary>, cartTotal: Double): VoucherSelectionBottomSheet {
            val fragment = VoucherSelectionBottomSheet()
            fragment.allVouchers = vouchers
            fragment.cartTotal = cartTotal
            return fragment
        }
    }

    fun setListener(listener: OnVoucherSelectedListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voucher_bottom_sheet, container, false)
        rvVouchers = view.findViewById(R.id.rvVouchers)
        etManualCode = view.findViewById(R.id.etManualCode)
        btnApplyManual = view.findViewById(R.id.btnApplyManual)

        rvVouchers.layoutManager = LinearLayoutManager(requireContext())

        btnApplyManual.setOnClickListener {
            val code = etManualCode.text.toString().trim()
            if (code.isNotEmpty() && listener != null) {
                listener?.onManualVoucherApplied(code.uppercase(Locale.getDefault()))
                dismiss()
            } else {
                Toast.makeText(context, "Vui lòng nhập mã hợp lệ", Toast.LENGTH_SHORT).show()
            }
        }

        setupAdapter()

        return view
    }

    private fun setupAdapter() {
        val available = ArrayList<VoucherItem>()
        val disabled = ArrayList<VoucherItem>()

        for (v in allVouchers) {
            if ("AVAILABLE" != v.status) {
                disabled.add(VoucherItem(v, 0.0, "Chỉ áp dụng với mã khả dụng / Còn hạn."))
                continue
            }
            if (v.minOrder > 0 && cartTotal < v.minOrder) {
                disabled.add(
                    VoucherItem(
                        v,
                        0.0,
                        "Đơn tối thiểu " + String.format(Locale.getDefault(), "%,.0fđ", v.minOrder)
                    )
                )
                continue
            }

            var actualDiscount = 0.0
            if ("PERCENTAGE" == v.discountType) {
                actualDiscount = (cartTotal * v.discountValue) / 100.0
            } else {
                actualDiscount = v.discountValue
            }
            if (v.maxDiscount > 0 && actualDiscount > v.maxDiscount) {
                actualDiscount = v.maxDiscount
            }
            available.add(VoucherItem(v, actualDiscount, null))
        }

        Collections.sort(available) { a, b -> b.actualDiscount.compareTo(a.actualDiscount) }

        val combined = ArrayList<VoucherItem>()
        combined.addAll(available)
        combined.addAll(disabled)

        rvVouchers.adapter = SheetAdapter(combined)
    }

    private inner class VoucherItem(
        val summary: VoucherSummary,
        val actualDiscount: Double,
        val disabledReason: String?
    )

    private inner class SheetAdapter(private val items: List<VoucherItem>) :
        RecyclerView.Adapter<SheetAdapter.VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_voucher, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val v = item.summary

            holder.tvCode.text = v.code
            holder.tvDescription.text = v.description
            holder.tvValidTo.text = "HSD: ${if (v.endDate != null) v.endDate.substring(0, 10) else "N/A"}"

            if (item.disabledReason == null) {
                // Khả dụng
                holder.itemView.alpha = 1.0f
                holder.tvStatus.text = "Khả dụng"
                holder.tvStatus.backgroundTintList = ColorStateList.valueOf(0x334CAF50)
                holder.tvStatus.setTextColor(-0xb350b0) // 0xFF4CAF50
                holder.tvDiscount.text = String.format(Locale.getDefault(), "-%,.0fđ", item.actualDiscount)

                holder.itemView.setOnClickListener {
                    listener?.onVoucherSelected(v)
                    dismiss()
                }
            } else {
                // Disabled
                holder.itemView.alpha = 0.5f
                holder.tvStatus.text = item.disabledReason
                holder.tvStatus.backgroundTintList = ColorStateList.valueOf(0x33888888.toInt())
                holder.tvStatus.setTextColor(0xFF888888.toInt())

                val discount = if ("PERCENTAGE".equals(v.discountType, ignoreCase = true)) {
                    "-${v.discountValue.toInt()}%"
                } else {
                    String.format(Locale.getDefault(), "-%,.0fđ", v.discountValue)
                }
                holder.tvDiscount.text = discount
                holder.itemView.setOnClickListener(null)
            }
        }

        override fun getItemCount(): Int = items.size

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvCode: TextView = itemView.findViewById(R.id.tvCode)
            val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
            val tvValidTo: TextView = itemView.findViewById(R.id.tvValidTo)
            val tvDiscount: TextView = itemView.findViewById(R.id.tvDiscount)
            val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        }
    }
}
