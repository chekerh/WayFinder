package tn.esprit.wayfinder.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.painter.ColorPainter

/**
 * Pinterest-style interest selection card
 */
@Composable
fun PinterestInterestCard(
    label: String,
    icon: ImageVector? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = tween(300),
        label = "elevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Pinterest-style question card with slide animation
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PinterestQuestionCard(
    question: String,
    questionId: String = "",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val questionIcon = getIconForQuestionType(questionId, question)
    
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(400)),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        // Foreground card that contains ONLY the icon, question and answers
        val scrollState = rememberScrollState()
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 900.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        ) {
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Question icon at top
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = questionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Question text
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        lineHeight = 28.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )

                // Answers / options content
                content()
            }
        }
    }
}

/**
 * Pinterest-style progress dots indicator
 */
@Composable
fun PinterestProgressDots(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < currentStep
            val isCurrent = index == currentStep
            
            val width by animateDpAsState(
                targetValue = if (isCurrent) 32.dp else 8.dp,
                animationSpec = tween(300),
                label = "width"
            )
            
            val alpha by animateFloatAsState(
                targetValue = if (isActive || isCurrent) 1f else 0.3f,
                animationSpec = tween(300),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .alpha(alpha)
                    .background(
                        color = if (isActive || isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

/**
 * Enhanced progress bar with gradient
 */
@Composable
fun PinterestProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(3.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    ),
                    shape = RoundedCornerShape(3.dp)
                )
        )
    }
}

/**
 * Icon mapping for common interest categories - Enhanced with more options
 */
fun getIconForInterest(interest: String): ImageVector? {
    val lowerInterest = interest.lowercase()
    return when {
        // Travel types
        lowerInterest.contains("solo") || lowerInterest.contains("just me") -> Icons.Default.Person
        lowerInterest.contains("couple") || lowerInterest.contains("romantic") || lowerInterest.contains("honeymoon") -> Icons.Default.Favorite
        lowerInterest.contains("family") || lowerInterest.contains("kids") || lowerInterest.contains("children") -> Icons.Default.FamilyRestroom
        lowerInterest.contains("friends") || lowerInterest.contains("group") || lowerInterest.contains("social") -> Icons.Default.Group
        lowerInterest.contains("business") || lowerInterest.contains("work") || lowerInterest.contains("corporate") -> Icons.Default.Business
        lowerInterest.contains("adventure") || lowerInterest.contains("adventure sports") -> Icons.Default.Terrain
        
        // Budget
        lowerInterest.contains("budget") || lowerInterest.contains("affordable") || lowerInterest.contains("economy") || lowerInterest.contains("$500") -> Icons.Default.AttachMoney
        lowerInterest.contains("mid-range") || lowerInterest.contains("mid range") || lowerInterest.contains("$1,500") -> Icons.Default.AccountBalance
        lowerInterest.contains("high-end") || lowerInterest.contains("high end") || lowerInterest.contains("$3,500") -> Icons.Default.Star
        lowerInterest.contains("luxury") || lowerInterest.contains("$7,000") -> Icons.Default.Star
        
        // Activities & Interests
        lowerInterest.contains("sightseeing") || lowerInterest.contains("landmarks") -> Icons.Default.CameraAlt
        lowerInterest.contains("adventure") || lowerInterest.contains("sports") -> Icons.Default.Sports
        lowerInterest.contains("relaxation") || lowerInterest.contains("spa") -> Icons.Default.Spa
        lowerInterest.contains("nightlife") || lowerInterest.contains("entertainment") -> Icons.Default.Nightlife
        lowerInterest.contains("culture") || lowerInterest.contains("history") || lowerInterest.contains("museum") -> Icons.Default.Museum
        lowerInterest.contains("nature") || lowerInterest.contains("wildlife") -> Icons.Default.Park
        lowerInterest.contains("food") || lowerInterest.contains("dining") || lowerInterest.contains("cuisine") -> Icons.Default.Restaurant
        lowerInterest.contains("shopping") -> Icons.Default.ShoppingBag
        lowerInterest.contains("beach") || lowerInterest.contains("beaches") || lowerInterest.contains("water") -> Icons.Default.BeachAccess
        lowerInterest.contains("mountain") || lowerInterest.contains("mountains") || lowerInterest.contains("hiking") -> Icons.Default.Terrain
        
        // Destinations
        lowerInterest.contains("europe") -> Icons.Default.Public
        lowerInterest.contains("asia") -> Icons.Default.Public
        lowerInterest.contains("america") || lowerInterest.contains("americas") -> Icons.Default.Public
        lowerInterest.contains("africa") -> Icons.Default.Public
        lowerInterest.contains("oceania") || lowerInterest.contains("australia") || lowerInterest.contains("pacific") -> Icons.Default.Public
        lowerInterest.contains("middle east") -> Icons.Default.Public
        lowerInterest.contains("tropical") || lowerInterest.contains("islands") -> Icons.Default.BeachAccess
        lowerInterest.contains("urban") || lowerInterest.contains("cities") || lowerInterest.contains("major cities") -> Icons.Default.LocationCity
        lowerInterest.contains("rural") || lowerInterest.contains("countryside") || lowerInterest.contains("nature") -> Icons.Default.Park
        
        // Travel frequency
        lowerInterest.contains("rarely") || lowerInterest.contains("once a year") -> Icons.Default.Event
        lowerInterest.contains("occasionally") || lowerInterest.contains("few times") -> Icons.Default.EventNote
        lowerInterest.contains("frequently") || lowerInterest.contains("monthly") -> Icons.Default.CalendarToday
        lowerInterest.contains("very frequently") || lowerInterest.contains("7+") -> Icons.Default.CalendarMonth
        
        // General travel
        lowerInterest.contains("travel") || lowerInterest.contains("tourism") || lowerInterest.contains("vacation") -> Icons.Default.Flight
        lowerInterest.contains("city") || lowerInterest.contains("urban") || lowerInterest.contains("metropolitan") -> Icons.Default.LocationCity
        else -> Icons.Default.Explore
    }
}

/**
 * Get icon for question type (displayed at top of question card)
 */
fun getIconForQuestionType(questionId: String, questionText: String): ImageVector {
    val lowerId = questionId.lowercase()
    val lowerText = questionText.lowercase()
    
    return when {
        lowerId.contains("travel_type") || lowerText.contains("travel style") || lowerText.contains("type of trip") -> Icons.Default.Person
        lowerId.contains("budget") || lowerText.contains("budget") -> Icons.Default.AccountBalance
        lowerId.contains("interests") || lowerText.contains("activities") || lowerText.contains("interest") -> Icons.Default.Explore
        lowerId.contains("destination") || lowerText.contains("destination") || lowerText.contains("regions") -> Icons.Default.Public
        lowerId.contains("frequency") || lowerText.contains("how often") || lowerText.contains("travel") -> Icons.Default.CalendarToday
        lowerId.contains("accommodation") || lowerText.contains("accommodation") -> Icons.Default.Hotel
        lowerId.contains("climate") || lowerText.contains("climate") -> Icons.Filled.WbSunny
        lowerId.contains("duration") || lowerText.contains("how long") -> Icons.Default.Schedule
        else -> Icons.Default.QuestionMark
    }
}

/**
 * Returns a curated Unsplash image URL that best matches the question topic.
 */
fun getImageForQuestion(questionId: String, questionText: String): String? {
    val normalizedId = questionId.lowercase()
    val normalizedText = questionText.lowercase()
    
    val imageMap = mapOf(
        "travel_type" to "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=80",
        "budget" to "https://images.unsplash.com/photo-1450101215322-bf5cd27642fc?auto=format&fit=crop&w=1600&q=80",
        "interests" to "https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=1600&q=80",
        "destination_preferences" to "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1600&q=80",
        "accommodation_preference" to "https://images.unsplash.com/photo-1505691938895-1758d7feb511?auto=format&fit=crop&w=1600&q=80",
        "travel_frequency" to "https://images.unsplash.com/photo-1500534314209-a26db0f5b553?auto=format&fit=crop&w=1600&q=80",
        "climate_preference" to "https://images.unsplash.com/photo-1500534627210-44b0d3b3c0a5?auto=format&fit=crop&w=1600&q=80",
        "duration_preference" to "https://images.unsplash.com/photo-1503220317375-aaad61436b1b?auto=format&fit=crop&w=1600&q=80",
        "group_size" to "https://images.unsplash.com/photo-1529333166437-7750a6dd5a70?auto=format&fit=crop&w=1600&q=80"
    )
    
    imageMap[normalizedId]?.let { return it }

    return when {
        normalizedText.contains("budget") || normalizedText.contains("cost") -> imageMap["budget"]
        normalizedText.contains("climate") || normalizedText.contains("weather") || normalizedText.contains("season") -> imageMap["climate_preference"]
        normalizedText.contains("destination") || normalizedText.contains("region") || normalizedText.contains("where do you want") -> imageMap["destination_preferences"]
        normalizedText.contains("accommodation") || normalizedText.contains("stay") || normalizedText.contains("hotel") -> imageMap["accommodation_preference"]
        normalizedText.contains("how often") || normalizedText.contains("frequency") -> imageMap["travel_frequency"]
        normalizedText.contains("how long") || normalizedText.contains("duration") || normalizedText.contains("trip length") -> imageMap["duration_preference"]
        normalizedText.contains("activities") || normalizedText.contains("interests") -> imageMap["interests"]
        normalizedText.contains("with who") || normalizedText.contains("group") || normalizedText.contains("people") -> imageMap["group_size"]
        else -> imageMap["travel_type"]
    }
}

