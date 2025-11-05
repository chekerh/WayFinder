package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import tn.esprit.wayfinder.R

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.linearGradient(
                colors = listOf(Color(0xFF008FE0), Color(0xFF00184C)), // Gradient colors
                start = Offset(0f, 0f),
                end = Offset(0f, 1000f)
            ))
    ) {
        // Centered content (Logo and tagline)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo Image (Add the image to drawable folder and replace below)
            Image(
                painter = painterResource(id = R.drawable.wayfinder_logo), // Correct
                contentDescription = "Splash Logo"
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Tagline text
            Text(
                text = "Voyagez autrement, voyagez intelligemment",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }

    // Navigate to Login Screen after 3 seconds
    LaunchedEffect(Unit) {
        delay(3000) // Splash for 3 seconds
        navController.navigate("login") // Navigate to login screen
    }
}

// Preview for Splash Screen
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(navController = rememberNavController())
}
