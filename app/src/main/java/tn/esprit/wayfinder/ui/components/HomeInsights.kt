package tn.esprit.wayfinder.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.utils.StringTranslator

data class Insight(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val highlightSection: String? = null // Section to highlight (e.g., "recommended_flights", "chat", "preferences")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeInsightsOverlay(
    navController: androidx.navigation.NavController,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val tokenManager = remember { TokenManager(context) }
    val currentUser = remember { tokenManager.getUser() }
    val hasCompletedOnboarding = currentUser?.onboardingCompleted == true && currentUser.onboardingSkipped != true
    
    // Generate insights based on user state
    val insights = remember(hasCompletedOnboarding) {
        buildList {
            // Always show welcome insight first
            add(Insight(
                id = "welcome",
                title = StringTranslator.translate(context, "Bienvenue sur WayFinder! 👋"),
                description = StringTranslator.translate(context, "Découvrez des destinations personnalisées et planifiez vos voyages en toute simplicité."),
                icon = Icons.Filled.Home
            ))
            
            // If user hasn't completed onboarding, show AI form insight
            if (!hasCompletedOnboarding) {
                add(Insight(
                    id = "ai_form",
                    title = StringTranslator.translate(context, "Formulaire IA personnalisé 🤖"),
                    description = StringTranslator.translate(context, "Remplissez notre formulaire alimenté par l'IA pour personnaliser votre expérience et obtenir des recommandations adaptées à vos préférences."),
                    icon = Icons.Filled.AutoAwesome,
                    highlightSection = "preferences"
                ))
            }
            
            // Show recommended flights insight
            add(Insight(
                id = "recommended_flights",
                title = StringTranslator.translate(context, "Vols recommandés ✈️"),
                description = StringTranslator.translate(context, "Parcourez nos suggestions de destinations basées sur vos préférences. Appuyez sur une carte pour voir les détails."),
                icon = Icons.Filled.Flight,
                highlightSection = "recommended_flights"
            ))
            
            // Show chat insight
            add(Insight(
                id = "chat",
                title = StringTranslator.translate(context, "Assistant IA 💬"),
                description = StringTranslator.translate(context, "Discutez avec notre assistant IA pour obtenir des suggestions de voyages personnalisées et des réponses à vos questions."),
                icon = Icons.Filled.Chat,
                highlightSection = "chat"
            ))
            
            // Show regions insight
            add(Insight(
                id = "regions",
                title = StringTranslator.translate(context, "Explorer par région 🌍"),
                description = StringTranslator.translate(context, "Filtrez les destinations par région pour découvrir des offres dans différentes parties du monde."),
                icon = Icons.Filled.Public,
                highlightSection = "regions"
            ))
            
            // If user completed onboarding, show preferences insight
            if (hasCompletedOnboarding) {
                add(Insight(
                    id = "preferences",
                    title = StringTranslator.translate(context, "Personnaliser vos préférences ⚙️"),
                    description = StringTranslator.translate(context, "Ajustez vos préférences de voyage pour affiner vos recommandations et personnaliser votre expérience."),
                    icon = Icons.Filled.Tune,
                    highlightSection = "preferences"
                ))
            }
        }
    }
    
    var currentInsightIndex by remember { mutableStateOf(0) }
    val currentInsight = insights.getOrNull(currentInsightIndex)
    
    if (currentInsight != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .zIndex(1000f)
                .clickable { /* Prevent clicks from passing through */ }
        ) {
            AnimatedVisibility(
                visible = currentInsight != null,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut() + slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200)
                )
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = StringTranslator.translate(context, "Fermer"),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentInsight.icon,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Title
                        Text(
                            text = currentInsight.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Description
                        Text(
                            text = currentInsight.description,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Skip all button
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(StringTranslator.translate(context, "Passer"))
                            }
                            
                            // Next/Complete button
                            Button(
                                onClick = {
                                    if (currentInsightIndex < insights.size - 1) {
                                        currentInsightIndex++
                                    } else {
                                        onComplete()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (currentInsightIndex < insights.size - 1) {
                                        StringTranslator.translate(context, "Suivant")
                                    } else {
                                        StringTranslator.translate(context, "Commencer")
                                    }
                                )
                            }
                        }
                        
                        // Progress dots
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(insights.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(if (index == currentInsightIndex) 10.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == currentInsightIndex) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

