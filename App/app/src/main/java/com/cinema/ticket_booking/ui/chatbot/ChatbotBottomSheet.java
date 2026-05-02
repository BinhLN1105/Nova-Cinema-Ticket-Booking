package com.cinema.ticket_booking.ui.chatbot;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cinema.ticket_booking.databinding.DialogChatbotBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatbotBottomSheet extends BottomSheetDialogFragment {

    private DialogChatbotBinding binding;
    private ChatbotViewModel viewModel;
    private ChatAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Nova Optimization: use transparent background for modern rounded corners
        setStyle(STYLE_NORMAL, com.cinema.ticket_booking.R.style.TransparentBottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogChatbotBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatbotViewModel.class);

        adapter = new ChatAdapter();
        binding.rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChat.setAdapter(adapter);

        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            if (!messages.isEmpty()) {
                binding.rvChat.smoothScrollToPosition(messages.size() - 1);
            }
        });

        binding.btnSend.setOnClickListener(v -> {
            String msg = binding.etMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                viewModel.sendMessage(msg);
                binding.etMessage.setText("");
            }
        });

        binding.btnClose.setOnClickListener(v -> dismiss());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // Cấu hình hành vi (Behavior) khi Dialog được hiển thị
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                // Ép buộc BottomSheet luôn ở trạng thái Mở rộng toàn bộ (Expanded)
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                // Bỏ qua trạng thái lấp lửng (Collapsed) để không bị kẹt khi vuốt xuống
                behavior.setSkipCollapsed(true);

                // Thiết lập chiều cao cho khớp với màn hình (Match Parent)
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        });

        // Cấu hình chế độ bàn phím: adjustResize giúp đẩy giao diện lên khi bàn phím
        // hiện ra
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
