package com.example.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.model.AuthState
import com.example.fintrack.model.SessionManager
import com.example.fintrack.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application): AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val sessionManager = SessionManager(application)
    private val _uiState = MutableStateFlow(AuthState())
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    init {
        val savedToken = sessionManager.fetchAuthToken()
        // 🔹 FIX 1: SessionManager se saved user name fetch kar rahe hain
        val savedName = sessionManager.fetchUserName() ?: "User"

        if (!savedToken.isNullOrEmpty()) {
            _uiState.value = AuthState(
                isLoggedIn = true,
                userName = savedName
            )
        }
    }

    fun login(email: String, pass: String) {

        // 🔹 FIX 2: Valid credentials check (&& ko || kar diya hai)
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter valid credentials")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = authRepository.login(email, pass)

            result.onSuccess { idToken ->
                val extractedName = email.substringBefore("@").replaceFirstChar { it.uppercase() }

                // 🔹 FIX 3: Token ke sath UserName bhi SessionManager mein save kar rahe hain
                sessionManager.saveAuthToken(idToken)
                sessionManager.saveUserName(extractedName)
                sessionManager.saveUserEmail(email)

                _uiState.value = AuthState(
                    isLoggedIn = true,
                    userName = extractedName,
                    email = email,
                    isLoading = false
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.localizedMessage ?: "Login failed"
                )
            }
        }
    }

    fun signUp(name: String, email: String, password: String, onSignUpSuccess: () -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()){
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill all the fields")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = authRepository.signUp(email, password)

            result.onSuccess { message ->
                // 🔹 FIX 4: Signup par enter kiya gaya real Name save kar rahe hain
                val formattedName = name.trim().replaceFirstChar { it.uppercase() }
                sessionManager.saveUserName(formattedName)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                onSignUpSuccess()
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.localizedMessage ?: "Sign up failed"
                )
            }
        }
    }

    fun logout() {
        sessionManager.logout()
        _uiState.value = AuthState(isLoggedIn = false)
    }

    fun verifyOtp(email: String, code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.confirmSignUp(email, code)
            if (result.isSuccess) {
                onResult(true, result.getOrDefault("Verified"))
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Verification failed")
            }
        }
    }
}