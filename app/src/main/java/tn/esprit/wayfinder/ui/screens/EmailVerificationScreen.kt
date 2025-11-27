package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    navController: NavController,
    email: String? = null,
    token: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val coroutineScope = rememberCoroutineScope()
    
    var verificationToken by remember { mutableStateOf(token ?: "") }
    var userEmail by remember { mutableStateOf(email ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }

    // Auto-verify if token is provided
    LaunchedEffect(token) {
        if (!token.isNullOrBlank()) {
            isLoading = true
            val success = try {
                authViewModel.verifyEmail(token)
            } catch (e: Exception) {
                false
            }
            if (success) {
                isVerified = true
                Toast.makeText(context, "Email vérifié avec succès!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Erreur lors de la vérification. Veuillez réessayer.", Toast.LENGTH_LONG).show()
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vérification d'email") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            if (isVerified) {
                // Success state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 64.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Email vérifié!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Votre adresse email a été vérifiée avec succès.",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                navController.navigate("home") {
                                    popUpTo("email_verification") { inclusive = true }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Continuer", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Verification form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Vérifiez votre email",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )

                        Text(
                            text = "Nous avons envoyé un lien de vérification à votre adresse email. Veuillez cliquer sur le lien dans l'email ou entrez le code de vérification ci-dessous.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = verificationToken,
                            onValueChange = { verificationToken = it },
                            label = { Text("Code de vérification") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Entrez le code de vérification") }
                        )

                        Button(
                            onClick = {
                                if (verificationToken.isBlank()) {
                                    Toast.makeText(context, "Veuillez entrer le code de vérification", Toast.LENGTH_SHORT).show()
                                } else {
                                    isLoading = true
                                    coroutineScope.launch {
                                        val success = try {
                                            authViewModel.verifyEmail(verificationToken)
                                        } catch (e: Exception) {
                                            false
                                        }
                                        isLoading = false
                                        if (success) {
                                            isVerified = true
                                            Toast.makeText(context, "Email vérifié avec succès!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Code de vérification invalide. Veuillez réessayer.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !isLoading && verificationToken.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Vérifier", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Vous n'avez pas reçu l'email?",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("Votre email") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("email@example.com") }
                        )

                        TextButton(
                            onClick = {
                                if (userEmail.isBlank()) {
                                    Toast.makeText(context, "Veuillez entrer votre email", Toast.LENGTH_SHORT).show()
                                } else {
                                    isLoading = true
                                    coroutineScope.launch {
                                        val success = try {
                                            authViewModel.resendVerificationEmail(userEmail)
                                        } catch (e: Exception) {
                                            false
                                        }
                                        isLoading = false
                                        if (success) {
                                            Toast.makeText(context, "Email de vérification renvoyé!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Erreur lors de l'envoi. Veuillez réessayer.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading && userEmail.isNotBlank()
                        ) {
                            Text("Renvoyer l'email de vérification", color = Color(0xFF1976D2))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmailVerificationScreenPreview() {
    WayFinderTheme {
        EmailVerificationScreen(rememberNavController())
    }
}

