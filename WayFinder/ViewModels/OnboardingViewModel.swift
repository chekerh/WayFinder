import Foundation
import SwiftUI

@MainActor
class OnboardingViewModel: ObservableObject {
    @Published var uiState: OnboardingUiState = .idle
    @Published var syncStatus: OnboardingSyncStatus?
    
    private var currentSessionId: String?
    private let service = OnboardingService.shared
    
    // MARK: - Start Onboarding
    func startOnboarding() async {
        uiState = .loading
        do {
            let response = try await service.startOnboarding()
            currentSessionId = response.sessionId
            
            if response.completed {
                uiState = .completed(message: response.message ?? "Onboarding completed!")
            } else if let question = response.question, let progress = response.progress {
                uiState = .questionLoaded(question: question, progress: progress)
            } else {
                uiState = .error("Invalid response from server")
            }
            await verifyProgress()
        } catch {
            // Special handling: backend returns error when onboarding is already completed
            let errorMessage = (error as? APIError)?.errorDescription ?? error.localizedDescription
            if errorMessage.lowercased().contains("already completed") {
                uiState = .completed(message: "Onboarding already completed. Redirecting to home...")
            } else {
                uiState = .error(errorMessage)
            }
        }
    }
    
    // MARK: - Submit Answer
    func submitAnswer(questionId: String, answer: AnswerValue) async {
        guard let sessionId = currentSessionId else {
            uiState = .error("No active session")
            return
        }
        
        uiState = .loading
        do {
            let request = AnswerRequest(sessionId: sessionId, questionId: questionId, answer: answer)
            let response = try await service.submitAnswer(request)
            currentSessionId = response.sessionId
            
            if response.completed {
                uiState = .completed(message: response.message ?? "Onboarding completed!")
            } else if let question = response.question, let progress = response.progress {
                uiState = .questionLoaded(question: question, progress: progress)
            } else {
                uiState = .error("Invalid response from server")
            }
            await verifyProgress()
        } catch {
            uiState = .error(error.localizedDescription)
        }
    }
    
    // MARK: - Skip Onboarding
    func skipOnboarding() async {
        uiState = .loading
        do {
            let response = try await service.skipOnboarding()
            uiState = .completed(message: response.message ?? "Onboarding skipped!")
        } catch {
            uiState = .error(error.localizedDescription)
        }
    }
    
    // MARK: - Reset Onboarding
    func resetOnboarding() async {
        uiState = .loading
        do {
            let response = try await service.resetOnboarding()
            currentSessionId = response.sessionId
            
            if response.completed {
                uiState = .completed(message: response.message ?? "Onboarding completed!")
            } else if let question = response.question, let progress = response.progress {
                uiState = .questionLoaded(question: question, progress: progress)
            } else {
                uiState = .error("Invalid response from server")
            }
            await verifyProgress()
        } catch {
            uiState = .error(error.localizedDescription)
        }
    }
    
    // MARK: - Verify Progress
    func verifyProgress() async {
        do {
            let status = try await service.getStatus()
            currentSessionId = status.sessionId ?? currentSessionId
            syncStatus = OnboardingSyncStatus(
                questionsAnswered: status.progress.questionsAnswered,
                canResume: status.canResume
            )
            
            if status.onboardingCompleted {
                uiState = .completed(message: "Onboarding already completed. Redirecting to home...")
            }
        } catch {
            // Ignore sync errors – UI can retry on demand
            print("⚠️ [OnboardingViewModel] Failed to verify progress: \(error.localizedDescription)")
        }
    }
}

// MARK: - OnboardingUiState
enum OnboardingUiState {
    case idle
    case loading
    case questionLoaded(question: OnboardingQuestion, progress: Progress)
    case completed(message: String)
    case error(String)
}

// MARK: - OnboardingSyncStatus
struct OnboardingSyncStatus {
    let questionsAnswered: Int
    let canResume: Bool
    let lastUpdated: Date = Date()
}

