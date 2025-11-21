import Foundation

@MainActor
final class PaymentViewModel: ObservableObject {
    @Published var payments: [Payment] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: PaymentService
    
    init(service: PaymentService = .shared) {
        self.service = service
    }
    
    func loadPaymentHistory() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [PaymentViewModel] Loading payment history")
        do {
            payments = try await service.getPaymentHistory()
            print("✅ [PaymentViewModel] Loaded \(payments.count) payments")
        } catch {
            print("❌ [PaymentViewModel] Error loading payment history: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
}

