package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.LoginRequest
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.viewmodels.AuthViewModel
import tn.esprit.wayfinder.viewmodels.LoginResult
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import tn.esprit.wayfinder.utils.GoogleSignInHelper
import tn.esprit.wayfinder.viewmodels.GoogleSignInResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val loginResult by authViewModel.loginResult.collectAsState()
    val googleSignInResult by authViewModel.googleSignInResult.collectAsState()

    // Google Client ID from strings.xml
    val googleClientId = context.getString(R.string.google_client_id_android)
    

    // Google Sign-In launcher - Simplified like iOS implementation
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("LoginScreen", "Google Sign-In result received. Result code: ${result.resultCode}")
        android.util.Log.d("LoginScreen", "Result data: ${result.data != null}")
        android.util.Log.d("LoginScreen", "Client ID used: ${googleClientId.take(50)}...")
        
        val idToken = GoogleSignInHelper.handleSignInIntent(result.data)
        
        if (idToken != null) {
            authViewModel.googleSignIn(context, idToken)
        } else {
            android.util.Log.e("LoginScreen", "Google Sign-In failed: No ID token received")
            Toast.makeText(
                context,
                "Échec de la connexion Google. Veuillez réessayer.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(loginResult) {
        when (val result = loginResult) {
            is LoginResult.Success -> {
                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                navController.navigate(result.navigateTo) {
                    popUpTo("login") { inclusive = true }
                }
                authViewModel.clearMessages()
            }
            is LoginResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                authViewModel.clearMessages()
            }
            else -> {}
        }
    }

    LaunchedEffect(googleSignInResult) {
        when (val result = googleSignInResult) {
            is GoogleSignInResult.Success -> {
                Toast.makeText(context, "Connexion Google réussie!", Toast.LENGTH_SHORT).show()
                navController.navigate(result.navigateTo) {
                    popUpTo("login") { inclusive = true }
                }
                authViewModel.clearMessages()
            }
            is GoogleSignInResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                authViewModel.clearMessages()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF4FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.wayfinder_logo),
                contentDescription = "Wayfindr logo",
                modifier = Modifier
                    .size(110.dp)
                    .padding(top = 16.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                "Bienvenue sur Wayfindr",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D47A1)
            )
            Text(
                "Veuillez vous connecter pour commencer",
                fontSize = 16.sp,
                color = Color(0xFF6B7280)
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Powered by Gemini",
                        fontSize = 14.sp,
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.SemiBold
                    )

                     OutlinedTextField(
                         value = username,
                         onValueChange = { username = it },
                         label = { Text("Nom d'utilisateur ou email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    )

                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Veuillez saisir votre identifiant et votre mot de passe.", Toast.LENGTH_SHORT).show()
                            } else {
                                val request = LoginRequest(username.trim(), password)
                                authViewModel.login(context, request)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = loginResult !is LoginResult.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (loginResult is LoginResult.Loading) {
                            CircularProgressIndicator(color = Color.Blue, modifier = Modifier.size(24.dp))
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

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            "OU",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF5F6F8), RoundedCornerShape(50))
                                .clickable {
                                    // Simplified like iOS - just check if Client ID is valid
                                    if (googleClientId.isBlank() || !googleClientId.contains(".apps.googleusercontent.com")) {
                                        Toast.makeText(context, "Google Client ID non configuré", Toast.LENGTH_LONG).show()
                                        return@clickable
                                    }
                                    
                                    try {
                                        val signInIntent = GoogleSignInHelper.getSignInIntent(context, googleClientId)
                                        googleSignInLauncher.launch(signInIntent)
                                    } catch (e: Exception) {
                                        android.util.Log.e("LoginScreen", "Error launching Google Sign-In", e)
                                        Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (googleSignInResult is GoogleSignInResult.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color(0xFF1976D2),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_google),
                                        contentDescription = "Google",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text("Google", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF5F6F8), RoundedCornerShape(50))
                                .clickable { /* TODO: Apple Sign-In */ }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_apple),
                                    contentDescription = "Apple",
                                    modifier = Modifier.size(22.dp)
                                )
                                Text("Apple", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    WayFinderTheme {
        LoginScreen(rememberNavController())
    }
}
