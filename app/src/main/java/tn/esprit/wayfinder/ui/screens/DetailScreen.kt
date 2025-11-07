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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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

@Composable
fun DetailScreen(navController: NavController) {
    // Dummy Data
    val title = "World Trade Center"
    val description =
        "Le Bahrain World Trade Center est un complexe de deux tours jumelles de 240 mètres situé à Manama, à Bahreïn, inauguré en 2008. Inspiré par les voiles arabes, c\'est le premier bâtiment à intégrer de grandes éoliennes."
    val features = listOf(
        "ensoleillé" to Icons.Outlined.WbSunny,
        "Resto" to Icons.Outlined.Restaurant,
        "Wi-Fi gratuit" to Icons.Outlined.Wifi,
        "Café" to Icons.Outlined.LocalCafe,
        "Affaires" to Icons.Outlined.BusinessCenter
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEAF2FF))) {
        ImageHeader(navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEAF2FF))
                .padding(top = 260.dp) // Start content below the header image
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFFEAF2FF)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Text(
                        text = "Équipements disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        features.forEach { (name, icon) ->
                            FeatureItem(name = name, icon = icon)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { /* Compare prices action */ },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A80F0))
                        ) {
                            Text(text = "Comparer les prix")
                        }
                        Button(
                            onClick = { /* Book action */ },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0A03D))
                        ) {
                            Text(text = "Réserver")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageHeader(navController: NavController) {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        Image(
            painter = painterResource(id = R.drawable.europe),
            contentDescription = "Location Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Top Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularButton(icon = Icons.Default.ArrowBack) { navController.popBackStack() }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularButton(icon = Icons.Default.Share) { /* Share action */ }
                CircularButton(icon = Icons.Default.Map) { /* Map action */ }
            }
        }
        // Side Buttons
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f)))
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f)))
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f)))
        }
    }
}

@Composable
fun CircularButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
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
    MaterialTheme {
        DetailScreen(navController = rememberNavController())
    }
}
