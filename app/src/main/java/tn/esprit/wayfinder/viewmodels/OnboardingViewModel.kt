package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.AnswerRequest
import tn.esprit.wayfinder.models.OnboardingQuestion
import tn.esprit.wayfinder.models.Progress
import tn.esprit.wayfinder.presentation.auth.OnboardingRepository

sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    object Loading : OnboardingUiState()
    data class QuestionLoaded(
        val question: OnboardingQuestion,
        val progress: Progress
    ) : OnboardingUiState()
    data class Completed(val message: String) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

class OnboardingViewModel(private val onboardingRepository: OnboardingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var currentSessionId: String? = null

    fun startOnboarding() {
        viewModelScope.launch {
            try {
                _uiState.value = OnboardingUiState.Loading
                val response = onboardingRepository.startOnboarding()
                currentSessionId = response.sessionId
                _uiState.value = OnboardingUiState.QuestionLoaded(
                    question = response.question,
                    progress = response.progress
                )
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to start onboarding")
            }
        }
    }

    fun submitAnswer(questionId: String, answer: Any) {
        viewModelScope.launch {
            try {
                _uiState.value = OnboardingUiState.Loading
                val sessionId = currentSessionId ?: return@launch

                val response = onboardingRepository.submitAnswer(
                    AnswerRequest(sessionId, questionId, answer)
                )

                if (response.completed) {
                    _uiState.value = OnboardingUiState.Completed(
                        message = response.message ?: "Onboarding completed!"
                    )
                } else {
                    currentSessionId = response.sessionId
                    _uiState.value = OnboardingUiState.QuestionLoaded(
                        question = response.question,
                        progress = response.progress
                    )
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to submit answer")
            }
        }
    }
}
