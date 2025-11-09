package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.LoginRequest
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) { // Removed viewModel from signature
    val context = LocalContext.current
    // FIX: Correctly initialize the ViewModel using the factory with context.
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by authViewModel.loading
    val loginResult by authViewModel.loginResult
    val errorMessage by authViewModel.errorMessage

    // Handle navigation after login result changes
    LaunchedEffect(loginResult) {
        loginResult?.onSuccess { response ->
            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
            if (response.onboardingCompleted) {
                navController.navigate("main_app") { popUpTo(0) }
            } else {
                navController.navigate("ai_onboarding_form") { popUpTo(0) }
            }
            authViewModel.clearMessages() // Reset state
        }
    }
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F8FF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Bienvenue sur Wayfindr", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        // Image placeholder
        Spacer(modifier = Modifier.height(140.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Veuillez vous connecter pour commencer", fontSize = 16.sp, color = Color.Gray)
        Text("Powered by Gemini", fontSize = 14.sp, color = Color(0xFFF44336), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val request = LoginRequest(username, password)
                authViewModel.login(context, request)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Se connecter", fontSize = 16.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { navController.navigate("signup_screen") }) {
                Text("Pas de compte ?")
            }
            TextButton(onClick = { /* TODO */ }) {
                Text("Mot de passe oublié ?")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Ou", color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        // Social Logins would be implemented here
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    WayFinderTheme {
        LoginScreen(rememberNavController())
    }
}
