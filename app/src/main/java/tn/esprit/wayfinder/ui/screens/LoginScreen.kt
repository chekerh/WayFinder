package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.R

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAF2FF))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))
        // Title
        Row {
            Text(
                text = "Bienvenue sur ",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Way",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFD32F2F) // Red
            )
            Text(
                text = "findr",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1976D2) // Blue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.wayfinder_logo), // Assuming this is the yin-yang logo
            contentDescription = "Wayfinder Logo",
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Subtitle
            Text(
                text = "Veuillez vous connecter pour commencer",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.DarkGray
            )

            // Powered by Gemini
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)) { // Red
                        append("Powered by ")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFFFBC02D), fontWeight = FontWeight.Bold)) { // Yellow
                        append("Gemini")
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Text Fields
        Column {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Checkbox and terms
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6200EE))
            )
            val annotatedString = buildAnnotatedString {
                append("J'''accepte la ")
                withStyle(style = SpanStyle(color = Color(0xFFF57C00))) { // Orange
                    pushStringAnnotation(tag = "URL", annotation = "policy")
                    append("Politique de Confidentialité")
                    pop()
                }
                append(" et les ")
                withStyle(style = SpanStyle(color = Color(0xFFF57C00))) { // Orange
                    pushStringAnnotation(tag = "URL", annotation = "terms")
                    append("Conditions d'''Utilisation")
                    pop()
                }
            }
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall,
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            // TODO: Handle click on annotation.item (e.g., open a browser)
                        }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login Button
        Button(
            onClick = { navController.navigate("home_screen") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = email.isNotEmpty() && password.isNotEmpty() && isChecked,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3682E3))
        ) {
            Text(text = "Se connecter", fontSize = 18.sp, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = "Connect",
                tint = Color.White
            )
        }
        Spacer(Modifier.weight(1f))
        // Other options
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
             Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pas de compte ?",
                    color = Color.Gray,
                    modifier = Modifier.clickable { /* TODO: Navigate to Sign Up screen */ }
                )
                Text(
                    text = "Mot de passe oublié ?",
                    color = Color(0xFF1976D2), // Blue
                    modifier = Modifier.clickable { /* TODO: Navigate to Forgot Password screen */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("OU", color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            // Social Logins
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { /* TODO: Handle Google login */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(30.dp),
                        tint = Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(onClick = { /* TODO: Handle Apple login */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_apple),
                        contentDescription = "Apple",
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }
        Spacer(Modifier.weight(0.5f))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun LoginScreenPreview() {
    // You might need a theme wrapper if your composable uses MaterialTheme.
    // For example: YourAppTheme { LoginScreen(navController = rememberNavController()) }
    LoginScreen(navController = rememberNavController())
}
