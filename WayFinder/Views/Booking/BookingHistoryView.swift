import SwiftUI

struct BookingHistoryView: View {
    @StateObject private var viewModel = BookingViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            Color(red: 0.918, green: 0.949, blue: 1.0) // #EAF2FF
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
                    // Header avec bouton retour et titre
                    HStack {
                        Button(action: { dismiss() }) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                        }
                        .buttonStyle(.plain)
                        
                        Text("Historique des réservations")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 8)
                    
                    // Sous-titre
                    HStack {
                        Text("Voici vos dernières réservations")
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 16)
                    
                    // Liste des réservations
                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 12) {
                            ForEach(viewModel.bookings) { booking in
                                NavigationLink(destination: BookingDetailScreen(booking: booking, viewModel: viewModel)) {
                                    BookingCard(booking: booking)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 74)
                    }
                }
            }
        }
        .navigationBarHidden(true)
        .safeAreaPadding(.horizontal)
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
            return Color(red: 0.133, green: 0.694, blue: 0.298) // Vert pour confirmé
        case .pending:
            return Color.orange // Orange pour en attente
        case .cancelled:
            return Color.red // Rouge pour annulé
        }
    }
    
    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 8) {
                // Numéro de confirmation en gras
                Text(booking.confirmationNumber)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                
                // Date et heure
                Text(formatDate(booking.createdAt))
                    .font(.system(size: 13))
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                
                // Montant avec devise
                if let price = booking.price {
                    Text(String(format: "%.2f %@", price, booking.currency ?? "EUR"))
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                        .padding(.top, 4)
                }
            }
            
            Spacer()
            
            // Badge de statut
            Text(booking.status.displayName)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(statusColor)
                )
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.white)
                .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
        )
    }
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            displayFormatter.timeZone = TimeZone(secondsFromGMT: 0)
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

