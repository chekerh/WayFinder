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
import androidx.compose.ui.tooling.preview.Preview
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
import tn.esprit.wayfinder.manager.TokenManager

@Composable
fun SurveyScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))

    val uiState by onboardingViewModel.uiState.collectAsState()
    
    // Check if user already completed onboarding - if so, reset it
    val currentUser = remember { tokenManager.getUser() }
    val shouldReset = remember { currentUser?.onboardingCompleted == true }

    LaunchedEffect(Unit) {
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
                }
            )
        }
        is OnboardingUiState.Completed -> {
            LaunchedEffect(state) {
                // Refresh user data to update onboarding status
                val tokenManager = TokenManager(context)
                val currentUser = tokenManager.getUser()
                if (currentUser != null) {
                    // Check if it was skipped by checking the message
                    val wasSkipped = state.message.contains("skipped", ignoreCase = true)
                    val updatedUser = currentUser.copy(
                        onboardingCompleted = true,
                        onboardingSkipped = wasSkipped
                    )
                    tokenManager.saveUser(updatedUser)
                }
                delay(2000)
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
    onAnswer: (Any) -> Unit
) {
    val progressValue = progress.total?.let { total ->
        if (total > 0) progress.current.toFloat() / total.toFloat() else 0f
    } ?: (progress.current.toFloat() / 8f)
    
    val totalSteps = progress.total ?: 8

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Pinterest-style progress bar
            PinterestProgressBar(
                progress = progressValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress dots
            PinterestProgressDots(
                currentStep = progress.current,
                totalSteps = totalSteps,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Pinterest-style question card with animations
            PinterestQuestionCard(question = question.text) {
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
            
            Spacer(modifier = Modifier.weight(1f))
        }
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
                        // Auto-submit after selection with a small delay for animation
                        coroutineScope.launch {
                            delay(300)
                            onAnswer(option.value)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
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
