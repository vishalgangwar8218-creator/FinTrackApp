package com.example.fintrack.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.api.RetrofitClient
import com.example.fintrack.model.ChatMessage
import com.example.fintrack.model.ChatRequest
import com.example.fintrack.model.ChatUiState
import com.example.fintrack.model.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    message = "Hello! I'm your FinTrack AI agent. How can I help you analyze your budget today?",
                    isUser = false
                )
            )
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputChanged(newInput: String) {
        _uiState.update { it.copy(currentInput = newInput) }
    }

    fun sendMessage() {
        val input = _uiState.value.currentInput.trim()
        if (input.isBlank()) return

        // 🔹 Get logged in user email
        val currentUserId = sessionManager.fetchUserEmail() ?: "default_user"

        val userMessage = ChatMessage(message = input, isUser = true)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                currentInput = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            try {
                // 🔹 Send userId along with formatted query
                val requestBody = ChatRequest(
                    userId = currentUserId, // 👈 PASS USER ID TO LAMBDA
                    query = input
                )

                val response = RetrofitClient.instance.sendChatMessage(requestBody)

                val aiReply = if (response.isSuccessful && response.body() != null) {
                    // Safe Elvis operator (?:) added so it NEVER returns null or empty String
                    val rawReply = response.body()?.reply
                    if (!rawReply.isNullOrBlank()) {
                        rawReply
                    } else {
                        "Received empty response from server."
                    }
                }   
                else if (response.code() == 429) {
                    "AI rate limit reached. Please wait 10-15 seconds and try again."
                } else {
                    val errBody = response.errorBody()?.string()
                    Log.e("CHAT_API_ERROR", "HTTP Code: ${response.code()} | Body: $errBody")
                    "Server error ${response.code()}: $errBody"
                }

                val aiMessage = ChatMessage(message = aiReply, isUser = false)

                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CHAT_API_EXCEPTION", "Exception: ${e.message}")
                val aiMessage = ChatMessage(
                    message = "Connection Error: ${e.localizedMessage ?: "Unable to reach server"}",
                    isUser = false
                )

                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiMessage,
                        isLoading = false
                    )
                }
            }
        }
    }
}