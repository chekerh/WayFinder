package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.AuthViewModel
import tn.esprit.wayfinder.viewmodels.PasswordResetResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))

    var email by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var showOtpFields by rememberSaveable { mutableStateOf(false) }

    val requestOtpResult by authViewModel.requestPasswordResetOtpResult.collectAsState()
    val resetPasswordResult by authViewModel.passwordResetResult.collectAsState()

    LaunchedEffect(requestOtpResult) {
        when (val result = requestOtpResult) {
            is PasswordResetResult.Success -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                authViewModel.clearMessages()
                showOtpFields = true
            }
            is PasswordResetResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                authViewModel.clearMessages()
            }
            else -> {}
        }
    }

    LaunchedEffect(resetPasswordResult) {
        when (val result = resetPasswordResult) {
            is PasswordResetResult.Success -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                authViewModel.clearMessages()
                navController.popBackStack("login", inclusive = false)
            }
            is PasswordResetResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                authViewModel.clearMessages()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringTranslator.translate(context, "Réinitialiser le mot de passe")) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                StringTranslator.translate(context, "Réinitialiser le mot de passe"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(StringTranslator.translate(context, "Email")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                enabled = !showOtpFields
            )

            if (showOtpFields) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 4) otpCode = it },
                    label = { Text(StringTranslator.translate(context, "Code OTP (4 chiffres)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(StringTranslator.translate(context, "Nouveau mot de passe")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (!showOtpFields) {
                        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Toast.makeText(context, "Veuillez saisir une adresse email valide.", Toast.LENGTH_SHORT).show()
                        } else {
                            authViewModel.requestPasswordResetOtp(context, email.trim())
                        }
                    } else {
                        if (otpCode.length != 4) {
                            Toast.makeText(context, "Veuillez saisir un code OTP à 4 chiffres.", Toast.LENGTH_SHORT).show()
                        } else if (newPassword.length < 6) {
                            Toast.makeText(context, "Le mot de passe doit contenir au moins 6 caractères.", Toast.LENGTH_SHORT).show()
                        } else {
                            authViewModel.resetPassword(context, email.trim(), otpCode.trim(), newPassword)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = requestOtpResult !is PasswordResetResult.Loading && resetPasswordResult !is PasswordResetResult.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (requestOtpResult is PasswordResetResult.Loading || resetPasswordResult is PasswordResetResult.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        StringTranslator.translate(
                            context,
                            if (!showOtpFields) "Envoyer le code" else "Réinitialiser le mot de passe"
                        ),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (showOtpFields) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { showOtpFields = false; otpCode = ""; newPassword = "" }) {
                    Text(StringTranslator.translate(context, "Annuler"))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    WayFinderTheme {
        ForgotPasswordScreen(rememberNavController())
    }
}

