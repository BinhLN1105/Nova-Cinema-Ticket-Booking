package com.cinema.ticket_booking.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cinema.ticket_booking.R;
import com.cinema.ticket_booking.data.model.response.VoucherSummary;
import com.cinema.ticket_booking.databinding.FragmentVoucherBinding;
import com.cinema.ticket_booking.util.SnackbarHelper;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VoucherFragment extends Fragment {

    private FragmentVoucherBinding binding;
    private VoucherViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVoucherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VoucherViewModel.class);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.btnApplyVoucher.setOnClickListener(v -> {
            String code = binding.etVoucherCode.getText() != null ? binding.etVoucherCode.getText().toString().trim() : "";
            if (code.isEmpty()) {
                SnackbarHelper.showError(binding.getRoot(), "Vui lòng nhập mã voucher");
            } else {
                SnackbarHelper.showSuccess(binding.getRoot(), "Tính năng nạp thẻ đang được phát triển!");
                binding.etVoucherCode.setText("");
            }
        });

        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.loadVouchers();

        viewModel.getVouchers().observe(getViewLifecycleOwner(), resource -> {
            binding.progressBar.setVisibility(View.GONE);
            if (resource.isSuccess() && resource.data != null
                    && resource.data.getContent() != null) {
                List<VoucherSummary> vouchers = resource.data.getContent();
                if (vouchers.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                    binding.rvVouchers.setVisibility(View.GONE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                    binding.rvVouchers.setVisibility(View.VISIBLE);
                    binding.rvVouchers.setAdapter(new VoucherAdapter(vouchers));
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ── Inner Adapter ────────────────────────────────────────────────────────
    static class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VH> {
        private final List<VoucherSummary> items;

        VoucherAdapter(List<VoucherSummary> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_voucher, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            VoucherSummary v = items.get(position);
            holder.tvCode.setText(v.getCode());
            holder.tvDescription.setText(v.getDescription());
            holder.tvValidTo.setText("HSD: " + (v.getValidTo() != null
                    ? v.getValidTo().substring(0, 10)
                    : "N/A"));

            String discount;
            if ("PERCENTAGE".equalsIgnoreCase(v.getDiscountType())) {
                discount = "-" + (int) v.getDiscountValue() + "%";
            } else {
                discount = String.format("-%,.0f₫", v.getDiscountValue());
            }
            holder.tvDiscount.setText(discount);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvCode, tvDescription, tvValidTo, tvDiscount;

            VH(@NonNull View itemView) {
                super(itemView);
                tvCode = itemView.findViewById(R.id.tvCode);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvValidTo = itemView.findViewById(R.id.tvValidTo);
                tvDiscount = itemView.findViewById(R.id.tvDiscount);
            }
        }
    }
}
