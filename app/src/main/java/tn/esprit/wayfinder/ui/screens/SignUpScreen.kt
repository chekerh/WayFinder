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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.SignUpRequest
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.GoogleSignInHelper
import tn.esprit.wayfinder.viewmodels.AuthViewModel
import tn.esprit.wayfinder.viewmodels.SignUpResult
import tn.esprit.wayfinder.viewmodels.GoogleSignInResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))

    var email by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val signUpResult by authViewModel.signUpResult.collectAsState()
    val googleSignInResult by authViewModel.googleSignInResult.collectAsState()

    // Google Client ID from strings.xml
    val googleClientId = context.getString(R.string.google_client_id_android)

    // Google Sign-In launcher - Simplified like iOS implementation
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("SignUpScreen", "Google Sign-In result received. Result code: ${result.resultCode}")
        
        if (result.data == null) {
            android.util.Log.e("SignUpScreen", "Result data is null")
            Toast.makeText(context, "Erreur: Aucune donnée reçue de Google Sign-In", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            
            if (idToken != null) {
                android.util.Log.d("SignUpScreen", "ID Token obtained, length: ${idToken.length}")
                authViewModel.googleSignIn(context, idToken)
            } else {
                android.util.Log.e("SignUpScreen", "ID Token is null")
                Toast.makeText(context, "Échec: Token Google non disponible", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            android.util.Log.e("SignUpScreen", "Google Sign-In ApiException: ${e.statusCode} - ${e.message}", e)
            val errorMsg = when (e.statusCode) {
                10 -> {
                    android.util.Log.e("SignUpScreen", "DEVELOPER_ERROR: Vérifiez API Google Sign-In activée et OAuth Consent Screen configuré")
                    """
                        Erreur DEVELOPER_ERROR (10)
                        
                        Vérifiez dans Google Cloud Console:
                        • API Google Sign-In activée?
                        • OAuth Consent Screen configuré?
                        • Package: tn.esprit.WayFinder
                        • SHA-1: 9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94
                        
                        Attendez 10-15 min après modification.
                    """.trimIndent()
                }
                12501 -> "Connexion annulée par l'utilisateur"
                7 -> "Erreur réseau. Vérifiez votre connexion Internet."
                8 -> "Erreur interne Google. Réessayez plus tard."
                16 -> "Un autre appel est en cours. Réessayez."
                else -> "Erreur Google Sign-In (${e.statusCode}): ${e.message ?: "Erreur inconnue"}"
            }
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("SignUpScreen", "Unexpected error during Google Sign-In", e)
            Toast.makeText(context, "Erreur inattendue: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(signUpResult) {
        when (val result = signUpResult) {
            is SignUpResult.OTPSent -> {
                Toast.makeText(context, "Code OTP envoyé à ${result.email}", Toast.LENGTH_SHORT).show()
                // Navigate to OTP verification screen with user data
                navController.currentBackStackEntry?.savedStateHandle?.apply {
                    set("signup_email", result.email)
                    set("signup_firstName", firstName)
                    set("signup_lastName", lastName)
                    set("signup_password", password)
                }
                navController.navigate("otp_screen") {
                    popUpTo("signup_screen") { inclusive = false }
                }
                authViewModel.clearMessages()
            }
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

    LaunchedEffect(googleSignInResult) {
        when (val result = googleSignInResult) {
            is GoogleSignInResult.Success -> {
                Toast.makeText(context, "Connexion Google réussie!", Toast.LENGTH_SHORT).show()
                navController.navigate(result.navigateTo) {
                    popUpTo("signup_screen") { inclusive = true }
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(32.dp))
        Spacer(modifier = Modifier.height(140.dp))
        Spacer(modifier = Modifier.height(32.dp))


        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                emailError = null
            },
            label = { Text("Email", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        passwordError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                emailError = when {
                    email.isBlank() -> "Veuillez saisir votre adresse email."
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Veuillez saisir une adresse email valide."
                    else -> null
                }

                when {
                    email.isBlank() || firstName.isBlank() || lastName.isBlank() -> {
                        Toast.makeText(context, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
                    }
                    emailError != null -> Unit // Error already shown
                    passwordError != null || confirmPasswordError != null -> Unit
                    else -> {
                        // Send OTP to email instead of registering directly
                        authViewModel.sendOTPForRegistration(email.trim().lowercase())
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
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
                            android.util.Log.e("SignUpScreen", "Error launching Google Sign-In", e)
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
                    Text(
                        "Google", 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { navController.navigate("login") }) {
            Text(
                "Already have an account? Log In",
                color = MaterialTheme.colorScheme.primary
            )
        }
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
