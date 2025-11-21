package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme

// FIX: Moved data class back inside the screen file as it is UI-specific.
private data class ResultOption(val price: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedResultsScreen(navController: NavController) {

    val options = listOf(
        ResultOption("100 TND", "Vol + A/R + Bagage à main (10kg)"),
        ResultOption("250 TND", "Vol + Hôtel + ..."),
        ResultOption("50 TND", "Transport de l'aéroport")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Résultats pour toi", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = { /*TODO*/ }) { Icon(Icons.Default.VpnKey, contentDescription = "Key") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Voyage à Paris - Vols + Hôtel", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    options.forEach { option ->
                        OptionItem(option)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // FIX: Changed to HorizontalDivider
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { /* TODO: Should maybe navigate to reservation details */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Voir plus d'options")
            }
        }
    }
}

@Composable
private fun OptionItem(option: ResultOption) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(option.description, fontSize = 14.sp, color = Color.Gray)
        Text(option.price, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
    }
}


@Preview(showBackground = true)
@Composable
fun PersonalizedResultsScreenPreview() {
    WayFinderTheme {
        PersonalizedResultsScreen(rememberNavController())
    }
}
