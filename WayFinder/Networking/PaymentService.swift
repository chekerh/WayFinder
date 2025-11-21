import Foundation

@MainActor
final class PaymentService {
    static let shared = PaymentService()
    private init() {}
    
    /// Récupère l'historique des paiements de l'utilisateur
    func getPaymentHistory() async throws -> [Payment] {
        let builder = DefaultRequest(
            method: "GET",
            path: "payment/history"
        )
        return try await APIService.shared.request(builder, decodeTo: [Payment].self)
    }
    
    /// Enregistre un paiement (pour les tests)
    func recordPayment(
        amount: Double,
        paymentMethod: String,
        paymentStatus: String
    ) async throws -> Payment {
        let request = RecordPaymentRequest(
            amount: amount,
            paymentMethod: paymentMethod,
            paymentStatus: paymentStatus
        )
        
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "payment/record",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Payment.self)
    }
    
    /// Crée un paiement Flouci
    func createFlouciPayment(
        amount: Double,
        successUrl: String,
        failUrl: String,
        webhookUrl: String? = nil,
        appTransactionId: String? = nil,
        appTransactionTime: Int64? = nil,
        customerName: String? = nil,
        customerPhone: String? = nil,
        customerEmail: String? = nil
    ) async throws -> FlouciPaymentResponse {
        let request = CreateFlouciPaymentRequest(
            amount: amount,
            successUrl: successUrl,
            failUrl: failUrl,
            webhookUrl: webhookUrl,
            appTransactionId: appTransactionId,
            appTransactionTime: appTransactionTime,
            customerName: customerName,
            customerPhone: customerPhone,
            customerEmail: customerEmail
        )
        
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "payment/flouci/create",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: FlouciPaymentResponse.self)
    }
    
    /// Récupère le statut d'un paiement Flouci
    func getFlouciPaymentStatus(paymentId: String) async throws -> FlouciPaymentStatusResponse {
        let builder = DefaultRequest(
            method: "GET",
            path: "payment/flouci/status/\(paymentId)"
        )
        return try await APIService.shared.request(builder, decodeTo: FlouciPaymentStatusResponse.self)
    }
}

