package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import tn.esprit.wayfinder.R

@Composable
fun SplashScreen(navController: NavController) {
    // Add a delay and then navigate to the next screen
    LaunchedEffect(key1 = true) {
        delay(3000) // 3-second delay
        navController.navigate("after_splash_screen") {
            // Pop up to the start destination of the graph to remove the splash screen from the back stack
            popUpTo("splash_screen") { inclusive = true }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Your splash screen UI
        Image(
            painter = painterResource(id = R.drawable.wayfinder_logo), // Replace with your logo
            contentDescription = "App Logo"
        )
    }
}
