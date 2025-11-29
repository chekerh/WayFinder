import Foundation

final class OnboardingService {
    static let shared = OnboardingService()
    private init() {}
    
    // MARK: - Start Onboarding
    func startOnboarding() async throws -> OnboardingResponse {
        let builder = DefaultRequest(
            method: "POST",
            path: "onboarding/start"
        )
        return try await APIService.shared.request(builder, decodeTo: OnboardingResponse.self)
    }
    
    // MARK: - Submit Answer
    func submitAnswer(_ request: AnswerRequest) async throws -> OnboardingResponse {
        // AnswerRequest has custom CodingKeys, so don't use convertToSnakeCase
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "onboarding/answer",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: OnboardingResponse.self)
    }
    
    // MARK: - Get Status
    func getStatus() async throws -> OnboardingStatus {
        let builder = DefaultRequest(
            method: "GET",
            path: "onboarding/status"
        )
        return try await APIService.shared.request(builder, decodeTo: OnboardingStatus.self)
    }
    
    // MARK: - Resume Onboarding
    func resumeOnboarding(_ request: ResumeRequest) async throws -> OnboardingResponse {
        // ResumeRequest has custom CodingKeys, so don't use convertToSnakeCase
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "onboarding/resume",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: OnboardingResponse.self)
    }
    
    // MARK: - Skip Onboarding
    func skipOnboarding() async throws -> OnboardingResponse {
        let builder = DefaultRequest(
            method: "POST",
            path: "onboarding/skip"
        )
        return try await APIService.shared.request(builder, decodeTo: OnboardingResponse.self)
    }
    
    // MARK: - Reset Onboarding
    func resetOnboarding() async throws -> OnboardingResponse {
        let builder = DefaultRequest(
            method: "POST",
            path: "onboarding/reset"
        )
        return try await APIService.shared.request(builder, decodeTo: OnboardingResponse.self)
    }
}

