package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import tn.esprit.wayfinder.viewmodels.SignUpResult

@Composable
fun VerificationScreenOTP(navController: NavController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val signUpResult by authViewModel.signUpResult.collectAsState()
    
    var otpValue by remember { mutableStateOf("") }
    
    // Get signup data from navigation state
    val email = remember {
        navController.previousBackStackEntry?.savedStateHandle?.get<String>("signup_email") ?: ""
    }
    val firstName = remember {
        navController.previousBackStackEntry?.savedStateHandle?.get<String>("signup_firstName") ?: ""
    }
    val lastName = remember {
        navController.previousBackStackEntry?.savedStateHandle?.get<String>("signup_lastName") ?: ""
    }
    val password = remember {
        navController.previousBackStackEntry?.savedStateHandle?.get<String>("signup_password") ?: ""
    }
    
    LaunchedEffect(signUpResult) {
        when (val result = signUpResult) {
            is SignUpResult.Success -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                authViewModel.clearMessages()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
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
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.height(32.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = StringTranslator.translate(context, "Vérification Email"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = StringTranslator.translate(context, "Entrez le code OTP envoyé à $email"),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))

            OtpTextField(otpText = otpValue, onOtpTextChange = { otpValue = it }, length = 4)

            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = {
                    if (email.isNotBlank()) {
                        authViewModel.sendOTPForRegistration(email)
                    }
                }
            ) {
                Text(
                    text = StringTranslator.translate(context, "Renvoyer le code"),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (otpValue.length == 4 && email.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank() && password.isNotBlank()) {
                        authViewModel.registerWithOTP(
                            email = email,
                            firstName = firstName,
                            lastName = lastName,
                            password = password,
                            otpCode = otpValue
                        )
                    } else {
                        Toast.makeText(
                            context,
                            StringTranslator.translate(context, "Veuillez entrer le code OTP complet"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = signUpResult !is SignUpResult.Loading && otpValue.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (signUpResult is SignUpResult.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        StringTranslator.translate(context, "Vérifier et créer le compte"),
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                StringTranslator.translate(context, "En vous inscrivant, vous acceptez nos conditions d'utilisation"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OtpTextField(
    otpText: String,
    onOtpTextChange: (String) -> Unit,
    length: Int = 4
) {
    BasicTextField(
        value = otpText,
        onValueChange = {
            if (it.length <= length) {
                onOtpTextChange(it)
            }
        },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(length) {
                    val char = otpText.getOrNull(it)
                    val isFocused = otpText.length == it
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(horizontal = 4.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color(0xFF1976D2) else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char?.toString() ?: "-", 
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun VerificationScreenOTPPreview() {
    WayFinderTheme {
        VerificationScreenOTP(rememberNavController())
    }
}
