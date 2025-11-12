package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F9FF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = R.drawable.travel_image),
                contentDescription = "Travel illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Bienvenue de nouveau, Wayfindrs!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF0D47A1)
                )
                Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connectez-vous pour gérer vos réservations et accéder à des offres exclusives pour votre prochain grand voyage.",
                    fontSize = 15.sp,
                textAlign = TextAlign.Center,
                    color = Color(0xFF6B7280),
                    lineHeight = 22.sp
            )
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Commencer", fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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
