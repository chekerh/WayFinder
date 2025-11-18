package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.viewmodels.FavoritesUiState
import tn.esprit.wayfinder.viewmodels.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    val context = LocalContext.current
    val favoritesViewModel: FavoritesViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by favoritesViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        favoritesViewModel.loadFavorites()
    }

    Scaffold(
        containerColor = Color(0xFFF0F8FF),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mes favoris",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is FavoritesUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is FavoritesUiState.Success -> {
                if (state.destinations.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Aucun favori",
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray
                    )
                    Text(
                        "Aucun favori pour le moment",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray
                    )
                    Text(
                        "Ajoutez des destinations à vos favoris\npour les retrouver facilement",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.destinations, key = { it.id }) { destination ->
                    FavoriteCard(
                        destination = destination,
                        onRemove = {
                            favoritesViewModel.removeFavorite("flight", destination.id)
                        },
                        onClick = {
                            navController.navigate("flight_detail/${destination.id}")
                        }
                    )
                }
            }
                }
            }
            is FavoritesUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            state.message,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { favoritesViewModel.loadFavorites() }) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun FavoriteCard(
    destination: FlightDestination,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image
            AsyncImage(
                model = destination.imageUrl ?: "",
                contentDescription = destination.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.travel_image),
                error = painterResource(id = R.drawable.travel_image)
            )
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = destination.country,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                if (destination.price != null && destination.price > 0) {
                    Text(
                        text = "${destination.price.toInt()} ${destination.currency}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }
            }
            
            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Retirer des favoris",
                    tint = Color(0xFFFF1744)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FavoritesScreenPreview() {
    WayFinderTheme {
        FavoritesScreen(rememberNavController())
    }
}

