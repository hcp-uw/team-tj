package com.example.verifai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.verifai.auth.AuthViewModel
import com.example.verifai.ui.login.LoginScreen
import com.example.verifai.ui.theme.VerifAITheme
import androidx.compose.ui.unit.dp
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        onClearError = authViewModel::clearError
                    )
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Greeting(
                            name = authState.user?.email?.substringBefore('@') ?: "User",
                            modifier = Modifier.padding(innerPadding),
                            onSignOut = authViewModel::signOut
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null
) {
    Column(modifier = modifier.padding(24.dp)) {
        if (onSignOut != null) {
            androidx.compose.material3.TextButton(onClick = onSignOut) {
                Text(text = "Sign out")
            }
        }
        Text(text = "Hello $name!")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VerifAITheme {
        Greeting("Android")
    }
}
