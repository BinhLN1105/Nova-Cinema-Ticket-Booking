package com.cinema.ticket_booking.ui.chatbot

import java.util.regex.Pattern

data class ChatMessage(
    val text: String = "",
    val isUser: Boolean = false,
    val isLoading: Boolean = false,
    val draftId: String? = null,
    var isConfirmingDraft: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor(text: String, isUser: Boolean) : this(
        text = text,
        isUser = isUser,
        isLoading = false,
        draftId = if (!isUser) extractDraftId(text) else null
    )

    constructor(isLoading: Boolean) : this(
        text = "",
        isUser = false,
        isLoading = isLoading,
        draftId = null
    )

    companion object {
        private val DRAFT_REGEX = Pattern.compile("Mã đặt vé tạm:\\s*#([a-f0-9\\-]+)", Pattern.CASE_INSENSITIVE)

        fun extractDraftId(text: String): String? {
            val matcher = DRAFT_REGEX.matcher(text)
            return if (matcher.find()) matcher.group(1) else null
        }

        fun createBotMessage(text: String): ChatMessage {
            return ChatMessage(text = text, isUser = false, isLoading = false, draftId = extractDraftId(text))
        }

        fun createUserMessage(text: String): ChatMessage {
            return ChatMessage(text = text, isUser = true, isLoading = false, draftId = null)
        }

        fun createLoadingMessage(): ChatMessage {
            return ChatMessage(text = "", isUser = false, isLoading = true, draftId = null)
        }
    }
}
