package tn.esprit.wayfinder.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingQuestion(
    val id: String,
    val type: String,
    val text: String,
    val options: List<QuestionOption>? = null,
    val required: Boolean = true,
    @SerialName("min_selections") val minSelections: Int? = null,
    @SerialName("max_selections") val maxSelections: Int? = null
)

@Serializable
data class QuestionOption(
    val value: String,
    val label: String,
    val min: Double? = null,
    val max: Double? = null
)

@Serializable
data class OnboardingResponse(
    @SerialName("session_id") val sessionId: String,
    val question: OnboardingQuestion,
    val progress: Progress,
    val completed: Boolean,
    @SerialName("redirect_to") val redirectTo: String? = null,
    val message: String? = null
)

@Serializable
data class Progress(
    val current: Int,
    val total: Int? = null
)

@Serializable
data class OnboardingStatus(
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean,
    @SerialName("session_id") val sessionId: String? = null,
    val progress: OnboardingProgress,
    @SerialName("can_resume") val canResume: Boolean
)

@Serializable
data class OnboardingProgress(
    @SerialName("questions_answered") val questionsAnswered: Int,
    @SerialName("current_question_id") val currentQuestionId: String? = null
)

@Serializable
data class AnswerRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("question_id") val questionId: String,
    @Contextual val answer: Any
)

@Serializable
data class ResumeRequest(val session_id: String?)
