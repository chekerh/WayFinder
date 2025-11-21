import SwiftUI

struct BookingOffersView: View {
    let destination: String
    @StateObject private var viewModel = BookingViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @State private var selectedOffer: BookingOffer?
    @State private var showReservationScreen = false
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            if viewModel.isLoadingOffers {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.offersErrorMessage {
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
                            await viewModel.searchOffers(destination: destination)
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
            } else if viewModel.offers.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "airplane.departure")
                        .font(.system(size: 48))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    Text("Aucune offre disponible")
                        .font(.headline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("Il n'y a pas d'offres disponibles pour cette destination")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(spacing: 16) {
                        Text("Offres disponibles pour \(destination)")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 20)
                            .padding(.top, 16)
                        
                        ForEach(viewModel.offers) { offer in
                            OfferCard(
                                offer: offer,
                                isSelected: selectedOffer?.id == offer.id,
                                onSelect: {
                                    selectedOffer = offer
                                }
                            )
                            .padding(.horizontal, 20)
                        }
                        
                        if let selectedOffer = selectedOffer {
                            NavigationLink(destination: createReservationScreen(for: selectedOffer), isActive: $showReservationScreen) {
                                Button(action: {
                                    showReservationScreen = true
                                }) {
                                    HStack {
                                        Image(systemName: "checkmark.circle.fill")
                                        Text("Confirmer la réservation")
                                            .font(.headline)
                                    }
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 16)
                                    .background(ThemeColors.accent())
                                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                }
                            }
                            .buttonStyle(.plain)
                            .padding(.horizontal, 20)
                            .padding(.top, 8)
                            .padding(.bottom, 20)
                        }
                    }
                }
            }
        }
        .navigationTitle("Réservation")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.searchOffers(destination: destination)
        }
    }
    
    @ViewBuilder
    private func createReservationScreen(for offer: BookingOffer) -> some View {
        // Create a FlightDestination from BookingOffer for ReservationScreen
        let flightDestination = FlightDestination(
            id: offer.id,
            name: offer.destination,
            city: offer.destination,
            country: offer.destination,
            imageUrl: nil,
            price: offer.price,
            currency: offer.currency ?? "EUR",
            description: offer.description,
            departureDate: offer.departureDate,
            arrivalDate: offer.returnDate,
            airline: offer.airline
        )
        
        ReservationScreen(
            destinationId: offer.id,
            destination: flightDestination
        )
    }
}

private struct OfferCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let offer: BookingOffer
    let isSelected: Bool
    let onSelect: () -> Void
    
    var body: some View {
        Button(action: onSelect) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(offer.destination)
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        if let airline = offer.airline {
                            Text(airline)
                                .font(.subheadline)
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        } else if let type = offer.type {
                            Text(type.capitalized)
                                .font(.subheadline)
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        }
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        Text("\(String(format: "%.2f", offer.price)) \(offer.currency ?? "TND")")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        if let departureDate = offer.departureDate {
                            Text(formatDate(departureDate))
                                .font(.caption)
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        } else if let type = offer.type {
                            Text(type.capitalized)
                                .font(.caption)
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        }
                    }
                }
                
                if let description = offer.description {
                    Text(description)
                        .font(.caption)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        .lineLimit(2)
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(ThemeColors.surface(colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(isSelected ? ThemeColors.accent() : Color.clear, lineWidth: 2)
                    )
            )
        }
        .buttonStyle(.plain)
    }
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .medium
            displayFormatter.timeStyle = .none
            displayFormatter.locale = Locale(identifier: "fr_FR")
            return displayFormatter.string(from: date)
        }
        
        return dateString
    }
}

struct BookingConfirmationView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    let confirmation: ConfirmBookingResponse
    let destination: String
    let onDismiss: () -> Void
    
    var body: some View {
        NavigationView {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 64))
                        .foregroundColor(.green)
                    
                    Text("Réservation confirmée !")
                        .font(.title.bold())
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    VStack(spacing: 12) {
                        InfoRow(label: "Destination", value: destination)
                        InfoRow(label: "N° de confirmation", value: confirmation.confirmationNumber)
                        InfoRow(label: "Statut", value: confirmation.status.displayName)
                    }
                    .padding(20)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(ThemeColors.surface(colorScheme))
                    )
                    .padding(.horizontal, 20)
                    
                    Button(action: {
                        dismiss()
                        onDismiss()
                    }) {
                        Text("Fermer")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(ThemeColors.accent())
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .padding(.horizontal, 20)
                    
                    Spacer()
                }
                .padding(.top, 40)
            }
            .navigationTitle("Confirmation")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private struct InfoRow: View {
    @Environment(\.colorScheme) private var colorScheme
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            Spacer()
            Text(value)
                .font(.subheadline.bold())
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
        }
    }
}

