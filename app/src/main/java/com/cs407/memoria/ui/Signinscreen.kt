package com.cs407.memoria.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cs407.memoria.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private const val TAG = "SignInScreen"

@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    onSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Configure Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.cs407.memoria.R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    // Launcher for Google Sign-In
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Result received with code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Account retrieved: ${account?.email}")
                val token = account?.idToken
                if (token != null) {
                    Log.d(TAG, "Token received, signing in with Firebase")
                    authViewModel.signInWithGoogle(token) { success, error ->
                        isLoading = false
                        Log.d(TAG, "Firebase sign in result - Success: $success, Error: $error")
                        if (success) {
                            onSignInSuccess()
                        } else {
                            errorMessage = error ?: "Sign in failed"
                        }
                    }
                } else {
                    isLoading = false
                    errorMessage = "Failed to get ID token"
                    Log.e(TAG, "ID token is null")
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Google sign in failed: ${e.message}"
                Log.e(TAG, "ApiException during sign in", e)
            }
        } else {
            isLoading = false
            Log.d(TAG, "Sign in cancelled or failed")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Memoria",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Sign in to start tracking your outfits",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    Log.d(TAG, "Launching Google Sign In")
                    launcher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Sign in with Google")
            }
        }

        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}