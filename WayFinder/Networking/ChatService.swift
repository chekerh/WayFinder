import Foundation

class ChatService {
    static let shared = ChatService()
    private init() {}
    
    private let apiService = APIService.shared
    
    func sendMessage(_ message: String, model: ChatModel? = nil) async throws -> ChatMessageResponse {
        let request = ChatMessageRequest(message: message, model: model)
        let body = try JSONEncoder().encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "chat/message",
            body: body
        )
        
        return try await apiService.request(builder, decodeTo: ChatMessageResponse.self)
    }
    
    func switchModel(_ model: ChatModel) async throws -> SwitchModelResponse {
        let request = SwitchModelRequest(model: model)
        let body = try JSONEncoder().encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "chat/switch-model",
            body: body
        )
        
        return try await apiService.request(builder, decodeTo: SwitchModelResponse.self)
    }
    
    func getHistory(limit: Int = 50) async throws -> [ChatHistoryItem] {
        let builder = DefaultRequest(
            method: "GET",
            path: "chat/history",
            queryItems: [URLQueryItem(name: "limit", value: "\(limit)")]
        )
        
        return try await apiService.request(builder, decodeTo: [ChatHistoryItem].self)
    }
    
    func clearHistory() async throws -> ClearHistoryResponse {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "chat/history"
        )
        
        return try await apiService.request(builder, decodeTo: ClearHistoryResponse.self)
    }
    
    func getAvailableModels() async throws -> AvailableModelsResponse {
        let builder = DefaultRequest(
            method: "GET",
            path: "chat/models"
        )
        
        return try await apiService.request(builder, decodeTo: AvailableModelsResponse.self)
    }
}

