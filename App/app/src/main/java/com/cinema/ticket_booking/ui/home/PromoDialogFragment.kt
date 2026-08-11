package com.cinema.ticket_booking.ui.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.cinema.ticket_booking.R
import com.cinema.ticket_booking.data.model.response.PromotionResponse
import com.cinema.ticket_booking.databinding.DialogPromoPopupBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context

class PromoDialogFragment : DialogFragment() {

    companion object {
        private const val PREFS_NAME = "PromoPrefs"
        private const val KEY_LAST_SHOWN_DATE = "last_shown_date"

        fun newInstance(promotion: PromotionResponse): PromoDialogFragment {
            return PromoDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("promotion", promotion)
                }
            }
        }

        fun shouldShow(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastShown = prefs.getString(KEY_LAST_SHOWN_DATE, "") ?: ""
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            return today != lastShown
        }
    }

    private var _binding: DialogPromoPopupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPromoPopupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCloseTop.setOnClickListener { dismiss() }
        binding.btnClose.setOnClickListener {
            if (binding.cbDontShowToday.isChecked) {
                saveDismissDate()
            }
            dismiss()
        }

        // Load dynamic promotion data
        arguments?.let {
            @Suppress("DEPRECATION")
            val promotion = it.getSerializable("promotion") as? PromotionResponse
            if (promotion != null && !promotion.imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(promotion.imageUrl)
                    .placeholder(R.drawable.placeholder_hero)
                    .into(binding.ivPromo)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun saveDismissDate() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString(KEY_LAST_SHOWN_DATE, today).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
