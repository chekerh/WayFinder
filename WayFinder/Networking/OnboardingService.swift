import Foundation

final class OnboardingService {
    static let shared = OnboardingService()
    private init() {}
    
    // MARK: - Start Onboarding
    
    func startOnboarding() async throws -> OnboardingResponse {
        let builder = DefaultRequest(
            method: "POST",
            path: "onboarding/start",
            headers: ["Content-Type": "application/json"]
        )
        
        return try await APIService.shared.request(builder, decodeTo: OnboardingResponse.self)
    }
    
    // MARK: - Submit Answer
    
    func submitAnswer(_ request: AnswerRequest) async throws -> OnboardingResponse {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        
        // Custom encoding for AnswerRequest
        var jsonDict: [String: Any] = [
            "session_id": request.sessionId,
            "question_id": request.questionId
        ]
        
        // Add answer based on type
        switch request.answer {
        case .string(let value):
            jsonDict["answer"] = value
        case .number(let value):
            jsonDict["answer"] = value
        case .array(let values):
            jsonDict["answer"] = values
        case .object(let dict):
            jsonDict["answer"] = dict
        }
        
        let jsonData = try JSONSerialization.data(withJSONObject: jsonDict)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "onboarding/answer",
            headers: ["Content-Type": "application/json"],
            body: jsonData
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
    
    func resumeOnboarding(sessionId: String?) async throws -> OnboardingResponse {
        let request = ResumeRequest(sessionId: sessionId)
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
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
            path: "onboarding/skip",
            headers: ["Content-Type": "application/json"]
        )
        
        return try await APIService.shared.request(builder, decodeTo: OnboardingResponse.self)
    }
    
    // MARK: - Reset Onboarding
    
    func resetOnboarding() async throws -> OnboardingResponse {
        let builder = DefaultRequest(
            method: "POST",
            path: "onboarding/reset",
            headers: ["Content-Type": "application/json"]
        )
        
        return try await APIService.shared.request(builder, decodeTo: OnboardingResponse.self)
    }
}

