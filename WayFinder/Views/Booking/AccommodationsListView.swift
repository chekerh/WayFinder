import SwiftUI

struct AccommodationsListView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    let destinationId: String
    let destination: FlightDestination?
    let accommodationType: String
    @State private var accommodations: [Accommodation] = []
    @State private var isLoading = true
    @State private var selectedAccommodation: Accommodation? = nil
    @State private var navigateToReservation = false
    
    var typeName: String {
        switch accommodationType {
        case "hotel": return "Hôtels"
        case "airbnb": return "Airbnbs"
        case "hostel": return "Auberges"
        case "resort": return "Résorts"
        case "apartment": return "Appartements"
        default: return "Logements"
        }
    }
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            if isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if accommodations.isEmpty {
                emptyStateView
            } else {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        // Back button at top
                        HStack {
                            Button(action: { dismiss() }) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    .frame(width: 44, height: 44)
                                    .background(
                                        ThemeColors.surface(colorScheme)
                                            .opacity(colorScheme == .dark ? 0.9 : 0.95)
                                    )
                                    .clipShape(Circle())
                            }
                            .buttonStyle(.plain)
                            .padding(.leading, 24)
                            .padding(.top, 16)
                            
                            Spacer()
                        }
                        
                        // Header
                        VStack(alignment: .leading, spacing: 8) {
                            Text("\(typeName) - \(destination?.name ?? destinationId)")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 24)
                        .padding(.top, 16)
                        .padding(.bottom, 24)
                        
                        // Accommodations list
                        VStack(spacing: 16) {
                            ForEach(accommodations) { accommodation in
                                AccommodationCard(
                                    accommodation: accommodation,
                                    isSelected: selectedAccommodation?.id == accommodation.id,
                                    onTap: {
                                        selectedAccommodation = accommodation
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 24)
                        .padding(.bottom, 100)
                    }
                }
                .safeAreaPadding(.horizontal)
            }
            
            // Continue button
            if !accommodations.isEmpty {
                VStack {
                    Spacer()
                    Button {
                        navigateToReservation = true
                    } label: {
                        Text(selectedAccommodation != nil ? "Continuer avec cet hébergement" : "Continuer sans hébergement")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(
                                selectedAccommodation != nil ? ThemeColors.accent() : ThemeColors.accent().opacity(0.6)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .disabled(selectedAccommodation == nil)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 32)
                }
            }
        }
        .navigationBarHidden(true)
        .task {
            await loadAccommodations()
        }
        .navigationDestination(isPresented: $navigateToReservation) {
            ReservationScreen(
                destinationId: destinationId,
                destination: destination,
                selectedAccommodation: selectedAccommodation
            )
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "bed.double.fill")
                .font(.system(size: 64))
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            
            Text("Aucun logement disponible pour le moment")
                .font(.headline)
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            
            Text("Veuillez réessayer plus tard")
                .font(.subheadline)
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    private func loadAccommodations() async {
        isLoading = true
        // Simulate API call - in real app, fetch from backend
        try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        
        // Generate mock accommodations
        accommodations = generateMockAccommodations(
            type: accommodationType,
            destination: destination?.name ?? destinationId
        )
        
        isLoading = false
    }
    
    private func generateMockAccommodations(type: String, destination: String) -> [Accommodation] {
        let names = [
            "\(destination) Grand Hotel",
            "\(destination) Boutique Hotel",
            "\(destination) Luxury Resort",
            "\(destination) Cozy Apartment",
            "\(destination) Central Hostel"
        ]
        
        let locations = [
            "Centre-ville",
            "Près de l'aéroport",
            "Zone touristique",
            "Quartier historique",
            "Bord de mer"
        ]
        
        let amenitiesList = [
            ["WiFi", "Piscine", "Gym"],
            ["WiFi", "Petit-déjeuner", "Parking"],
            ["WiFi", "Spa", "Restaurant"],
            ["WiFi", "Cuisine équipée", "Lave-linge"],
            ["WiFi", "Salle commune", "Cuisine partagée"]
        ]
        
        return (0..<5).map { index in
            Accommodation(
                id: "\(type)_\(index)",
                name: names[index % names.count],
                type: type,
                price: Double.random(in: 50...300),
                currency: "EUR",
                rating: Double.random(in: 3.5...5.0),
                imageUrl: nil,
                location: locations[index % locations.count],
                amenities: amenitiesList[index % amenitiesList.count]
            )
        }
    }
}

struct AccommodationCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let accommodation: Accommodation
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                // Image placeholder
                RoundedRectangle(cornerRadius: 12)
                    .fill(ThemeColors.surface(colorScheme))
                    .frame(height: 180)
                    .overlay(
                        Image(systemName: "photo.fill")
                            .font(.system(size: 48))
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    )
                
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text(accommodation.name)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        Spacer()
                        
                        if isSelected {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 24))
                                .foregroundColor(ThemeColors.accent())
                        }
                    }
                    
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .font(.caption)
                            .foregroundColor(.yellow)
                        Text(String(format: "%.1f", accommodation.rating))
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        Text("•")
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        
                        Image(systemName: "mappin.circle.fill")
                            .font(.caption)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        Text(accommodation.location)
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                    
                    // Amenities
                    if !accommodation.amenities.isEmpty {
                        HStack(spacing: 8) {
                            ForEach(accommodation.amenities.prefix(3), id: \.self) { amenity in
                                Text(amenity)
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(ThemeColors.surface(colorScheme))
                                    .clipShape(Capsule())
                            }
                        }
                    }
                    
                    HStack {
                        Spacer()
                        Text(String(format: "%.2f %@/nuit", accommodation.price, accommodation.currency))
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(ThemeColors.accent())
                    }
                }
                .padding(.horizontal, 4)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(ThemeColors.surface(colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(isSelected ? ThemeColors.accent() : Color.clear, lineWidth: 2)
                    )
            )
            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 8, x: 0, y: 4)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    AccommodationsListView(
        destinationId: "test",
        destination: nil,
        accommodationType: "hotel"
    )
}

