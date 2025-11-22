package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.models.SearchHistory
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.SearchHistoryUiState
import tn.esprit.wayfinder.viewmodels.SearchHistoryViewModel
import tn.esprit.wayfinder.utils.StringTranslator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHistoryScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val searchHistoryViewModel: SearchHistoryViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by searchHistoryViewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Recent, 1 = Saved

    LaunchedEffect(Unit) {
        searchHistoryViewModel.loadRecentSearches()
        searchHistoryViewModel.loadSavedSearches()
        searchHistoryViewModel.loadSearchStats()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringTranslator.translate(context, "Historique de recherche"),
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
                actions = {
                    if (selectedTab == 0 && (uiState as? SearchHistoryUiState.Success)?.recentSearches?.isNotEmpty() == true) {
                        IconButton(onClick = {
                            searchHistoryViewModel.clearRecentSearches {
                                searchHistoryViewModel.loadRecentSearches()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = StringTranslator.translate(context, "Effacer tout")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(StringTranslator.translate(context, "Récent")) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(StringTranslator.translate(context, "Enregistrés")) }
                )
            }

            when (val state = uiState) {
                is SearchHistoryUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SearchHistoryUiState.Success -> {
                    val searches = if (selectedTab == 0) state.recentSearches else state.savedSearches

                    if (searches.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.History else Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Text(
                                    if (selectedTab == 0) StringTranslator.translate(context, "Aucune recherche récente") else StringTranslator.translate(context, "Aucune recherche enregistrée"),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.Gray
                                )
                                Text(
                                    if (selectedTab == 0) StringTranslator.translate(context, "Vos recherches récentes apparaîtront ici") else StringTranslator.translate(context, "Enregistrez des recherches pour y accéder rapidement"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searches, key = { it.id }) { search ->
                                SearchHistoryItem(
                                    search = search,
                                    isSavedTab = selectedTab == 1,
                                    onSave = { savedName ->
                                        searchHistoryViewModel.saveSearch(search.id, savedName) {
                                            searchHistoryViewModel.loadRecentSearches()
                                            searchHistoryViewModel.loadSavedSearches()
                                        }
                                    },
                                    onUnsave = {
                                        searchHistoryViewModel.unsaveSearch(search.id) {
                                            searchHistoryViewModel.loadRecentSearches()
                                            searchHistoryViewModel.loadSavedSearches()
                                        }
                                    },
                                    onDelete = {
                                        searchHistoryViewModel.deleteSearchHistory(search.id) {
                                            searchHistoryViewModel.loadRecentSearches()
                                            searchHistoryViewModel.loadSavedSearches()
                                        }
                                    },
                                    onClick = {
                                        // Navigate to search results or perform search
                                        // This would depend on the search type and params
                                    }
                                )
                            }
                        }
                    }
                }
                is SearchHistoryUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
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
                            Button(onClick = {
                                if (selectedTab == 0) {
                                    searchHistoryViewModel.loadRecentSearches()
                                } else {
                                    searchHistoryViewModel.loadSavedSearches()
                                }
                            }) {
                                Text("Réessayer")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SearchHistoryItem(
    search: SearchHistory,
    isSavedTab: Boolean,
    onSave: (String) -> Unit,
    onUnsave: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var savedName by remember { mutableStateOf(search.savedName ?: "") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getSearchTypeIcon(search.searchType),
                    contentDescription = search.searchType,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = search.searchQuery ?: formatSearchParams(search.searchParams),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (search.savedName != null) {
                        Text(
                            text = search.savedName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = formatTimestamp(search.lastSearchedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (search.searchCount > 1) {
                        Text(
                            text = "Recherché ${search.searchCount} fois",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            Row {
                if (!isSavedTab && !search.isSaved) {
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkBorder,
                            contentDescription = "Enregistrer",
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
                if (isSavedTab || search.isSaved) {
                    IconButton(onClick = onUnsave) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = "Désenregistrer",
                            tint = Color(0xFFFF9800)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Supprimer",
                        tint = Color.Red
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Enregistrer la recherche") },
            text = {
                OutlinedTextField(
                    value = savedName,
                    onValueChange = { savedName = it },
                    label = { Text("Nom de la recherche") },
                    placeholder = { Text("Ex: Voyage à Paris") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (savedName.isNotBlank()) {
                            onSave(savedName)
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun getSearchTypeIcon(searchType: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (searchType.lowercase()) {
        "flight" -> Icons.Filled.Flight
        "hotel" -> Icons.Filled.Hotel
        "destination" -> Icons.Filled.Place
        "activity" -> Icons.Filled.LocalActivity
        else -> Icons.Filled.Search
    }
}

fun formatSearchParams(params: Map<String, kotlinx.serialization.json.JsonElement>): String {
    val parts = mutableListOf<String>()
    params["destination"]?.let { parts.add("Destination: ${it.toString().trim('"')}") }
    params["origin"]?.let { parts.add("Départ: ${it.toString().trim('"')}") }
    params["dateFrom"]?.let { parts.add("Du: ${it.toString().trim('"')}") }
    params["dateTo"]?.let { parts.add("Au: ${it.toString().trim('"')}") }
    params["city"]?.let { parts.add("Ville: ${it.toString().trim('"')}") }
    params["query"]?.let { parts.add(it.toString().trim('"')) }
    return parts.take(3).joinToString(" • ")
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(timestamp)
        val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        timestamp
    }
}

@Preview(showBackground = true)
@Composable
fun SearchHistoryScreenPreview() {
    WayFinderTheme {
        SearchHistoryScreen(rememberNavController())
    }
}

