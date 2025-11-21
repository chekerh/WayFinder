import SwiftUI

struct PaymentHistoryView: View {
    @StateObject private var viewModel = PaymentViewModel()
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.errorMessage {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 48))
                        .foregroundColor(.orange)
                    Text("Erreur")
                        .font(.headline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text(error)
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    Button(action: {
                        Task {
                            await viewModel.loadPaymentHistory()
                        }
                    }) {
                        Text("Réessayer")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(ThemeColors.accent())
                            .clipShape(Capsule())
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.payments.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "creditcard")
                        .font(.system(size: 48))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    Text("Aucun paiement")
                        .font(.headline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("Votre historique de paiements apparaîtra ici")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(spacing: 0) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Historique des paiements")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        Text("Tous vos paiements (\(viewModel.payments.count))")
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.top, 20)
                    .padding(.bottom, 24)
                    
                    // Liste des paiements
                    ScrollView {
                        VStack(spacing: 16) {
                            ForEach(viewModel.payments) { payment in
                                PaymentCard(payment: payment)
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 100)
                    }
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadPaymentHistory()
        }
        .refreshable {
            await viewModel.loadPaymentHistory()
        }
    }
}

struct PaymentCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let payment: Payment
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(payment.paymentMethod)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    Text(formatDate(payment.transactionDate))
                        .font(.caption)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text(formatAmount(payment.amount))
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    StatusBadge(status: payment.paymentStatus)
                }
            }
            
            Divider()
            
            HStack {
                Text("Transaction ID")
                    .font(.caption)
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                
                Spacer()
                
                Text(payment.transactionId)
                    .font(.caption)
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    .lineLimit(1)
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
        )
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 8, x: 0, y: 4)
    }
    
    private func formatAmount(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "TND"
        return formatter.string(from: NSNumber(value: amount)) ?? "\(amount) TND"
    }
    
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: date)
    }
}

struct StatusBadge: View {
    let status: String
    
    var body: some View {
        Text(status.capitalized)
            .font(.caption)
            .fontWeight(.semibold)
            .foregroundColor(status == "success" ? .green : .red)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(
                Capsule()
                    .fill((status == "success" ? Color.green : Color.red).opacity(0.1))
            )
    }
}

struct PaymentHistoryView_Previews: PreviewProvider {
    static var previews: some View {
        PaymentHistoryView()
    }
}

