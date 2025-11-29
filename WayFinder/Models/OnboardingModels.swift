import Foundation

// MARK: - OnboardingQuestion
struct OnboardingQuestion: Decodable, Identifiable {
    let id: String
    let type: QuestionType
    let text: String
    let options: [QuestionOption]?
    let required: Bool
    let minSelections: Int?
    let maxSelections: Int?
    
    enum QuestionType: String, Decodable {
        case singleChoice = "single_choice"
        case multipleChoice = "multiple_choice"
        case text = "text"
        case number = "number"
        case date = "date"
    }
    
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

// MARK: - QuestionOption
struct QuestionOption: Decodable, Identifiable {
    let id: String
    let value: String
    let label: String
    let min: Double?
    let max: Double?
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        value = try container.decode(String.self, forKey: .value)
        label = try container.decode(String.self, forKey: .label)
        min = try container.decodeIfPresent(Double.self, forKey: .min)
        max = try container.decodeIfPresent(Double.self, forKey: .max)
        id = value // Use value as id
    }
    
    enum CodingKeys: String, CodingKey {
        case value
        case label
        case min
        case max
    }
}

// MARK: - OnboardingResponse
struct OnboardingResponse: Decodable {
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

// MARK: - Progress
struct Progress: Decodable {
    let current: Int
    let total: Int?
}

// MARK: - OnboardingStatus
struct OnboardingStatus: Decodable {
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

// MARK: - OnboardingProgress
struct OnboardingProgress: Decodable {
    let questionsAnswered: Int
    let currentQuestionId: String?
    
    enum CodingKeys: String, CodingKey {
        case questionsAnswered = "questions_answered"
        case currentQuestionId = "current_question_id"
    }
}

// MARK: - AnswerRequest
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
        
        // Encode answer based on type
        switch answer {
        case .string(let value):
            try container.encode(value, forKey: .answer)
        case .number(let value):
            try container.encode(value, forKey: .answer)
        case .array(let values):
            try container.encode(values, forKey: .answer)
        }
    }
}

// MARK: - AnswerValue
enum AnswerValue {
    case string(String)
    case number(Double)
    case array([String])
}

// MARK: - ResumeRequest
struct ResumeRequest: Encodable {
    let sessionId: String?
    
    enum CodingKeys: String, CodingKey {
        case sessionId = "session_id"
    }
}

