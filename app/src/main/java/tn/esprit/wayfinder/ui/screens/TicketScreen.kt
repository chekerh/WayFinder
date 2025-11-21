package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.ui.theme.WayFinderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(navController: NavController) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Votre Ticket", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        // Image(painter = painterResource(id = R.drawable.wayfinder_logo), contentDescription = "User avatar", modifier = Modifier.size(24.dp)) // Placeholder for user avatar
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)) // Blue color
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("000-224-XXX-BWA", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Icon(Icons.Default.Flight, contentDescription = "Flight", tint = Color.White, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TicketInfo("Passager", "Javier")
                        TicketInfo("Date", "22 Oct 24")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TicketInfo("Heure", "12:30")
                        TicketInfo("Porte", "A12")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    // Replace with your actual barcode image
                    // Image(
                    //     painter = painterResource(id = R.drawable.barcode_placeholder),
                    //     contentDescription = "Barcode",
                    //     modifier = Modifier.fillMaxWidth().height(80.dp)
                    // )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { /*TODO*/ },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Save PDF", color = Color.Black)
                }
                Button(
                    onClick = { /*TODO*/ },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                ) {
                    Text("Modifier", color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
             // Image(
             //    painter = painterResource(id = R.drawable.gemini_logo), // Replace with your logo
             //    contentDescription = "Gemini Logo",
             //    modifier = Modifier.size(80.dp)
             // )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TicketInfo(title: String, value: String) {
    Column {
        Text(title.uppercase(), color = Color.LightGray, fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun TicketScreenPreview() {
    WayFinderTheme {
        TicketScreen(rememberNavController())
    }
}
