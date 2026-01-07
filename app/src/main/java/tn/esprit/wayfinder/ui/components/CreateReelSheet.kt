package tn.esprit.wayfinder.ui.components

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.media.MediaPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tn.esprit.wayfinder.models.MusicTrack
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.AiTravelVideoViewModel
import tn.esprit.wayfinder.viewmodels.JourneyViewModel
import tn.esprit.wayfinder.viewmodels.JourneyUploadUiState

@Composable
fun CreateReelSheet(
    onReelCreated: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    val journeyViewModel: JourneyViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val aiVideoViewModel: AiTravelVideoViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    
    val uploadState by journeyViewModel.uploadUiState.collectAsState()
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var selectedMusicTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var selectedStartTime by remember { mutableStateOf(0f) }
    var selectedEndTime by remember { mutableStateOf(30f) }
    val trackTimeRanges = remember { mutableMapOf<String, Pair<Float, Float>>() }
    var musicTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var isLoadingTracks by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Observe upload state
    LaunchedEffect(uploadState) {
        val state = uploadState // Assign to local variable for smart cast
        when (state) {
            is JourneyUploadUiState.Success -> {
                isCreating = false
                onReelCreated()
            }
            is JourneyUploadUiState.Error -> {
                isCreating = false
                errorMessage = state.message
            }
            is JourneyUploadUiState.Uploading, 
            is JourneyUploadUiState.Compressing -> {
                isCreating = true
            }
            else -> {}
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let { loadImageFromUri(context, it) { bitmap ->
            selectedImageBitmap = bitmap
        }}
    }
    
    // Load music tracks
    val musicTracksState by aiVideoViewModel.musicTracks.collectAsState()
    
    LaunchedEffect(Unit) {
        // Load music tracks explicitly
        aiVideoViewModel.loadMusicTracks()
    }
    
    LaunchedEffect(musicTracksState) {
        musicTracks = musicTracksState
        isLoadingTracks = false
        android.util.Log.d("CreateReelSheet", "Loaded ${musicTracksState.size} music tracks")
        musicTracksState.forEach { track ->
            android.util.Log.d("CreateReelSheet", "Track: ${track.name}, URL: ${track.previewUrl}")
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Create Reel"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = colorScheme.onSurface
                        )
                    }
                }
                
                // Image Selection Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Select Image"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    
                    if (selectedImageBitmap != null) {
                        // Preview selected image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            Image(
                                bitmap = selectedImageBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            
                            IconButton(
                                onClick = {
                                    selectedImageBitmap = null
                                    selectedImageUri = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                        .padding(8.dp)
                                )
                            }
                        }
                    } else {
                        // Image picker button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = StringTranslator.translate(context, "Tap to select image"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                // Music Selection Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Select Music (Optional)"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    
                    if (isLoadingTracks) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (musicTracks.isEmpty()) {
                        Text(
                            text = StringTranslator.translate(context, "No music tracks available"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(musicTracks) { track ->
                                // Get initial time values from map or defaults
                                val initialStartTime = trackTimeRanges[track.id]?.first ?: 0f
                                val initialEndTime = trackTimeRanges[track.id]?.second ?: 30f
                                
                                val cardStartTime = remember(track.id) { mutableStateOf<Float>(initialStartTime) }
                                val cardEndTime = remember(track.id) { mutableStateOf<Float>(initialEndTime) }
                                
                                // Sync with parent when selected
                                LaunchedEffect(selectedMusicTrack?.id) {
                                    if (selectedMusicTrack?.id == track.id) {
                                        cardStartTime.value = selectedStartTime
                                        cardEndTime.value = selectedEndTime
                                    }
                                }
                                
                                MusicTrackCard(
                                    track = track,
                                    isSelected = selectedMusicTrack?.id == track.id,
                                    onSelect = {
                                        if (selectedMusicTrack?.id == track.id) {
                                            selectedMusicTrack = null
                                        } else {
                                            selectedMusicTrack = track
                                            selectedStartTime = cardStartTime.value
                                            selectedEndTime = cardEndTime.value
                                        }
                                    },
                                    startTime = cardStartTime,
                                    endTime = cardEndTime,
                                    onTimeRangeChanged = { newStart, newEnd ->
                                        if (selectedMusicTrack?.id == track.id) {
                                            selectedStartTime = newStart
                                            selectedEndTime = newEnd
                                        }
                                        trackTimeRanges[track.id] = Pair(newStart, newEnd)
                                    }
                                )
                            }
                        }
                    }
                    
                    // Selected music display
                    selectedMusicTrack?.let { track ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${StringTranslator.translate(context, "Selected")}: ${track.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurfaceVariant
                                )
                                
                                TextButton(onClick = { selectedMusicTrack = null }) {
                                    Text(
                                        text = StringTranslator.translate(context, "Clear"),
                                        color = colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Create Button
                Button(
                    onClick = {
                        createReel(
                            coroutineScope = coroutineScope,
                            context = context,
                            imageUri = selectedImageUri,
                            musicTrack = selectedMusicTrack,
                            startTime = selectedStartTime,
                            endTime = selectedEndTime,
                            journeyViewModel = journeyViewModel,
                            onSuccess = onReelCreated,
                            onError = { message ->
                                errorMessage = message
                            },
                            onLoading = { loading ->
                                isCreating = loading
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = selectedImageBitmap != null && !isCreating,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        disabledContainerColor = colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Text(
                                text = StringTranslator.translate(context, "Create Reel"),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // Error message
                errorMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicTrackCard(
    track: MusicTrack,
    isSelected: Boolean,
    onSelect: () -> Unit,
    startTime: MutableState<Float>,
    endTime: MutableState<Float>,
    onTimeRangeChanged: (Float, Float) -> Unit = { _, _ -> }
) {
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember(track.id) { mutableStateOf<MediaPlayer?>(null) }
    
    // Initialize MediaPlayer when expanded
    LaunchedEffect(isExpanded, track.id) {
        if (isExpanded && track.previewUrl != null && track.previewUrl.isNotEmpty()) {
            isLoading = true
            errorMessage = null
            
            try {
                // Release any existing player first
                mediaPlayer?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            mp.stop()
                        }
                        mp.release()
                    } catch (e: Exception) {
                        android.util.Log.e("MusicTrackCard", "Error releasing player", e)
                    }
                    mediaPlayer = null
                }
                
                android.util.Log.d("MusicTrackCard", "Loading preview URL: ${track.previewUrl}")
                
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                }
                
                // Validate URL
                val urlString = track.previewUrl
                if (urlString.isNullOrBlank()) {
                    throw IllegalArgumentException("Preview URL is empty")
                }
                
                android.util.Log.d("MusicTrackCard", "Setting data source: $urlString")
                player.setDataSource(urlString)
                player.prepareAsync()
                android.util.Log.d("MusicTrackCard", "prepareAsync() called")
                
                var prepared = false
                player.setOnPreparedListener { mp ->
                    android.util.Log.d("MusicTrackCard", "MediaPlayer prepared successfully")
                    if (!prepared) {
                        prepared = true
                        try {
                            val durationMs = mp.duration
                            if (durationMs > 0) {
                                duration = (durationMs / 1000f)
                                if (endTime.value > duration) {
                                    endTime.value = duration
                                }
                                mediaPlayer = mp
                                isLoading = false
                                android.util.Log.d("MusicTrackCard", "Duration set: $duration seconds")
                            } else {
                                android.util.Log.w("MusicTrackCard", "MediaPlayer duration is 0")
                                errorMessage = "Invalid audio file"
                                isLoading = false
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MusicTrackCard", "Error in onPrepared", e)
                            errorMessage = "Failed to load audio: ${e.message}"
                            isLoading = false
                        }
                    }
                }
                
                player.setOnErrorListener { mp, what, extra ->
                    android.util.Log.e("MusicTrackCard", "MediaPlayer error: what=$what, extra=$extra")
                    errorMessage = "Failed to play audio (error: $what)"
                    try {
                        mp.release()
                    } catch (e: Exception) {
                        android.util.Log.e("MusicTrackCard", "Error releasing on error", e)
                    }
                    mediaPlayer = null
                    isLoading = false
                    false
                }
                
                player.setOnCompletionListener {
                    android.util.Log.d("MusicTrackCard", "Playback completed")
                    isPlaying = false
                    currentTime = startTime.value
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicTrackCard", "Failed to load preview", e)
                errorMessage = "Failed to load preview: ${e.message}"
                mediaPlayer = null
                isLoading = false
            }
        } else {
            // Release when collapsed
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                    mp.release()
                } catch (e: Exception) {
                    android.util.Log.e("MusicTrackCard", "Error releasing player", e)
                }
            }
            mediaPlayer = null
            isPlaying = false
            currentTime = 0f
        }
    }
    
    // Clean up MediaPlayer when composable is disposed or track changes
    DisposableEffect(track.id) {
        onDispose {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                    mp.release()
                } catch (e: Exception) {
                    android.util.Log.e("MusicTrackCard", "Error in DisposableEffect", e)
                }
            }
            mediaPlayer = null
        }
    }
    
    // Update current time while playing
    LaunchedEffect(isPlaying) {
        if (isPlaying && mediaPlayer != null) {
            while (isPlaying) {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    try {
                        currentTime = (mp.currentPosition / 1000f)
                        if (currentTime >= endTime.value) {
                            mp.pause()
                            mp.seekTo((startTime.value * 1000).toInt())
                            isPlaying = false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicTrackCard", "Error updating time", e)
                        isPlaying = false
                        break
                    }
                } else {
                    break
                }
                delay(100)
            }
        }
    }
    
    Column {
        Card(
            modifier = Modifier
                .width(160.dp)
                .clickable { onSelect() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceVariant
            ),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary) else null
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                    
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.Close else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = track.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = track.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Expanded preview controls
        AnimatedVisibility(visible = isExpanded && track.previewUrl != null) {
            Card(
                modifier = Modifier
                    .width(160.dp)
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Loading indicator or error message
                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Loading...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "Error",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                    
                    // Play/Pause button
                    Button(
                        onClick = {
                            if (isPlaying) {
                                try {
                                    mediaPlayer?.pause()
                                    isPlaying = false
                                } catch (e: Exception) {
                                    android.util.Log.e("MusicTrackCard", "Error pausing", e)
                                    isPlaying = false
                                }
                            } else {
                                mediaPlayer?.let { mp ->
                                    try {
                                        if (!mp.isPlaying) {
                                            val seekPos = (startTime.value * 1000).toInt().coerceAtLeast(0)
                                            mp.seekTo(seekPos)
                                            mp.start()
                                            isPlaying = true
                                            android.util.Log.d("MusicTrackCard", "Started playback at ${startTime.value}s")
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("MusicTrackCard", "Error starting playback", e)
                                        errorMessage = "Failed to play: ${e.message}"
                                        isPlaying = false
                                    }
                                } ?: run {
                                    android.util.Log.w("MusicTrackCard", "MediaPlayer is null, cannot play")
                                    errorMessage = "MediaPlayer not ready. Please wait for track to load."
                                }
                            }
                        },
                        enabled = !isLoading && mediaPlayer != null && duration > 0 && errorMessage == null
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPlaying) "Pause" else "Preview")
                    }
                    
                    // Time slider
                    if (duration > 0) {
                        Slider(
                            value = currentTime,
                            onValueChange = { newTime ->
                                currentTime = newTime.coerceIn(0f, duration)
                                mediaPlayer?.seekTo((currentTime * 1000).toInt())
                            },
                            valueRange = 0f..duration
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatMusicTime(currentTime),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp
                            )
                            Text(
                                text = formatMusicTime(duration),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp
                            )
                        }
                        
                        // Time range selection
                        Text(
                            text = "Select Section",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Row {
                            Text("Start:", fontSize = 11.sp)
                            Slider(
                                value = startTime.value,
                                onValueChange = { 
                                    val newValue = it.coerceIn(0f, endTime.value - 1f)
                                    startTime.value = newValue
                                    onTimeRangeChanged(newValue, endTime.value)
                                },
                                valueRange = 0f..kotlin.math.min(endTime.value - 1f, duration),
                                modifier = Modifier.weight(1f)
                            )
                            Text("${startTime.value.toInt()}s", fontSize = 11.sp, modifier = Modifier.width(30.dp))
                        }
                        
                        Row {
                            Text("End:", fontSize = 11.sp)
                            Slider(
                                value = endTime.value,
                                onValueChange = { 
                                    val newValue = it.coerceIn(startTime.value + 1f, kotlin.math.min(duration, 60f))
                                    endTime.value = newValue
                                    onTimeRangeChanged(startTime.value, newValue)
                                },
                                valueRange = kotlin.math.max(startTime.value + 1f, 0f)..kotlin.math.min(duration, 60f),
                                modifier = Modifier.weight(1f)
                            )
                            Text("${endTime.value.toInt()}s", fontSize = 11.sp, modifier = Modifier.width(30.dp))
                        }
                    }
                }
            }
        }
    }
    
}

private fun loadImageFromUri(
    context: Context,
    uri: Uri,
    onLoaded: (android.graphics.Bitmap) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        bitmap?.let { onLoaded(it) }
    } catch (e: Exception) {
        android.util.Log.e("CreateReelSheet", "Failed to load image", e)
    }
}

private fun formatMusicTime(seconds: Float): String {
    val minutes = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return String.format("%d:%02d", minutes, secs)
}

private fun createReel(
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    imageUri: Uri?,
    musicTrack: MusicTrack?,
    startTime: Float,
    endTime: Float,
    journeyViewModel: JourneyViewModel,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onLoading: (Boolean) -> Unit
) {
    if (imageUri == null) {
        onError("Please select an image")
        return
    }
    
    onLoading(true)
    
    // Launch upload in coroutine scope
    coroutineScope.launch {
        try {
            // Create description with music track info if selected
            val description = musicTrack?.let { 
                "Music: ${it.name} (${startTime.toInt()}-${endTime.toInt()}s)"
            }
            val tags = if (musicTrack != null) listOf("reel", "music") else listOf("reel")
            
            android.util.Log.d("CreateReelSheet", "Starting reel upload with image: $imageUri, music: ${musicTrack?.name}")
            
            // Create journey (reel) using JourneyViewModel
            // The upload state will be observed in LaunchedEffect above
            journeyViewModel.uploadJourney(
                imageUris = listOf(imageUri),
                bookingId = null,
                destination = null,
                description = description,
                tags = tags,
                isPublic = true,
                context = context
            )
            
            android.util.Log.d("CreateReelSheet", "Upload initiated successfully")
            // Note: onLoading(false) will be called when upload state changes
        } catch (e: Exception) {
            android.util.Log.e("CreateReelSheet", "Failed to create reel", e)
            onLoading(false)
            onError(e.message ?: "Failed to create reel")
        }
    }
}

