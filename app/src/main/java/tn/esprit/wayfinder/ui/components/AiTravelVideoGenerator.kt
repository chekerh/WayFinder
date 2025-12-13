package tn.esprit.wayfinder.ui.components

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import tn.esprit.wayfinder.models.MusicTrack
import tn.esprit.wayfinder.models.TravelPlanSuggestion
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.AiTravelVideoViewModel
import tn.esprit.wayfinder.viewmodels.AiVideoUiState

/**
 * AI Travel Video Generator Component
 * Allows users to generate travel videos from text prompts, images, and music
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
    val musicTracks by viewModel.musicTracks.collectAsState()
    val travelPlans by viewModel.travelPlans.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()
    val selectedMusicTrack by viewModel.selectedMusicTrack.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    
    var promptText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var showMusicSelector by remember { mutableStateOf(false) }
    var showTravelPlans by remember { mutableStateOf(false) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadImagesFromUris(context, uris)
        }
    }

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
                            text = StringTranslator.translate(context, "Texte, photos & musique"),
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
                    
                    // AI Travel Plans Section
                    if (travelPlans.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Plans de voyage IA"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                            TextButton(onClick = { showTravelPlans = !showTravelPlans }) {
                                Text(if (showTravelPlans) "Masquer" else "Voir tout")
                            }
                        }
                        
                        AnimatedVisibility(visible = showTravelPlans) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(travelPlans) { plan ->
                                    TravelPlanCard(
                                        plan = plan,
                                        onClick = {
                                            promptText = plan.videoPrompt
                                            showTravelPlans = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        if (!showTravelPlans) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(travelPlans.take(3)) { plan ->
                                    SuggestionChip(
                                        onClick = { promptText = plan.videoPrompt },
                                        label = {
                                            Text(
                                                text = plan.title.take(25) + if (plan.title.length > 25) "..." else "",
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0xFFE8F5E9)
                                        )
                                    )
                                }
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
                                StringTranslator.translate(context, "Décrivez votre vidéo de voyage..."),
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
                            onDone = { focusManager.clearFocus() }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        enabled = isServiceAvailable && uiState !is AiVideoUiState.Loading && uiState !is AiVideoUiState.Generating,
                        maxLines = 3
                    )
                    
                    // Image Upload Section
                    ImageUploadSection(
                        selectedImages = selectedImages,
                        onAddImage = { imageUrl -> viewModel.addImage(imageUrl) },
                        onRemoveImage = { imageUrl -> viewModel.removeImage(imageUrl) },
                        onPickFromGallery = { imagePickerLauncher.launch("image/*") },
                        isEnabled = isServiceAvailable && uiState !is AiVideoUiState.Loading && uiState !is AiVideoUiState.Generating,
                        isUploading = isUploadingImage,
                        uploadError = uploadError,
                        onClearError = { viewModel.clearUploadError() }
                    )
                    
                    // Music Selection Section
                    MusicSelectionSection(
                        musicTracks = musicTracks,
                        selectedTrack = selectedMusicTrack,
                        onSelectTrack = { track -> viewModel.selectMusicTrack(track) },
                        showSelector = showMusicSelector,
                        onToggleSelector = { showMusicSelector = !showMusicSelector }
                    )
                    
                    // Suggestions (when no prompt)
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
                                                viewModel.clearImages()
                                                viewModel.selectMusicTrack(null)
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
                                if (selectedImages.isNotEmpty() || selectedMusicTrack != null) {
                                    viewModel.generateVideoWithMedia(promptText)
                                } else {
                                    viewModel.generateVideo(promptText)
                                }
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

/**
 * Image Upload Section Component
 */
@Composable
private fun ImageUploadSection(
    selectedImages: List<String>,
    onAddImage: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    isEnabled: Boolean,
    isUploading: Boolean,
    uploadError: String?,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
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
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = StringTranslator.translate(context, "Photos") + " (${selectedImages.size}/20)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
            }
            
            // Upload from gallery button
            Button(
                onClick = onPickFromGallery,
                enabled = isEnabled && selectedImages.size < 20 && !isUploading,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer
                )
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = StringTranslator.translate(context, "Envoi..."),
                        fontSize = 12.sp
                    )
                } else {
                    Icon(
                        Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = StringTranslator.translate(context, "Galerie"),
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Upload error
        if (uploadError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uploadError,
                        fontSize = 11.sp,
                        color = colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearError,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        
        // Selected Images Row
        if (selectedImages.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedImages) { imageUrl ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Remove button
                        IconButton(
                            onClick = { onRemoveImage(imageUrl) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Optional: URL input as alternative
        var showUrlInput by remember { mutableStateOf(false) }
        
        TextButton(
            onClick = { showUrlInput = !showUrlInput },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (showUrlInput) 
                    StringTranslator.translate(context, "Masquer URL") 
                else 
                    StringTranslator.translate(context, "Ou ajouter via URL"),
                fontSize = 11.sp,
                color = colorScheme.primary
            )
        }
        
        AnimatedVisibility(visible = showUrlInput) {
            var imageUrlInput by remember { mutableStateOf("") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = imageUrlInput,
                    onValueChange = { imageUrlInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "https://...",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    enabled = isEnabled && selectedImages.size < 20,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
                IconButton(
                    onClick = {
                        if (imageUrlInput.startsWith("http")) {
                            onAddImage(imageUrlInput)
                            imageUrlInput = ""
                        }
                    },
                    enabled = imageUrlInput.startsWith("http") && selectedImages.size < 20
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add image",
                        tint = if (imageUrlInput.startsWith("http")) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Music Selection Section Component
 */
@Composable
private fun MusicSelectionSection(
    musicTracks: List<MusicTrack>,
    selectedTrack: MusicTrack?,
    onSelectTrack: (MusicTrack?) -> Unit,
    showSelector: Boolean,
    onToggleSelector: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleSelector() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = StringTranslator.translate(context, "Musique de fond"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
            }
            
            if (selectedTrack != null) {
                AssistChip(
                    onClick = { onSelectTrack(null) },
                    label = {
                        Text(selectedTrack.name, fontSize = 11.sp)
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            } else {
                Icon(
                    if (showSelector) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
        
        AnimatedVisibility(visible = showSelector && musicTracks.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(musicTracks) { track ->
                    MusicTrackChip(
                        track = track,
                        isSelected = selectedTrack?.id == track.id,
                        onClick = { onSelectTrack(if (selectedTrack?.id == track.id) null else track) }
                    )
                }
            }
        }
    }
}

/**
 * Music Track Chip Component
 */
@Composable
private fun MusicTrackChip(
    track: MusicTrack,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceVariant
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                )
                Text(
                    text = track.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = track.genre,
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = track.duration,
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Travel Plan Card Component
 */
@Composable
private fun TravelPlanCard(
    plan: TravelPlanSuggestion,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = plan.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = plan.description,
                fontSize = 11.sp,
                color = colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colorScheme.primary
                )
                Text(
                    text = plan.duration,
                    fontSize = 10.sp,
                    color = colorScheme.primary
                )
            }
            // Destinations
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(plan.destinations.take(3)) { dest ->
                    Text(
                        text = dest,
                        fontSize = 9.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                colorScheme.surface,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Utiliser ce plan",
                    fontSize = 11.sp
                )
            }
        }
    }
}
