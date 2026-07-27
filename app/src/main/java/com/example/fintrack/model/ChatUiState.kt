package com.example.fintrack.model

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(message = "Hello! I'm your FinTrack AI agent. How can I help you analyze your budget today?", isUser = false)
    ),
    val currentInput: String = "",
    val isLoading: Boolean = false
)