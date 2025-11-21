package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import kotlinx.coroutines.delay
import tn.esprit.wayfinder.models.OnboardingQuestion
import tn.esprit.wayfinder.models.Progress
import tn.esprit.wayfinder.models.QuestionOption
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.OnboardingUiState
import tn.esprit.wayfinder.viewmodels.OnboardingViewModel

@Composable
fun SurveyScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))

    val uiState by onboardingViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        onboardingViewModel.startOnboarding()
    }

    when (val state = uiState) {
        is OnboardingUiState.Loading -> {
            LoadingIndicator()
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

@Composable
fun QuestionScreen(
    question: OnboardingQuestion,
    progress: Progress,
    onAnswer: (Any) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val progressValue = progress.total?.let { total ->
            if (total > 0) progress.current.toFloat() / total.toFloat() else 0f
        } ?: (progress.current.toFloat() / 8f) 

        LinearProgressIndicator(
            progress = { progressValue }, // FIX: Use lambda syntax
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = question.text,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (question.type) {
            "single_choice" -> {
                SingleChoiceQuestion(question.options ?: emptyList(), onAnswer)
            }
            "multiple_choice" -> {
                MultipleChoiceQuestion(question, onAnswer)
            }
        }
    }
}

@Composable
fun SingleChoiceQuestion(options: List<QuestionOption>, onAnswer: (Any) -> Unit) {
    Column {
        options.forEach { option ->
            Button(
                onClick = { onAnswer(option.value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(option.label)
            }
        }
    }
}

@Composable
fun MultipleChoiceQuestion(question: OnboardingQuestion, onAnswer: (Any) -> Unit) {
    var selectedOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    val options = question.options ?: emptyList()

    Column {
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = selectedOptions.contains(option.value),
                    onCheckedChange = { checked ->
                        val currentSelection = selectedOptions.toMutableList()
                        if (checked) {
                            currentSelection.add(option.value)
                        } else {
                            currentSelection.remove(option.value)
                        }
                        
                        val max = question.maxSelections
                        if (max != null && currentSelection.size > max) {
                           selectedOptions = currentSelection.takeLast(max)
                        } else {
                           selectedOptions = currentSelection
                        }
                    }
                )
                Text(text = option.label, modifier = Modifier.padding(start = 8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onAnswer(selectedOptions) },
            enabled = selectedOptions.size >= (question.minSelections ?: 1),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}


@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun CompletionMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.headlineMedium)
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
