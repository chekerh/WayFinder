import SwiftUI

struct BookingDetailScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    let booking: Booking
    @ObservedObject var viewModel: BookingViewModel
    
    @State private var showDeleteAlert = false
    @State private var isDeleting = false
    
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
        ZStack {
            Color(red: 0.918, green: 0.949, blue: 1.0) // #EAF2FF
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header avec bouton retour, titre et icône corbeille
                HStack {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                    .buttonStyle(.plain)
                    
                    Text("Détails de la réservation")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    Spacer()
                    
                    Button(action: {
                        showDeleteAlert = true
                    }) {
                        Image(systemName: "trash")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.red)
                    }
                    .buttonStyle(.plain)
                    .disabled(isDeleting)
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 24)
                
                // Liste des informations
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 16) {
                        // Carte Statut
                        InfoCard(
                            label: "Statut",
                            value: booking.status.displayName,
                            statusColor: statusColor
                        )
                        
                        // Carte Numéro de confirmation
                        InfoCard(
                            label: "Numéro de confirmation",
                            value: booking.confirmationNumber,
                            statusColor: nil
                        )
                        
                        // Carte Prix total
                        if let price = booking.price {
                            InfoCard(
                                label: "Prix total",
                                value: String(format: "%.1f %@", price, booking.currency ?? "EUR"),
                                statusColor: nil
                            )
                        }
                        
                        // Carte Date de réservation
                        InfoCard(
                            label: "Date de réservation",
                            value: formatDate(booking.createdAt),
                            statusColor: nil
                        )
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 32)
                }
            }
        }
        .navigationBarHidden(true)
        .safeAreaPadding(.horizontal)
        .alert("Supprimer la réservation", isPresented: $showDeleteAlert) {
            Button("Annuler", role: .cancel) { }
            Button("Supprimer", role: .destructive) {
                Task {
                    await deleteBooking()
                }
            }
        } message: {
            Text("Êtes-vous sûr de vouloir supprimer cette réservation ?")
        }
        .overlay {
            if isDeleting {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.black.opacity(0.3))
            }
        }
    }
    
    private func deleteBooking() async {
        isDeleting = true
        defer { isDeleting = false }
        
        do {
            try await viewModel.deleteBooking(id: booking.id)
            // Naviguer vers l'historique après suppression réussie
            dismiss()
        } catch {
            print("❌ [BookingDetailScreen] Error deleting booking: \(error.localizedDescription)")
            // TODO: Afficher une alerte d'erreur si nécessaire
        }
    }
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = formatter.date(from: dateString) {
            let displayFormatter = ISO8601DateFormatter()
            displayFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            displayFormatter.timeZone = TimeZone(secondsFromGMT: 0)
            return displayFormatter.string(from: date)
        }
        
        return dateString
    }
}

private struct InfoCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let label: String
    let value: String
    let statusColor: Color?
    
    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 15))
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            
            Spacer()
            
            if let statusColor = statusColor {
                // Badge de statut
                Text(value)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(statusColor)
                    )
            } else {
                // Valeur normale
                Text(value)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.white)
                .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
        )
    }
}

struct BookingDetailScreen_Previews: PreviewProvider {
    static var previews: some View {
        BookingDetailScreen(
            booking: Booking(
                id: "1",
                destination: "Paris",
                status: .confirmed,
                confirmationNumber: "CONF-H8SJBGBY",
                createdAt: "2025-11-20T20:09:48.937Z",
                price: 750.0,
                currency: "EUR",
                departureDate: nil,
                returnDate: nil
            ),
            viewModel: BookingViewModel()
        )
    }
}

