package com.example.fintrack.model

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("FinTrackPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val USER_TOKEN = "auth_token"
        private const val USER_NAME = "user_name" // 👈 Key for User Name
        private const val USER_EMAIL = "user_email"
    }

    // 🔹 Token Handling
    fun saveAuthToken(token: String) {
        prefs.edit().putString(USER_TOKEN, token).apply()
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    // 🔹 User Name Handling (Ye naye add kiye hain)
    fun saveUserName(name: String) {
        prefs.edit().putString(USER_NAME, name).apply()
    }

    fun fetchUserName(): String? {
        return prefs.getString(USER_NAME, null)
    }

    // 🔹 User Email Handling (👈 ADDED THIS)
    fun saveUserEmail(email: String) {
        prefs.edit().putString(USER_EMAIL, email).apply()
    }

    fun fetchUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }

    // 🔹 Logout (Token + UserName dono clear kar dega)
    fun logout() {
        prefs.edit().clear().apply()
    }
}