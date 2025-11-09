import Foundation

final class SurveyService {
    static let shared = SurveyService()
    private init() {}
    
    func submitPreferences(_ request: SurveyPreferenceRequest) async throws -> SurveyPreferenceResponse {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "preferences",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        
        return try await APIService.shared.request(builder, decodeTo: SurveyPreferenceResponse.self)
    }
}
