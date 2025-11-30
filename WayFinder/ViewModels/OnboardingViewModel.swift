import Foundation
import SwiftUI

enum OnboardingUiState {
    case idle
    case loading
    case questionLoaded(question: OnboardingQuestion, progress: Progress)
    case completed(message: String)
    case error(message: String)
}

@MainActor
class OnboardingViewModel: ObservableObject {
    @Published var uiState: OnboardingUiState = .idle
    @Published var syncStatus: OnboardingSyncStatus?
    
    private var currentSessionId: String?
    private var operationCounter = AtomicInteger()
    private var latestOperationId = 0
    
    struct OnboardingSyncStatus {
        let questionsAnswered: Int
        let canResume: Bool
        let lastUpdated: Date
    }
    
    // MARK: - Start Onboarding
    
    func startOnboarding() {
        let operationId = beginOperation()
        Task {
            do {
                uiState = .loading
                let response = try await OnboardingService.shared.startOnboarding()
                
                guard !isStaleOperation(operationId) else { return }
                
                currentSessionId = response.sessionId
                
                if response.completed {
                    uiState = .completed(message: response.message ?? "Onboarding completed!")
                } else if let question = response.question, let progress = response.progress {
                    uiState = .questionLoaded(question: question, progress: progress)
                } else {
                    uiState = .error(message: "Invalid response from server")
                }
                
                await verifyProgress()
            } catch {
                guard !isStaleOperation(operationId) else { return }
                
                let errorDescription = error.localizedDescription.lowercased()
                let isAlreadyCompleted = errorDescription.contains("already completed") || 
                                        errorDescription.contains("déjà complété")
                
                // Handle "already completed" error - treat as completion, not error
                if isAlreadyCompleted {
                    uiState = .completed(message: "Onboarding already completed. Redirecting to home...")
                } else if let apiError = error as? APIError,
                   case .httpError(let statusCode, let data) = apiError,
                   statusCode == 400 {
                    // Extract message from data if available
                    var errorMessage: String?
                    if let data = data,
                       let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let message = json["message"] as? String {
                        errorMessage = message
                        let messageLower = message.lowercased()
                        if messageLower.contains("already completed") || messageLower.contains("déjà complété") {
                            uiState = .completed(message: "Onboarding already completed. Redirecting to home...")
                            return
                        }
                    }
                    
                    uiState = .error(message: error.localizedDescription)
                } else {
                    uiState = .error(message: error.localizedDescription)
                }
            }
        }
    }
    
    // MARK: - Submit Answer
    
    func submitAnswer(questionId: String, answer: AnswerValue) {
        let operationId = beginOperation()
        Task {
            do {
                guard let sessionId = currentSessionId else {
                    uiState = .error(message: "No active session")
                    return
                }
                
                uiState = .loading
                
                let request = AnswerRequest(
                    sessionId: sessionId,
                    questionId: questionId,
                    answer: answer
                )
                
                let response = try await OnboardingService.shared.submitAnswer(request)
                
                guard !isStaleOperation(operationId) else { return }
                
                if response.completed {
                    uiState = .completed(message: response.message ?? "Onboarding completed!")
                } else {
                    currentSessionId = response.sessionId
                    if let question = response.question, let progress = response.progress {
                        uiState = .questionLoaded(question: question, progress: progress)
                    } else {
                        uiState = .error(message: "Invalid response from server")
                    }
                }
                
                await verifyProgress()
            } catch {
                guard !isStaleOperation(operationId) else { return }
                uiState = .error(message: error.localizedDescription)
            }
        }
    }
    
    // MARK: - Skip Onboarding
    
    func skipOnboarding() {
        let operationId = beginOperation()
        Task {
            do {
                uiState = .loading
                let response = try await OnboardingService.shared.skipOnboarding()
                
                guard !isStaleOperation(operationId) else { return }
                
                uiState = .completed(message: response.message ?? "Onboarding skipped!")
            } catch {
                guard !isStaleOperation(operationId) else { return }
                uiState = .error(message: error.localizedDescription)
            }
        }
    }
    
    // MARK: - Reset Onboarding
    
    func resetOnboarding() {
        let operationId = beginOperation()
        Task {
            do {
                uiState = .loading
                let response = try await OnboardingService.shared.resetOnboarding()
                
                guard !isStaleOperation(operationId) else { return }
                
                currentSessionId = response.sessionId
                
                if response.completed {
                    uiState = .completed(message: response.message ?? "Onboarding completed!")
                } else if let question = response.question, let progress = response.progress {
                    uiState = .questionLoaded(question: question, progress: progress)
                } else {
                    uiState = .error(message: "Invalid response from server")
                }
                
                await verifyProgress()
            } catch {
                guard !isStaleOperation(operationId) else { return }
                uiState = .error(message: error.localizedDescription)
            }
        }
    }
    
    // MARK: - Verify Progress
    
    func verifyProgress() async {
        do {
            let status = try await OnboardingService.shared.getStatus()
            currentSessionId = status.sessionId ?? currentSessionId
            
            syncStatus = OnboardingSyncStatus(
                questionsAnswered: status.progress.questionsAnswered,
                canResume: status.canResume,
                lastUpdated: Date()
            )
            
            if status.onboardingCompleted {
                uiState = .completed(message: "Onboarding already completed. Redirecting to home...")
            }
        } catch {
            // Ignore sync errors - UI can retry on demand
            print("⚠️ [OnboardingViewModel] Failed to verify progress: \(error.localizedDescription)")
        }
    }
    
    // MARK: - Operation Management
    
    private func beginOperation() -> Int {
        let id = operationCounter.increment()
        latestOperationId = id
        return id
    }
    
    private func isStaleOperation(_ operationId: Int) -> Bool {
        return operationId != latestOperationId
    }
}

// MARK: - Atomic Integer Helper

class AtomicInteger {
    private var value: Int = 0
    private let queue = DispatchQueue(label: "com.wayfinder.atomic")
    
    func increment() -> Int {
        return queue.sync {
            value += 1
            return value
        }
    }
}
