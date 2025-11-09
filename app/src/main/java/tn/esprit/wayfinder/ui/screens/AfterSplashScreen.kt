package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.ui.theme.WayFinderTheme

@Composable
fun AfterSplashScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp), // Increased padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly // FIX: Changed to SpaceEvenly
    ) {
        // You need to add an image named 'after_splash_illustration' to your res/drawable folder
        // Image(
        //     painter = painterResource(id = R.drawable.after_splash_illustration),
        //     contentDescription = "Travel Illustration",
        //     modifier = Modifier.fillMaxWidth(0.9f)
        // )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Bienvenue de nouveau, Wayfinders!", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connectez-vous pour gérer vos réservations et accéder à des offres exclusives pour votre prochain grand voyage.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { navController.navigate("login_screen") },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)), // Yellow color
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Commencer", fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AfterSplashScreenPreview() {
    WayFinderTheme {
        AfterSplashScreen(rememberNavController())
    }
}
