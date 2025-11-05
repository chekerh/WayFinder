package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import tn.esprit.wayfinder.R
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.text.style.TextAlign

@Composable
fun AfterSplashScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Image with animation (adjust based on your image assets)
        Image(
            painter = painterResource(id = R.drawable.travel_image),
            contentDescription = "Traveler",
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bienvenue de nouveau, Wayfinders!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Connectez-vous pour gérer vos réservations et accéder à des offres exclusives sur les visites guidées et voyages.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = {
                navController.navigate("login_screen") // Navigate to the login screen
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Commencer")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AfterSplashScreenPreview() {
    AfterSplashScreen(navController = rememberNavController())
}
