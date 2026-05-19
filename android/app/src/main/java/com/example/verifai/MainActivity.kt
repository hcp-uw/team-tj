package com.example.verifai

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.verifai.auth.AuthViewModel
import com.example.verifai.notifications.NotificationHelper
import com.example.verifai.ui.VerifAIApp
import com.example.verifai.ui.login.LoginScreen
import com.example.verifai.ui.theme.VerifAITheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannel(this)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            VerifAITheme {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.uiState.collectAsState()

                if (authState.user == null) {
                    LoginScreen(
                        authState = authState,
                        onSignIn = authViewModel::signIn,
                        onSignUp = authViewModel::signUp,
                        onClearError = authViewModel::clearError,
                    )
                } else {
                    VerifAIApp(
                        userName = authState.user?.email?.substringBefore('@') ?: "User",
                        onSignOut = authViewModel::signOut,
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (NotificationHelper.canPostNotifications(this)) return
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
