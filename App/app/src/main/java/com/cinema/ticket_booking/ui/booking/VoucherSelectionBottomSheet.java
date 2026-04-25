package com.cinema.ticket_booking.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.VoucherSummary;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VoucherSelectionBottomSheet extends BottomSheetDialogFragment {

    public interface OnVoucherSelectedListener {
        void onVoucherSelected(VoucherSummary voucher);
        void onManualVoucherApplied(String code);
    }

    private List<VoucherSummary> allVouchers = new ArrayList<>();
    private double cartTotal;
    private OnVoucherSelectedListener listener;

    private RecyclerView rvVouchers;
    private EditText etManualCode;
    private Button btnApplyManual;

    public static VoucherSelectionBottomSheet newInstance(List<VoucherSummary> vouchers, double cartTotal) {
        VoucherSelectionBottomSheet fragment = new VoucherSelectionBottomSheet();
        fragment.allVouchers = vouchers;
        fragment.cartTotal = cartTotal;
        return fragment;
    }

    public void setListener(OnVoucherSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voucher_bottom_sheet, container, false);
        rvVouchers = view.findViewById(R.id.rvVouchers);
        etManualCode = view.findViewById(R.id.etManualCode);
        btnApplyManual = view.findViewById(R.id.btnApplyManual);

        rvVouchers.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnApplyManual.setOnClickListener(v -> {
            String code = etManualCode.getText().toString().trim();
            if(!code.isEmpty() && listener != null) {
                listener.onManualVoucherApplied(code.toUpperCase());
                dismiss();
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập mã hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        setupAdapter();

        return view;
    }

    private void setupAdapter() {
        List<VoucherItem> available = new ArrayList<>();
        List<VoucherItem> disabled = new ArrayList<>();

        for (VoucherSummary v : allVouchers) {
            if (!"AVAILABLE".equals(v.getStatus())) {
                disabled.add(new VoucherItem(v, 0, "Chỉ áp dụng với mã khả dụng / Còn hạn."));
                continue;
            }
            if (v.getMinOrder() > 0 && cartTotal < v.getMinOrder()) {
                disabled.add(new VoucherItem(v, 0, "Đơn tối thiểu " + String.format("%,.0f₫", v.getMinOrder())));
                continue;
            }

            double actualDiscount = 0;
            if ("PERCENTAGE".equals(v.getDiscountType())) {
                actualDiscount = (cartTotal * v.getDiscountValue()) / 100.0;
            } else {
                actualDiscount = v.getDiscountValue();
            }
            if (v.getMaxDiscount() > 0 && actualDiscount > v.getMaxDiscount()) {
                actualDiscount = v.getMaxDiscount();
            }
            available.add(new VoucherItem(v, actualDiscount, null));
        }

        Collections.sort(available, (a, b) -> Double.compare(b.actualDiscount, a.actualDiscount));

        List<VoucherItem> combined = new ArrayList<>();
        combined.addAll(available);
        combined.addAll(disabled);

        rvVouchers.setAdapter(new SheetAdapter(combined));
    }

    private class VoucherItem {
        VoucherSummary summary;
        double actualDiscount;
        String disabledReason;

        VoucherItem(VoucherSummary summary, double actualDiscount, String disabledReason) {
            this.summary = summary;
            this.actualDiscount = actualDiscount;
            this.disabledReason = disabledReason;
        }
    }

    private class SheetAdapter extends RecyclerView.Adapter<SheetAdapter.VH> {
        private List<VoucherItem> items;

        SheetAdapter(List<VoucherItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            VoucherItem item = items.get(position);
            VoucherSummary v = item.summary;

            holder.tvCode.setText(v.getCode());
            holder.tvDescription.setText(v.getDescription());
            holder.tvValidTo.setText("HSD: " + (v.getEndDate() != null ? v.getEndDate().substring(0, 10) : "N/A"));

            if (item.disabledReason == null) {
                // Khả dụng
                holder.itemView.setAlpha(1.0f);
                holder.tvStatus.setText("Khả dụng");
                holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x334CAF50));
                holder.tvStatus.setTextColor(0xFF4CAF50);
                holder.tvDiscount.setText(String.format("-%,.0f₫", item.actualDiscount));
                
                holder.itemView.setOnClickListener(view -> {
                    if (listener != null) {
                        listener.onVoucherSelected(v);
                        dismiss();
                    }
                });
            } else {
                // Disabled
                holder.itemView.setAlpha(0.5f);
                holder.tvStatus.setText(item.disabledReason);
                holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x33888888));
                holder.tvStatus.setTextColor(0xFF888888);
                
                String discount;
                if ("PERCENTAGE".equalsIgnoreCase(v.getDiscountType())) {
                    discount = "-" + (int) v.getDiscountValue() + "%";
                } else {
                    discount = String.format("-%,.0f₫", v.getDiscountValue());
                }
                holder.tvDiscount.setText(discount);
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvCode, tvDescription, tvValidTo, tvDiscount, tvStatus;
            VH(@NonNull View itemView) {
                super(itemView);
                tvCode = itemView.findViewById(R.id.tvCode);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvValidTo = itemView.findViewById(R.id.tvValidTo);
                tvDiscount = itemView.findViewById(R.id.tvDiscount);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }
        }
    }
}
