package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.models.SignUpRequest
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.viewmodels.AuthViewModel
import tn.esprit.wayfinder.viewmodels.SignUpResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))

    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val signUpResult by authViewModel.signUpResult.collectAsState()

    LaunchedEffect(signUpResult) {
        when (val result = signUpResult) {
            is SignUpResult.Success -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                authViewModel.clearMessages()
                navController.navigate("login") { 
                    popUpTo("signup_screen") { inclusive = true }
                }
            }
            is SignUpResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                authViewModel.clearMessages()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F8FF))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Spacer(modifier = Modifier.height(140.dp))
        Spacer(modifier = Modifier.height(32.dp))


        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it.trimStart() },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )
        passwordError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )
        confirmPasswordError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                passwordError = validatePassword(password)
                confirmPasswordError = when {
                    confirmPassword.isBlank() -> "Veuillez confirmer votre mot de passe."
                    confirmPassword != password -> "Les mots de passe ne correspondent pas."
                    else -> null
                }

                when {
                    username.isBlank() || email.isBlank() || firstName.isBlank() || lastName.isBlank() -> {
                        Toast.makeText(context, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
                    }
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        Toast.makeText(context, "Veuillez saisir une adresse e-mail valide.", Toast.LENGTH_SHORT).show()
                    }
                    passwordError != null || confirmPasswordError != null -> Unit
                    else -> {
                        val request = SignUpRequest(
                            username = username.trim(),
                            email = email.trim(),
                            first_name = firstName.trim(),
                            last_name = lastName.trim(),
                            password = password
                        )
                        authViewModel.signup(request)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = signUpResult !is SignUpResult.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
            shape = RoundedCornerShape(12.dp)
        ) {
             if (signUpResult is SignUpResult.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Register", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Log In")
        }
    }
}

private fun validatePassword(password: String): String? {
    if (password.length < 6) {
        return "Le mot de passe doit contenir au moins 6 caractères."
    }
    val uppercase = Regex("[A-Z]")
    val lowercase = Regex("[a-z]")
    val digit = Regex("[0-9]")
    val special = Regex("[^A-Za-z0-9]")
    return when {
        !uppercase.containsMatchIn(password) -> "Le mot de passe doit contenir une lettre majuscule."
        !lowercase.containsMatchIn(password) -> "Le mot de passe doit contenir une lettre minuscule."
        !digit.containsMatchIn(password) -> "Le mot de passe doit contenir un chiffre."
        !special.containsMatchIn(password) -> "Le mot de passe doit contenir un caractère spécial."
        else -> null
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    WayFinderTheme {
        SignUpScreen(rememberNavController())
    }
}
