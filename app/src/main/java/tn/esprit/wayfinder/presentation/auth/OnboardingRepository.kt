package tn.esprit.wayfinder.presentation.auth

import tn.esprit.wayfinder.models.AnswerRequest
import tn.esprit.wayfinder.models.OnboardingResponse
import tn.esprit.wayfinder.models.OnboardingStatus
import tn.esprit.wayfinder.models.ResumeRequest
import tn.esprit.wayfinder.network.ApiService

class OnboardingRepository(private val apiService: ApiService) {

    suspend fun startOnboarding(): OnboardingResponse {
        return apiService.startOnboarding()
    }

    suspend fun submitAnswer(answer: AnswerRequest): OnboardingResponse {
        return apiService.submitAnswer(answer)
    }

    suspend fun getStatus(): OnboardingStatus {
        return apiService.getOnboardingStatus()
    }

    suspend fun resume(sessionId: String?): OnboardingResponse {
        return apiService.resumeOnboarding(ResumeRequest(sessionId))
    }

    suspend fun skipOnboarding(): OnboardingResponse {
        return apiService.skipOnboarding()
    }

    suspend fun resetOnboarding(): OnboardingResponse {
        return apiService.resetOnboarding()
    }
}
