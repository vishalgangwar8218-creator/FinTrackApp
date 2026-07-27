package com.example.fintrack.model

data class AuthState(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)