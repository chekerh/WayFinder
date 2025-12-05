package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.models.GroupFlight
import tn.esprit.wayfinder.models.GroupFlightMember
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.GroupFlightViewModel
import tn.esprit.wayfinder.viewmodels.GroupFlightDetailUiState
import tn.esprit.wayfinder.manager.TokenManager
import androidx.compose.ui.res.painterResource
import tn.esprit.wayfinder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupFlightDetailScreen(
    navController: NavController,
    groupId: String
) {
    val context = LocalContext.current
    val groupFlightViewModel: GroupFlightViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val detailState by groupFlightViewModel.detailUiState.collectAsState()
    val tokenManager = remember { TokenManager(context) }
    val currentUser = remember { tokenManager.getUser() }
    
    LaunchedEffect(groupId) {
        groupFlightViewModel.loadGroupFlightDetail(groupId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Détails du vol de groupe"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = detailState) {
            is GroupFlightDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is GroupFlightDetailUiState.Success -> {
                GroupFlightDetailContent(
                    groupFlight = state.groupFlight,
                    currentUserId = currentUser?.id,
                    onJoin = {
                        groupFlightViewModel.joinGroupFlight(state.groupFlight.id)
                    },
                    onInvite = { userIds ->
                        groupFlightViewModel.inviteUsers(state.groupFlight.id, userIds)
                    },
                    onBook = {
                        // Navigate to booking with group flight info
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("group_flight_id", state.groupFlight.id)
                        navController.navigate("review_booking/${state.groupFlight.destinationId}")
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is GroupFlightDetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { groupFlightViewModel.loadGroupFlightDetail(groupId) }) {
                        Text(StringTranslator.translate(context, "Réessayer"))
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun GroupFlightDetailContent(
    groupFlight: GroupFlight,
    currentUserId: String?,
    onJoin: () -> Unit,
    onInvite: (List<String>) -> Unit,
    onBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isOrganizer = currentUserId == groupFlight.organizerId
    val isMember = groupFlight.members.any { it.userId == currentUserId && it.status == tn.esprit.wayfinder.models.MemberStatus.ACCEPTED }
    val canJoin = !isMember && !isOrganizer && groupFlight.members.size < groupFlight.maxMembers
    
    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Destination Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = groupFlight.destination?.name ?: "Destination",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "${groupFlight.destination?.city ?: ""}, ${groupFlight.destination?.country ?: ""}",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Members Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Membres (${groupFlight.members.size}/${groupFlight.maxMembers})"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (isOrganizer) {
                            TextButton(onClick = { /* Show invite dialog */ }) {
                                Text(StringTranslator.translate(context, "Inviter"))
                            }
                        }
                    }
                    
                    items(groupFlight.members) { member ->
                        MemberItem(member = member, isOrganizer = member.userId == groupFlight.organizerId)
                    }
                }
            }
        }
        
        // Shared Hotel Info
        groupFlight.sharedHotel?.let { hotel ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Hotel,
                                contentDescription = null,
                                tint = colorScheme.primary
                            )
                            Text(
                                text = StringTranslator.translate(context, "Hôtel partagé"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            text = hotel.name,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${StringTranslator.translate(context, "Chambre")}: ${hotel.roomType}",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Coût par personne"),
                                fontSize = 14.sp
                            )
                            Text(
                                text = String.format("%.2f %s", hotel.costPerPerson, groupFlight.destination?.currency ?: "EUR"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Cost Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Coût total"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Par personne"),
                            fontSize = 16.sp
                        )
                        Text(
                            text = String.format("%.2f %s", groupFlight.costPerPerson, groupFlight.destination?.currency ?: "EUR"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colorScheme.primary
                        )
                    }
                    if (groupFlight.savings > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = StringTranslator.translate(context, "Économies totales"),
                                    fontSize = 14.sp,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = String.format("%.2f %s", groupFlight.savings, groupFlight.destination?.currency ?: "EUR"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Action Buttons
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (canJoin) {
                    Button(
                        onClick = onJoin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Rejoindre le groupe"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (isMember || isOrganizer) {
                    Button(
                        onClick = onBook,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107)
                        )
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Réserver maintenant"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberItem(
    member: GroupFlightMember,
    isOrganizer: Boolean
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = member.user?.profileImageUrl ?: "",
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.europe),
            error = painterResource(id = R.drawable.europe)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() } ?: "User",
                fontWeight = FontWeight.Medium
            )
            if (isOrganizer) {
                Text(
                    text = StringTranslator.translate(context, "Organisateur"),
                    fontSize = 12.sp,
                    color = colorScheme.primary
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when (member.status) {
                tn.esprit.wayfinder.models.MemberStatus.ACCEPTED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                tn.esprit.wayfinder.models.MemberStatus.INVITED -> Color(0xFFFFC107).copy(alpha = 0.2f)
                tn.esprit.wayfinder.models.MemberStatus.DECLINED -> Color(0xFFF44336).copy(alpha = 0.2f)
            }
        ) {
            Text(
                text = when (member.status) {
                    tn.esprit.wayfinder.models.MemberStatus.ACCEPTED -> StringTranslator.translate(context, "Accepté")
                    tn.esprit.wayfinder.models.MemberStatus.INVITED -> StringTranslator.translate(context, "Invité")
                    tn.esprit.wayfinder.models.MemberStatus.DECLINED -> StringTranslator.translate(context, "Refusé")
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

