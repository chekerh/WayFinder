package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme

@Composable
fun ReservationScreen(navController: NavController) {
    var selectedTime by remember { mutableStateOf("07:00") }
    val timeOptions = listOf("07:00", "07:45", "09:00")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F8FF))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Réservation",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Calendar Icon",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Calendar Placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Octobre 2025",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Add a more detailed calendar grid placeholder if needed
                Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Calendar will be implemented here", color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        timeOptions.forEach { time ->
            TimeOption(time, selectedTime == time) {
                selectedTime = time
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.navigate("chat") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Réserver", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TimeOption(time: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFFD1E7FF) else Color.White
    val borderColor = if (isSelected) Color(0xFF2196F3) else Color.LightGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReservationScreenPreview() {
    WayFinderTheme {
        ReservationScreen(rememberNavController())
    }
}
