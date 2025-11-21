import Foundation

struct Payment: Decodable, Identifiable {
    let id: String
    let transactionId: String
    let userId: String
    let amount: Double
    let paymentStatus: String
    let transactionDate: Date
    let paymentMethod: String
    let createdAt: Date?
    let updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case transactionId = "transaction_id"
        case userId = "user_id"
        case amount
        case paymentStatus = "payment_status"
        case transactionDate = "transaction_date"
        case paymentMethod = "payment_method"
        case createdAt
        case updatedAt
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle MongoDB _id
        if let idString = try? container.decode(String.self, forKey: .id) {
            id = idString
        } else if let idDict = try? container.decode([String: String].self, forKey: .id),
                  let idValue = idDict["$oid"] {
            id = idValue
        } else {
            throw DecodingError.dataCorruptedError(forKey: .id, in: container, debugDescription: "Invalid id format")
        }
        
        transactionId = try container.decode(String.self, forKey: .transactionId)
        
        // Handle userId
        if let userIdString = try? container.decode(String.self, forKey: .userId) {
            userId = userIdString
        } else if let userIdDict = try? container.decode([String: String].self, forKey: .userId),
                  let userIdValue = userIdDict["$oid"] {
            userId = userIdValue
        } else {
            userId = ""
        }
        
        amount = try container.decode(Double.self, forKey: .amount)
        paymentStatus = try container.decode(String.self, forKey: .paymentStatus)
        
        // Handle transaction date
        if let dateString = try? container.decode(String.self, forKey: .transactionDate) {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            transactionDate = formatter.date(from: dateString) ?? ISO8601DateFormatter().date(from: dateString) ?? Date()
        } else {
            transactionDate = Date()
        }
        
        paymentMethod = try container.decode(String.self, forKey: .paymentMethod)
        createdAt = nil
        updatedAt = nil
    }
}

struct CreateFlouciPaymentRequest: Encodable {
    let amount: Double
    let successUrl: String
    let failUrl: String
    let webhookUrl: String?
    let appTransactionId: String?
    let appTransactionTime: Int64?
    let customerName: String?
    let customerPhone: String?
    let customerEmail: String?
    
    enum CodingKeys: String, CodingKey {
        case amount
        case successUrl = "success_url"
        case failUrl = "fail_url"
        case webhookUrl = "webhook_url"
        case appTransactionId = "app_transaction_id"
        case appTransactionTime = "app_transaction_time"
        case customerName = "customer_name"
        case customerPhone = "customer_phone"
        case customerEmail = "customer_email"
    }
}

struct FlouciPaymentResponse: Decodable {
    let status: Bool
    let message: String?
    let data: FlouciPaymentData?
}

struct FlouciPaymentData: Decodable {
    let paymentUrl: String?
    let paymentToken: String?
    
    enum CodingKeys: String, CodingKey {
        case paymentUrl = "payment_url"
        case paymentToken = "payment_token"
    }
}

struct FlouciPaymentStatusResponse: Decodable {
    let status: Bool
    let message: String?
    let data: FlouciPaymentStatusData?
}

struct FlouciPaymentStatusData: Decodable {
    let status: String?
    let amount: Double?
    let transactionId: String?
    
    enum CodingKeys: String, CodingKey {
        case status
        case amount
        case transactionId = "transaction_id"
    }
}

struct RecordPaymentRequest: Encodable {
    let amount: Double
    let paymentMethod: String
    let paymentStatus: String
}

