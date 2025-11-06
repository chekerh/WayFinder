package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.R
import kotlin.math.absoluteValue

// Data classes for our models
data class Region(val name: String, val imageRes: Int)
data class Destination(val name: String, val description: String, val imageRes: Int, val rating: String)

// Main Composable for the Home Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Dummy data based on your drawable resources
    val regions = listOf(
        Region("Europe", R.drawable.europe),
        Region("Asie", R.drawable.asia),
        Region("Australie", R.drawable.australia)
    )
    val destinations = listOf(
        Destination("Italie", "Découvrez ce que Rome a à offrir", R.drawable.europe, "4.9"),
        Destination("France", "Explorez Paris, la ville de l'amour", R.drawable.asia, "4.8"),
        Destination("Espagne", "Découvrez Barcelone et son architecture unique", R.drawable.australia, "4.7")
    )

    Scaffold(
        containerColor = Color(0xFFF0F4F8),
        bottomBar = { CustomBottomNavigationBar() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = it.calculateBottomPadding())
        ) {
            TopBar()
            Column {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Explore le monde à ta façon",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    RegionSection(regions = regions)
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Comparateur avec Gemini", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Voir tous", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                DestinationsSection(destinations = destinations)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Salut, Javier", style = MaterialTheme.typography.titleMedium)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.5f))
                .clickable { /* Handle notification click */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.Black.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun RegionSection(regions: List<Region>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(24.dp) // Adjusted spacing
    ) {
        regions.forEach { region -> RegionChip(region = region) }
    }
}

@Composable
fun RegionChip(region: Region) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
        Image(
            painter = painterResource(id = region.imageRes),
            contentDescription = region.name,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp)) // Adjusted spacing
        Text(text = region.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DestinationsSection(destinations: List<Destination>) {
    val pagerState = rememberPagerState(initialPage = 1) { destinations.size }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 64.dp),
    ) { page ->
        Card(
            modifier = Modifier
                .graphicsLayer {
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                    scaleY = lerp(start = 0.8f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                }
                .width(280.dp)
                .height(340.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            DestinationCardContent(destination = destinations[page])
        }
    }
}

@Composable
fun DestinationCardContent(destination: Destination) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = destination.imageRes),
            contentDescription = destination.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 400f)
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Text(text = destination.name, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = destination.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = "⭐ ${destination.rating}", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun CustomBottomNavigationBar() {
    var selectedIndex by remember { mutableStateOf(0) }
    val navBarBackgroundColor = Brush.horizontalGradient(listOf(Color(0xFF63A4FF), Color(0xFF1976D2)))

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(navBarBackgroundColor).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavItem(iconRes = R.drawable.icon_home, isSelected = selectedIndex == 0, onClick = { selectedIndex = 0 })
            BottomNavItem(iconVector = Icons.Default.FavoriteBorder, isSelected = selectedIndex == 1, onClick = { selectedIndex = 1 })
            BottomNavItem(iconVector = Icons.Outlined.ChatBubbleOutline, isSelected = selectedIndex == 2, onClick = { selectedIndex = 2 })
            BottomNavItem(iconVector = Icons.Default.PersonOutline, isSelected = selectedIndex == 3, onClick = { selectedIndex = 3 })
        }
    }
}

@Composable
fun RowScope.BottomNavItem(
    iconRes: Int? = null,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.weight(1f).clip(CircleShape).clickable { onClick() }
    ) {
        val backgroundColor = if (isSelected) Color.White else Color.Black.copy(alpha = 0.15f)
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(backgroundColor))
        val iconColor = if (isSelected) Color(0xFF1976D2) else Color.White
        if (iconRes != null) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(28.dp), tint = iconColor)
        }
        if (iconVector != null) {
            Icon(imageVector = iconVector, contentDescription = null, modifier = Modifier.size(28.dp), tint = iconColor)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(navController = rememberNavController())
    }
}
