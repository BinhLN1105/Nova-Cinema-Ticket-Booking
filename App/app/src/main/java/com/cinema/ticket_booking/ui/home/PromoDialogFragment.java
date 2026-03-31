package com.cinema.ticket_booking.ui.home;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.cinema.ticket_booking.databinding.DialogPromoPopupBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.bumptech.glide.Glide;
import com.cinema.ticket_booking.data.model.response.PromotionResponse;
import com.cinema.ticket_booking.R;
import java.io.Serializable;

/**
 * Fragment that displays a promotional popup with a "Don't show today" option.
 * Implementation based on Galaxy Cinema design.
 */
public class PromoDialogFragment extends DialogFragment {

    public static PromoDialogFragment newInstance(PromotionResponse promotion) {
        PromoDialogFragment fragment = new PromoDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("promotion", promotion);
        fragment.setArguments(args);
        return fragment;
    }

    private DialogPromoPopupBinding binding;
    private static final String PREFS_NAME = "PromoPrefs";
    private static final String KEY_LAST_SHOWN_DATE = "last_shown_date";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogPromoPopupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCloseTop.setOnClickListener(v -> dismiss());
        binding.btnClose.setOnClickListener(v -> {
            if (binding.cbDontShowToday.isChecked()) {
                saveDismissDate();
            }
            dismiss();
        });

        // Load dynamic promotion data
        if (getArguments() != null) {
            PromotionResponse promotion = (PromotionResponse) getArguments().getSerializable("promotion");
            if (promotion != null && promotion.getImageUrl() != null) {
                Glide.with(this)
                    .load(promotion.getImageUrl())
                    .placeholder(R.drawable.placeholder_hero)
                    .into(binding.ivPromo);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Pro-tip: Make standard dialog background transparent to allow CardView round corners
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void saveDismissDate() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit().putString(KEY_LAST_SHOWN_DATE, today).apply();
    }

    /**
     * Checks if the popup should be shown based on Saved SharedPreferences date.
     * Pro-tip: Simple String comparison ("yyyy-MM-dd") for reliable daily dismissal.
     */
    public static boolean shouldShow(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastShown = prefs.getString(KEY_LAST_SHOWN_DATE, "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return !today.equals(lastShown);
    }
}
