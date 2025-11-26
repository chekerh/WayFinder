import Foundation

enum ChatModel: String, Codable, CaseIterable {
    case huggingface = "huggingface"
    case openaiGpt4oMini = "openai_gpt4o_mini"
    case openaiGpt4o = "openai_gpt4o"
    
    var displayName: String {
        switch self {
        case .huggingface:
            return "Hugging Face (Free)"
        case .openaiGpt4oMini:
            return "OpenAI GPT-4o Mini"
        case .openaiGpt4o:
            return "OpenAI GPT-4o"
        }
    }
}

struct ChatMessageRequest: Encodable {
    let message: String
    let model: ChatModel?
    
    enum CodingKeys: String, CodingKey {
        case message
        case model
    }
}

struct FlightPack: Decodable {
    let title: String
    let price: String
    let origin: String
    let destination: String
    let airline: String?
    let details: String?
}

struct ChatMessageResponse: Decodable {
    let message: String
    let modelUsed: String
    let flightPacks: [FlightPack]?
    let sessionId: String
    
    enum CodingKeys: String, CodingKey {
        case message
        case modelUsed = "model_used"
        case flightPacks = "flight_packs"
        case sessionId = "session_id"
    }
}

struct SwitchModelRequest: Encodable {
    let model: ChatModel
}

struct SwitchModelResponse: Decodable {
    let success: Bool
    let model: String
}

struct ChatHistoryItem: Decodable {
    let message: String
    let role: String // "user" or "assistant"
    let modelUsed: String
    let flightPacks: [FlightPack]?
    let createdAt: String?
    
    enum CodingKeys: String, CodingKey {
        case message
        case role
        case modelUsed = "model_used"
        case flightPacks = "flight_packs"
        case createdAt = "created_at"
    }
}

struct ClearHistoryResponse: Decodable {
    let success: Bool
}

struct AvailableModel: Decodable {
    let id: String
    let name: String
    let available: Bool
}

struct AvailableModelsResponse: Decodable {
    let models: [AvailableModel]
}

