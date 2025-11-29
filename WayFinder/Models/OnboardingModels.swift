import Foundation

// MARK: - Onboarding Question Models

struct OnboardingQuestion: Codable, Identifiable {
    let id: String
    let type: String
    let text: String
    let options: [QuestionOption]?
    let required: Bool
    let minSelections: Int?
    let maxSelections: Int?
    
    enum CodingKeys: String, CodingKey {
        case id
        case type
        case text
        case options
        case required
        case minSelections = "min_selections"
        case maxSelections = "max_selections"
    }
}

struct QuestionOption: Codable, Identifiable, Hashable {
    let value: String
    let label: String
    let min: Double?
    let max: Double?
    
    var id: String { value }
}

struct Progress: Codable {
    let current: Int
    let total: Int?
}

// MARK: - Onboarding Response Models

struct OnboardingResponse: Codable {
    let sessionId: String
    let question: OnboardingQuestion?
    let progress: Progress?
    let completed: Bool
    let redirectTo: String?
    let message: String?
    
    enum CodingKeys: String, CodingKey {
        case sessionId = "session_id"
        case question
        case progress
        case completed
        case redirectTo = "redirect_to"
        case message
    }
}

struct OnboardingStatus: Codable {
    let onboardingCompleted: Bool
    let sessionId: String?
    let progress: OnboardingProgress
    let canResume: Bool
    
    enum CodingKeys: String, CodingKey {
        case onboardingCompleted = "onboarding_completed"
        case sessionId = "session_id"
        case progress
        case canResume = "can_resume"
    }
}

struct OnboardingProgress: Codable {
    let questionsAnswered: Int
    let currentQuestionId: String?
    
    enum CodingKeys: String, CodingKey {
        case questionsAnswered = "questions_answered"
        case currentQuestionId = "current_question_id"
    }
}

// MARK: - Onboarding Request Models

struct AnswerRequest: Encodable {
    let sessionId: String
    let questionId: String
    let answer: AnswerValue
    
    enum CodingKeys: String, CodingKey {
        case sessionId = "session_id"
        case questionId = "question_id"
        case answer
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(sessionId, forKey: .sessionId)
        try container.encode(questionId, forKey: .questionId)
        
        // Encode answer based on its type
        switch answer {
        case .string(let value):
            try container.encode(value, forKey: .answer)
        case .number(let value):
            try container.encode(value, forKey: .answer)
        case .array(let values):
            try container.encode(values, forKey: .answer)
        case .object(let dict):
            try container.encode(dict, forKey: .answer)
        }
    }
}

enum AnswerValue {
    case string(String)
    case number(Double)
    case array([String])
    case object([String: String])
}

struct ResumeRequest: Codable {
    let sessionId: String?
    
    enum CodingKeys: String, CodingKey {
        case sessionId = "session_id"
    }
}

