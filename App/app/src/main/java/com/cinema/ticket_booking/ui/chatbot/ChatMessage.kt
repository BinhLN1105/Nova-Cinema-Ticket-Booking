package com.cinema.ticket_booking.ui.chatbot

class ChatMessage {
    val text: String
    val isUser: Boolean
    val isLoading: Boolean // Used for "typing..." indicator

    constructor(text: String, isUser: Boolean) {
        this.text = text
        this.isUser = isUser
        this.isLoading = false
    }

    constructor(isLoading: Boolean) {
        this.isLoading = isLoading
        this.isUser = false
        this.text = ""
    }
}
