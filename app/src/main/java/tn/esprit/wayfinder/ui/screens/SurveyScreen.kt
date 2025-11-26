package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import tn.esprit.wayfinder.models.OnboardingQuestion
import tn.esprit.wayfinder.models.Progress
import tn.esprit.wayfinder.models.QuestionOption
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.OnboardingUiState
import tn.esprit.wayfinder.viewmodels.OnboardingViewModel
import tn.esprit.wayfinder.viewmodels.OnboardingSyncStatus
import tn.esprit.wayfinder.viewmodels.UserViewModel
import tn.esprit.wayfinder.manager.TokenManager

private const val MAX_ONBOARDING_QUESTIONS = 5

@Composable
fun SurveyScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val userViewModel: UserViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))

    val uiState by onboardingViewModel.uiState.collectAsState()
    val syncStatus by onboardingViewModel.syncStatus.collectAsState()
    
    // Check if user already completed onboarding - if so, reset it
    val currentUser = remember { tokenManager.getUser() }
    val shouldReset = remember { currentUser?.onboardingCompleted == true }

    LaunchedEffect(Unit) {
        onboardingViewModel.verifyProgress()
        if (shouldReset) {
            // User wants to retake - reset onboarding first
            onboardingViewModel.resetOnboarding()
        } else {
        onboardingViewModel.startOnboarding()
        }
    }

    when (val state = uiState) {
        is OnboardingUiState.Loading -> {
            LoadingIndicatorWithSkip(
                onSkip = {
                    onboardingViewModel.skipOnboarding()
                }
            )
        }
        is OnboardingUiState.QuestionLoaded -> {
            QuestionScreen(
                question = state.question,
                progress = state.progress,
                onAnswer = {
                    onboardingViewModel.submitAnswer(state.question.id, it)
                },
                onSkip = {
                    onboardingViewModel.skipOnboarding()
                },
                syncStatus = syncStatus
            )
        }
        is OnboardingUiState.Completed -> {
            LaunchedEffect(state) {
                // Refresh user data from backend to get latest onboarding status
                val tokenManager = TokenManager(context)
                val wasSkipped = state.message.contains("skipped", ignoreCase = true)
                
                try {
                    // Try to refresh user profile from backend
                    userViewModel.loadProfile()
                    
                    // Wait for profile to load with timeout
                    var attempts = 0
                    while (attempts < 10) { // Wait up to 2 seconds (10 * 200ms)
                        delay(200)
                        val updatedUser = tokenManager.getUser()
                        if (updatedUser != null && updatedUser.onboardingCompleted == true) {
                            // Profile successfully loaded and updated
                            break
                        }
                        attempts++
                    }
                    
                    // Get updated user from token manager (it should be updated by UserViewModel)
                    val updatedUser = tokenManager.getUser()
                    if (updatedUser != null) {
                        // Ensure flags are set correctly
                        val finalUser = updatedUser.copy(
                            onboardingCompleted = true,
                            onboardingSkipped = wasSkipped
                        )
                        tokenManager.saveUser(finalUser)
                    } else {
                        // Fallback: update locally if profile load didn't work
                        val currentUser = tokenManager.getUser()
                        if (currentUser != null) {
                            val fallbackUser = currentUser.copy(
                                onboardingCompleted = true,
                                onboardingSkipped = wasSkipped
                            )
                            tokenManager.saveUser(fallbackUser)
                        }
                    }
                } catch (e: Exception) {
                    // If refresh fails, update locally
                    val currentUser = tokenManager.getUser()
                    if (currentUser != null) {
                        val updatedUser = currentUser.copy(
                            onboardingCompleted = true,
                            onboardingSkipped = wasSkipped
                        )
                        tokenManager.saveUser(updatedUser)
                    }
                }
                
                delay(2000) // Show completion message briefly (2 seconds)
                onComplete()
            }
            CompletionMessage(message = state.message)
        }
        is OnboardingUiState.Error -> {
            ErrorScreen(message = state.message) {
                onboardingViewModel.startOnboarding()
            }
        }
        is OnboardingUiState.Idle -> {
            LoadingIndicator()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun QuestionScreen(
    question: OnboardingQuestion,
    progress: Progress,
    onAnswer: (Any) -> Unit,
    onSkip: () -> Unit = {},
    syncStatus: OnboardingSyncStatus? = null
) {
    val totalSteps = minOf(progress.total ?: MAX_ONBOARDING_QUESTIONS, MAX_ONBOARDING_QUESTIONS)
    val progressValue = (progress.current.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
    val backgroundImageUrl = remember(question.id, question.text) { 
        getImageForQuestion(question.id, question.text) 
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-screen background image
        if (backgroundImageUrl != null) {
            AsyncImage(
                model = backgroundImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.4f),
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        }

        // Dark overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
        )

        // Top skip button
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(10f)
        ) {
            Text(
                text = "Passer",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        // Main content - using BoxWithConstraints to better manage space
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp)) // Space for skip button

            // Pinterest-style progress bar
            PinterestProgressBar(
                progress = progressValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
            
            // Progress dots
            PinterestProgressDots(
                currentStep = progress.current,
                totalSteps = totalSteps,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            syncStatus?.let { status ->
                SyncStatusIndicator(
                    status = status,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Pinterest-style question card with animations - takes available space
            Box(
                modifier = Modifier.weight(1f)
            ) {
                PinterestQuestionCard(
                    question = question.text,
                    questionId = question.id,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (question.type) {
                        "single_choice" -> {
                            PinterestSingleChoiceQuestion(
                                question.options ?: emptyList(),
                                onAnswer
                            )
                        }
                        "multiple_choice" -> {
                            PinterestMultipleChoiceQuestion(question, onAnswer)
                        }
                    }
                }
            }
            
            // Skip button at the bottom of the page
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Passer cette étape",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SyncStatusIndicator(
    status: OnboardingSyncStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        val statusText = if (status.canResume) {
            "${status.questionsAnswered} réponses sauvegardées • reprise possible"
        } else {
            "${status.questionsAnswered} réponses sauvegardées"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PinterestSingleChoiceQuestion(
    options: List<QuestionOption>,
    onAnswer: (Any) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(
                        durationMillis = 400 + (index * 50),
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 400 + (index * 50)
                    )
                )
            ) {
                PinterestInterestCard(
                    label = option.label,
                    icon = getIconForInterest(option.label),
                    isSelected = isSelected,
                    onClick = {
                        selectedIndex = index
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Confirm button for single choice
        val canProceed = selectedIndex != null
        
        AnimatedVisibility(
            visible = canProceed,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut()
        ) {
            Button(
                onClick = {
                    selectedIndex?.let { idx ->
                        coroutineScope.launch {
                            onAnswer(options[idx].value)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = "Confirmer",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PinterestMultipleChoiceQuestion(
    question: OnboardingQuestion,
    onAnswer: (Any) -> Unit
) {
    var selectedOptions by remember { mutableStateOf<Set<String>>(emptySet()) }
    val options = question.options ?: emptyList()
    val minSelections = question.minSelections ?: 1
    val maxSelections = question.maxSelections

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Use grid layout for better visual appeal (Pinterest-style)
        if (options.size > 4) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(options.size) { index ->
                    val option = options[index]
                    val isSelected = selectedOptions.contains(option.value)
                    
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(
                                durationMillis = 300 + (index * 30),
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(300 + (index * 30))
                        )
                    ) {
                        PinterestInterestCard(
                            label = option.label,
                            icon = getIconForInterest(option.label),
                            isSelected = isSelected,
                            onClick = {
                                selectedOptions = if (isSelected) {
                                    selectedOptions - option.value
                                } else {
                                    val newSelection = selectedOptions + option.value
                                    if (maxSelections != null && newSelection.size > maxSelections) {
                                        // Remove oldest selection if max reached
                                        selectedOptions.drop(1).toSet() + option.value
                        } else {
                                        newSelection
                                    }
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // For fewer options, use column layout
            options.forEachIndexed { index, option ->
                val isSelected = selectedOptions.contains(option.value)
                
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(
                            durationMillis = 400 + (index * 50),
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(400 + (index * 50))
                    )
                ) {
                    PinterestInterestCard(
                        label = option.label,
                        icon = getIconForInterest(option.label),
                        isSelected = isSelected,
                        onClick = {
                            selectedOptions = if (isSelected) {
                                selectedOptions - option.value
                            } else {
                                val newSelection = selectedOptions + option.value
                                if (maxSelections != null && newSelection.size > maxSelections) {
                                    selectedOptions.drop(1).toSet() + option.value
                                } else {
                                    newSelection
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pinterest-style next button
        val canProceed = selectedOptions.size >= minSelections
        
        AnimatedVisibility(
            visible = canProceed,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut()
        ) {
            Button(
                onClick = { onAnswer(selectedOptions.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoadingIndicatorWithSkip(
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Skip button at top right
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Preparing your experience...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale)
                    .alpha(alpha),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Preparing your experience...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CompletionMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SurveyScreenPreview() {
    WayFinderTheme {
        SurveyScreen(onComplete = {})
    }
}
