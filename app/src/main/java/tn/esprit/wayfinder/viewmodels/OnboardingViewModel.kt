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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
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
                if (response.completed) {
                    _uiState.value = OnboardingUiState.Completed(
                        message = response.message ?: "Onboarding completed!"
                    )
                } else {
                    _uiState.value = OnboardingUiState.QuestionLoaded(
                        question = requireNotNull(response.question),
                        progress = requireNotNull(response.progress)
                    )
                }
            } catch (e: Exception) {
                // Special handling: backend returns 400 when onboarding is already completed
                if (e is HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    val message = try {
                        errorBody?.let {
                            val element = Json.parseToJsonElement(it)
                            element.jsonObject["message"]?.jsonPrimitive?.content
                        }
                    } catch (_: Exception) {
                        null
                    }

                    if (message == "Onboarding already completed") {
                        _uiState.value = OnboardingUiState.Completed(
                            message = "Onboarding already completed. Redirecting to home..."
                        )
                        return@launch
                    }
                }

                _uiState.value = OnboardingUiState.Error(
                    e.message ?: "Failed to start onboarding"
                )
            }
        }
    }

    fun submitAnswer(questionId: String, answer: Any) {
        viewModelScope.launch {
            try {
                _uiState.value = OnboardingUiState.Loading
                val sessionId = currentSessionId ?: return@launch

                val serializedAnswer: JsonElement = when (answer) {
                    is String -> JsonPrimitive(answer)
                    is List<*> -> {
                        val strings = answer.filterIsInstance<String>()
                        JsonArray(strings.map { JsonPrimitive(it) })
                    }
                    else -> JsonPrimitive(answer.toString())
                }

                val response = onboardingRepository.submitAnswer(
                    AnswerRequest(sessionId, questionId, serializedAnswer)
                )

                if (response.completed) {
                    _uiState.value = OnboardingUiState.Completed(
                        message = response.message ?: "Onboarding completed!"
                    )
                } else {
                    currentSessionId = response.sessionId
                    _uiState.value = OnboardingUiState.QuestionLoaded(
                        question = requireNotNull(response.question),
                        progress = requireNotNull(response.progress)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to submit answer")
            }
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            try {
                _uiState.value = OnboardingUiState.Loading
                val response = onboardingRepository.skipOnboarding()
                _uiState.value = OnboardingUiState.Completed(
                    message = response.message ?: "Onboarding skipped!"
                )
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to skip onboarding")
            }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            try {
                _uiState.value = OnboardingUiState.Loading
                val response = onboardingRepository.resetOnboarding()
                currentSessionId = response.sessionId
                if (response.completed) {
                    _uiState.value = OnboardingUiState.Completed(
                        message = response.message ?: "Onboarding completed!"
                    )
                } else {
                    _uiState.value = OnboardingUiState.QuestionLoaded(
                        question = requireNotNull(response.question),
                        progress = requireNotNull(response.progress)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to reset onboarding")
            }
        }
    }
}
