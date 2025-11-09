package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Europe", "Asie", "Amerique", "Australie")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Salut, Javier", fontSize = 16.sp, color = Color.Gray)
                        Text("Explore le monde à ta façon", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF0F8FF))
            )
        },
        containerColor = Color(0xFFF0F8FF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(horizontal = 16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Text("Comparer avec Gemini", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clickable { navController.navigate("detail") },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box {
                    // Image(painter = painterResource(id = R.drawable.rome_colosseum), ...)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray) // Placeholder background
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text("Découvrez ce que Rome a à offrir", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    WayFinderTheme {
        HomeScreen(rememberNavController())
    }
}
