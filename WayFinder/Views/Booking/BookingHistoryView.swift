import SwiftUI

struct BookingHistoryView: View {
    @StateObject private var viewModel = BookingViewModel()
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
                            await viewModel.loadHistory()
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
            } else if viewModel.bookings.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "airplane.departure")
                        .font(.system(size: 48))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    Text("Aucune réservation")
                        .font(.headline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("Vos réservations apparaîtront ici")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(spacing: 0) {
                    // Header avec titre et sous-titre
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Historique des réservations")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        Text("Toutes vos réservations (\(viewModel.bookings.count))")
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.top, 20)
                    .padding(.bottom, 24)
                    
                    // Liste des réservations
                    ScrollView {
                        VStack(spacing: 16) {
                            ForEach(viewModel.bookings) { booking in
                                BookingCard(booking: booking)
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 20)
                    }
                }
            }
        }
        .navigationBarHidden(true)
        .task {
            await viewModel.loadHistory()
        }
        .refreshable {
            await viewModel.loadHistory()
        }
    }
}

private struct BookingCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let booking: Booking
    
    private var statusColor: Color {
        switch booking.status {
        case .confirmed:
            return ThemeColors.accent() // Bleu pour confirmé
        case .pending:
            return Color.gray // Gris pour en attente
        case .cancelled:
            return Color.gray // Gris pour annulé
        }
    }
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 6) {
                // Destination en bleu et gras
                Text(booking.destination)
                    .font(.headline.bold())
                    .foregroundColor(ThemeColors.accent())
                
                // Date en gris
                Text(formatDate(booking.createdAt))
                    .font(.subheadline)
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            }
            
            Spacer()
            
            // Statut aligné à droite
            Text(booking.status.displayName)
                .font(.subheadline)
                .foregroundColor(statusColor)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 8, x: 0, y: 4)
        )
    }
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .long
            displayFormatter.timeStyle = .none
            displayFormatter.locale = Locale(identifier: "fr_FR")
            return displayFormatter.string(from: date)
        }
        
        return dateString
    }
}

struct BookingHistoryView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            BookingHistoryView()
        }
    }
}

