import SwiftUI

struct BookingHistoryView: View {
    @StateObject private var viewModel = BookingViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    var onBackToHome: (() -> Void)? = nil
    
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
                    Text("booking_history_title")
                        .font(.headline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("booking_history_empty")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(spacing: 0) {
                    // Header avec bouton retour et titre
                    HStack {
                        Button(action: {
                            if let onBackToHome = onBackToHome {
                                onBackToHome()
                            } else {
                                dismiss()
                            }
                        }) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                        }
                        .buttonStyle(.plain)
                        
                        Text("booking_history_title")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 8)
                    
                    // Sous-titre
                    HStack {
                        Text("booking_history_subtitle")
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 16)
                    
                    // Liste des réservations
                    ScrollView(showsIndicators: false) {
                        LazyVStack(alignment: .leading, spacing: 12) {
                            ForEach(viewModel.bookings) { booking in
                                BookingCard(booking: booking, viewModel: viewModel)
                                    .id(booking.id) // Ensure unique IDs for proper rendering order
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
    @ObservedObject var viewModel: BookingViewModel
    
    @State private var isReactivating = false
    @State private var showReactivateError = false
    @State private var reactivateErrorMessage: String?
    @State private var showDeleteConfirmation = false
    @State private var isDeleting = false
    @State private var offset: CGFloat = 0
    
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
        ZStack(alignment: .trailing) {
            // Bouton de suppression (visible quand on swipe vers la gauche)
            HStack {
                Spacer()
                Button(action: {
                    withAnimation(.spring()) {
                        offset = 0
                    }
                    // Attendre un peu avant de supprimer pour voir l'animation
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showDeleteConfirmation = true
                    }
                }) {
                    Image(systemName: "trash")
                        .foregroundColor(.white)
                        .font(.system(size: 16, weight: .medium))
                        .frame(width: 60)
                        .frame(maxHeight: .infinity)
                        .background(
                            Color.red
                                .clipShape(Rectangle())
                        )
                }
                .buttonStyle(.plain)
                .disabled(isDeleting)
            }
            .opacity(offset < -10 ? 1 : 0) // Visible seulement quand on swipe
            .allowsHitTesting(offset < -10) // Désactiver les interactions quand invisible
            
            // Contenu de la carte
            VStack(spacing: 0) {
                HStack(spacing: 12) {
                    NavigationLink(destination: BookingDetailScreen(booking: booking, viewModel: viewModel)) {
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 8) {
                                // Destination avec pays en gras et grand (ex: "Paris, France")
                                Text(DestinationHelper.getFullDestinationName(from: booking.destination))
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                
                                // Numéro de confirmation en plus petit
                                Text(booking.confirmationNumber)
                                    .font(.system(size: 13, weight: .regular))
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                
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
                    }
                    .buttonStyle(.plain)
                }
            
                // Bouton Réserver à nouveau pour les réservations annulées
                if booking.status == .cancelled {
                    Divider()
                        .padding(.horizontal, 16)
                    
                    Button(action: {
                        Task {
                            await reactivateBooking()
                        }
                    }) {
                        HStack {
                            if isReactivating {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                            } else {
                                Image(systemName: "arrow.clockwise")
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            Text("Réserver à nouveau")
                                .font(.system(size: 14, weight: .semibold))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(ThemeColors.accent())
                    }
                    .disabled(isReactivating)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
            }
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(ThemeColors.surface(colorScheme))
            )
            .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
            .offset(x: offset)
            .gesture(
                DragGesture()
                    .onChanged { value in
                        if value.translation.width < 0 {
                            // Swipe vers la gauche
                            offset = max(value.translation.width, -60)
                        } else if offset < 0 {
                            // Permettre de revenir en arrière
                            offset = min(0, offset + value.translation.width)
                        }
                    }
                    .onEnded { value in
                        if value.translation.width < -30 || offset < -30 {
                            // Ouvrir le bouton de suppression
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                offset = -60
                            }
                        } else {
                            // Fermer le bouton de suppression
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                offset = 0
                            }
                        }
                    }
            )
        }
        .clipped() // Empêcher le bouton de dépasser les bords
        .contentShape(Rectangle())
        .alert("Erreur", isPresented: $showReactivateError) {
            Button("OK", role: .cancel) { }
        } message: {
            if let errorMessage = reactivateErrorMessage {
                Text(errorMessage)
            }
        }
        .alert("Supprimer la réservation", isPresented: $showDeleteConfirmation) {
            Button("Annuler", role: .cancel) {
                showDeleteConfirmation = false
            }
            Button("Supprimer", role: .destructive) {
                Task {
                    await deleteBooking()
                }
            }
        } message: {
            Text("Êtes-vous sûr de vouloir supprimer définitivement cette réservation ? Cette action est irréversible.")
        }
    }
    
    private func deleteBooking() async {
        isDeleting = true
        defer { isDeleting = false }
        
        do {
            try await viewModel.deleteBooking(id: booking.id)
            print("✅ [BookingCard] Booking deleted: \(booking.confirmationNumber)")
        } catch {
            print("❌ [BookingCard] Error deleting booking: \(error.localizedDescription)")
            reactivateErrorMessage = error.localizedDescription
            showReactivateError = true
        }
    }
    
    private func reactivateBooking() async {
        isReactivating = true
        defer { isReactivating = false }
        
        do {
            try await viewModel.reactivateBooking(booking)
            print("✅ [BookingCard] Booking reactivated: \(booking.confirmationNumber)")
        } catch {
            print("❌ [BookingCard] Error reactivating booking: \(error.localizedDescription)")
            reactivateErrorMessage = error.localizedDescription
            showReactivateError = true
        }
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

