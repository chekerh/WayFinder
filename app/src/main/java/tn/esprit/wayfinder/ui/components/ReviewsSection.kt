package tn.esprit.wayfinder.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.Review
import tn.esprit.wayfinder.models.ReviewStatsResponse
import tn.esprit.wayfinder.viewmodels.ReviewsUiState
import tn.esprit.wayfinder.viewmodels.ReviewsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReviewsSection(
    itemType: String,
    itemId: String,
    reviewsViewModel: ReviewsViewModel
) {
    val uiState by reviewsViewModel.uiState.collectAsState()
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        reviewsViewModel.loadReviews(itemType, itemId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avis",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showReviewDialog = true }) {
                    Text("Ajouter un avis")
                }
            }

            when (val state = uiState) {
                is ReviewsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ReviewsUiState.Success -> {
                    state.stats?.let { stats ->
                        ReviewStatsDisplay(stats = stats)
                    }

                    if (state.reviews.isEmpty()) {
                        Text(
                            text = "Aucun avis pour le moment. Soyez le premier à laisser un avis!",
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(400.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.reviews) { review ->
                                ReviewCard(
                                    review = review,
                                    isUserReview = state.userReview?.id == review.id,
                                    onEdit = { showReviewDialog = true },
                                    onDelete = {
                                        reviewsViewModel.deleteReview(review.id, itemType, itemId)
                                    }
                                )
                            }
                        }
                    }
                }
                is ReviewsUiState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                else -> {}
            }
        }
    }

    if (showReviewDialog) {
        ReviewDialog(
            itemType = itemType,
            itemId = itemId,
            existingReview = (uiState as? ReviewsUiState.Success)?.userReview,
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                val existingReview = (uiState as? ReviewsUiState.Success)?.userReview
                if (existingReview != null) {
                    reviewsViewModel.updateReview(
                        existingReview.id,
                        itemType,
                        itemId,
                        rating,
                        comment,
                        null
                    )
                } else {
                    reviewsViewModel.createReview(itemType, itemId, rating, comment, null)
                }
                showReviewDialog = false
            }
        )
    }
}

@Composable
fun ReviewStatsDisplay(stats: ReviewStatsResponse) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%.1f", stats.averageRating),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StarRating(
                    rating = stats.averageRating.toInt(),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "${stats.totalReviews} avis",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // Rating distribution
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (5 downTo 1).forEach { stars ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$stars",
                        fontSize = 12.sp,
                        modifier = Modifier.width(16.dp)
                    )
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            val count = stats.ratingDistribution[stars.toString()] ?: 0
                            if (stats.totalReviews > 0) count.toFloat() / stats.totalReviews else 0f
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = Color(0xFFFFD700),
                        trackColor = Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stats.ratingDistribution[stars.toString()] ?: 0}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.width(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewCard(
    review: Review,
    isUserReview: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val profileImageUrl = review.userId.profileImageUrl?.let { url ->
                        if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
                    } ?: "https://i.pravatar.cc/150?img=${kotlin.math.abs(review.userId.id.hashCode()) % 70}"
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.europe),
                        error = painterResource(id = R.drawable.europe)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = buildString {
                                val firstName = review.userId.firstName?.takeIf { it.isNotBlank() }
                                val lastName = review.userId.lastName?.takeIf { it.isNotBlank() }
                                when {
                                    firstName != null && lastName != null -> append("$firstName $lastName")
                                    firstName != null -> append(firstName)
                                    lastName != null -> append(lastName)
                                    else -> append(review.userId.username)
                                }
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        StarRating(
                            rating = review.rating,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                if (isUserReview) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF1976D2)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
            review.comment?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = formatTimestamp(review.createdAt),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StarRating(
    rating: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (index < rating) Color(0xFFFFD700) else Color.LightGray,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun ReviewDialog(
    itemType: String,
    itemId: String,
    existingReview: Review?,
    onDismiss: () -> Unit,
    onSubmit: (Int, String?) -> Unit
) {
    var rating by remember { mutableStateOf(existingReview?.rating ?: 0) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingReview != null) "Modifier votre avis" else "Ajouter un avis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Note")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$star stars",
                                tint = if (star <= rating) Color(0xFFFFD700) else Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Commentaire (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating, comment.takeIf { it.isNotBlank() }) },
                enabled = rating > 0
            ) {
                Text(if (existingReview != null) "Modifier" else "Publier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(timestamp)
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        timestamp
    }
}

