package com.example.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.fintrack.ui.theme.FinTrackTheme
import com.example.fintrack.ui.view.LoginScreen
import com.example.fintrack.ui.view.MainScreen
import com.example.fintrack.ui.view.SignUpScreen
import com.example.fintrack.viewmodel.AuthViewModel
import com.example.fintrack.viewmodel.ChatViewModel
import com.example.fintrack.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val expenseViewModel: ExpenseViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        enableEdgeToEdge()
        setContent {
            FinTrackTheme {
                val authState by authViewModel.uiState.collectAsState()
                var isSigninUp by remember { mutableStateOf(false) }

                LaunchedEffect(authState.isLoggedIn) {
                    if (authState.isLoggedIn) {
                        // Jab user fresh Login kare ya app open ho with active session
                        expenseViewModel.onUserSessionChanged()
                    } else {
                        // Jab user Logout ho jaye, UI list instantly clear kar do
                        expenseViewModel.onUserSessionChanged()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!authState.isLoggedIn){
                        if (isSigninUp) {
                            SignUpScreen(
                                authViewModel = authViewModel,
                                onNavigateToLogin = {isSigninUp = false}
                            )
                        }else{
                            LoginScreen(
                                authViewModel = authViewModel,
                                expenseViewModel = expenseViewModel,
                                onLoginSuccess = {},
                                onNavigateToSignUp = {isSigninUp = true}
                            )
                        }
                    } else {
                        // Pass both ViewModels here safely
                        MainScreen(
                            expenseViewModel = expenseViewModel,
                            chatViewModel = chatViewModel,
                            authViewModel = authViewModel,
                            userName = authState.userName.ifBlank { "User" }
                        )
                    }
                }
            }
        }
    }
}