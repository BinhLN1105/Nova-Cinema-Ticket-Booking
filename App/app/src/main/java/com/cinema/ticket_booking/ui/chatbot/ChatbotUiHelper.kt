package com.cinema.ticket_booking.ui.chatbot

import android.os.Bundle
import android.view.View
import com.cinema.ticket_booking.util.SnackbarHelper

object ChatbotUiHelper {

    fun createChatAdapter(
        viewModel: ChatbotViewModel,
        isViewActive: () -> Boolean,
        rootView: () -> View,
        onNavigateToConfirm: (Bundle) -> Unit
    ): ChatAdapter {
        lateinit var chatAdapter: ChatAdapter
        chatAdapter = ChatAdapter { draftId, position ->
            chatAdapter.setDraftConfirmingState(position, true)
            viewModel.prepareDraftBooking(draftId) { isSuccess, errorMessage, quote ->
                if (isViewActive()) {
                    chatAdapter.setDraftConfirmingState(position, false)
                }
                if (isSuccess && quote != null) {
                    val bundle = Bundle().apply {
                        putParcelable("initialQuote", quote)
                        putLong("expireTime", System.currentTimeMillis() + 10 * 60 * 1000)
                    }
                    onNavigateToConfirm(bundle)
                } else {
                    val msg = errorMessage ?: "Đơn vé nháp không tồn tại hoặc đã hết hạn (tối đa 10 phút)."
                    SnackbarHelper.showError(rootView(), msg)
                }
            }
        }
        return chatAdapter
    }

    fun createSuggestionAdapter(viewModel: ChatbotViewModel): SuggestionChipAdapter {
        return SuggestionChipAdapter(SuggestionChipAdapter.getDefaultPrompts()) { prompt ->
            viewModel.sendMessage(prompt.queryText)
        }
    }
}
