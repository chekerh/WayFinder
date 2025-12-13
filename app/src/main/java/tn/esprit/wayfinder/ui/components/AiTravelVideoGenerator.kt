package tn.esprit.wayfinder.ui.components

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.AiTravelVideoViewModel
import tn.esprit.wayfinder.viewmodels.AiVideoUiState

/**
 * AI Travel Video Generator Component
 * Allows users to generate travel videos from text prompts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTravelVideoGenerator(
    modifier: Modifier = Modifier,
    onVideoGenerated: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme
    
    val viewModel: AiTravelVideoViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isServiceAvailable by viewModel.isServiceAvailable.collectAsState()
    
    var promptText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with gradient accent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // AI Icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF667EEA),
                                        Color(0xFF764BA2)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = StringTranslator.translate(context, "Créer une vidéo IA"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = StringTranslator.translate(context, "Décrivez votre voyage de rêve"),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Expand/Collapse button
                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = colorScheme.primary
                    )
                }
            }
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Service unavailable warning
                    if (!isServiceAvailable) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.errorContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = StringTranslator.translate(context, "Service temporairement indisponible"),
                                    fontSize = 13.sp,
                                    color = colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    // Prompt input field
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                StringTranslator.translate(context, "Ex: Coucher de soleil sur une plage tropicale..."),
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                tint = colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (promptText.isNotEmpty()) {
                                IconButton(onClick = { promptText = "" }) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        contentDescription = "Clear",
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (promptText.isNotBlank() && isServiceAvailable) {
                                    viewModel.generateVideo(promptText)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        enabled = isServiceAvailable && uiState !is AiVideoUiState.Loading && uiState !is AiVideoUiState.Generating,
                        maxLines = 3
                    )
                    
                    // Suggestions
                    if (suggestions.isNotEmpty() && promptText.isEmpty()) {
                        Text(
                            text = StringTranslator.translate(context, "Suggestions:"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurfaceVariant
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestions.take(5)) { suggestion ->
                                SuggestionChip(
                                    onClick = { promptText = suggestion },
                                    label = {
                                        Text(
                                            text = suggestion.take(40) + if (suggestion.length > 40) "..." else "",
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                    
                    // UI State display
                    when (val state = uiState) {
                        is AiVideoUiState.Loading -> {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = colorScheme.primary
                            )
                            Text(
                                text = StringTranslator.translate(context, "Démarrage de la génération..."),
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        is AiVideoUiState.Generating -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colorScheme.primary
                                )
                                Text(
                                    text = "${StringTranslator.translate(context, "Génération en cours")} ${state.progress}%",
                                    fontSize = 13.sp,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                if (state.enhancedPrompt.isNotEmpty()) {
                                    Text(
                                        text = "\"${state.enhancedPrompt.take(80)}...\"",
                                        fontSize = 11.sp,
                                        color = colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                TextButton(
                                    onClick = { viewModel.cancelGeneration() }
                                ) {
                                    Text(StringTranslator.translate(context, "Annuler"))
                                }
                            }
                        }
                        
                        is AiVideoUiState.Completed -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = StringTranslator.translate(context, "Vidéo générée avec succès!"),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                // Open video in browser or player
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.videoUrl))
                                                context.startActivity(intent)
                                                onVideoGenerated?.invoke(state.videoUrl)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(StringTranslator.translate(context, "Voir"))
                                        }
                                        
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.resetState()
                                                promptText = ""
                                            }
                                        ) {
                                            Text(StringTranslator.translate(context, "Nouveau"))
                                        }
                                    }
                                }
                            }
                        }
                        
                        is AiVideoUiState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorScheme.errorContainer.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        fontSize = 13.sp,
                                        color = colorScheme.onErrorContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    TextButton(
                                        onClick = { viewModel.resetState() }
                                    ) {
                                        Text(StringTranslator.translate(context, "Réessayer"))
                                    }
                                }
                            }
                        }
                        
                        else -> { /* Idle or ServiceUnavailable */ }
                    }
                    
                    // Generate button
                    if (uiState is AiVideoUiState.Idle || uiState is AiVideoUiState.Error) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.generateVideo(promptText)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = promptText.isNotBlank() && isServiceAvailable,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                disabledContainerColor = colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = StringTranslator.translate(context, "Générer la vidéo"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Collapsed state preview
            if (!isExpanded) {
                Text(
                    text = StringTranslator.translate(context, "Appuyez pour créer des vidéos de voyage avec l'IA"),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { isExpanded = true }
                )
            }
        }
    }
}

