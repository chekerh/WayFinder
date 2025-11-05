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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.linearGradient(
                colors = listOf(Color(0xFF008FE0), Color(0xFF00184C)), // Gradient colors
            ))
    ) {
        // Centered content (Logo and tagline)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row for logo and "Wayfindr" text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Wayfindr" text with red and yellow colors
                Text(
                    text = "Way",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = Color(0xFFD20000) // Red color for "Way"
                    ),
                    modifier = Modifier.padding(end = 4.dp), // Small padding between "Way" and logo
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "findr",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = Color(0xFFFCBF40) // Yellow color for "findr"
                    ),
                    modifier = Modifier.padding(end = 8.dp), // Space between the text and the logo
                    textAlign = TextAlign.Center
                )

                // Logo Image
                Image(
                    painter = painterResource(id = R.drawable.wayfinder_logo), // Replace with actual logo resource
                    contentDescription = "Wayfindr Logo",
                    modifier = Modifier.size(50.dp) // Adjusted logo size
                )
            }

            // Adjusted Spacer to move the tagline down
            Spacer(modifier = Modifier.height(32.dp)) // Increased height here

            // Tagline text
            Text(
                text = "Voyagez autrement, voyagez intelligemment", // Exact tagline
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White, // White color for tagline
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    // Navigate to the next screen after a delay
    LaunchedEffect(key1 = true) {
        delay(3000) // 3-second delay
        navController.navigate("after_splash_screen") {
            // Pop up to the start destination of the graph to remove the splash screen from the back stack
            popUpTo("splash_screen") { inclusive = true }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(navController = rememberNavController())
}
