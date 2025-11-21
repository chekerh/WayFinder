package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.* 
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.ui.theme.WayFinderTheme

@Composable
fun DetailScreen(navController: NavController) {
    val title = "World Trade Center"
    val description =
        "Le Bahrain World Trade Center est un complexe de deux tours jumelles de 240 mètres..."
    val features = listOf(
        "ensoleillé" to Icons.Outlined.WbSunny,
        "Resto" to Icons.Outlined.Restaurant,
        "Wi-Fi gratuit" to Icons.Outlined.Wifi,
        "Café" to Icons.Outlined.LocalCafe,
        "Affaires" to Icons.Outlined.Business
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Header Image with buttons
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            // Image(painter = painterResource(id = R.drawable.your_image_here), ...)
            Box(modifier = Modifier.fillMaxSize().background(Color.Gray)) // Placeholder
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter), // FIX: Removed parentheses
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularButton(icon = Icons.AutoMirrored.Filled.ArrowBack) { navController.popBackStack() }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularButton(icon = Icons.Default.Share) { /* Share action */ }
                    CircularButton(icon = Icons.Default.FavoriteBorder) { /* Favorite action */ }
                }
            }
        }

        // Content Card
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 250.dp), 
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Équipements disponibles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    features.forEach { (name, icon) ->
                        FeatureItem(name = name, icon = icon)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { 
                            // Navigate to comparison screen
                            navController.navigate("all_flights/null")
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Text(text = "Comparer les prix")
                    }
                    Button(
                        onClick = { 
                            // Navigate to lodging choice screen
                            navController.navigate("lodging_choice/null")
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                    ) {
                        Text(text = "Réserver", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun CircularButton(icon: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
fun FeatureItem(name: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = name, tint = Color.Gray, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    WayFinderTheme {
        DetailScreen(rememberNavController())
    }
}
