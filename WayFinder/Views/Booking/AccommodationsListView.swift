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
        defer { isLoading = false }
        
        do {
            // Essayer de charger depuis le cache d'abord
            let cityName = destination?.city ?? destination?.name ?? destinationId
            let cacheKey = cityName.lowercased().replacingOccurrences(of: " ", with: "_")
            let cache = AppDataCache.shared.hotelsCache(for: cacheKey, type: accommodationType)
            
            if let cachedAccommodations = cache.load() {
                print("✅ [AccommodationsListView] Loaded \(cachedAccommodations.count) accommodations from cache")
                accommodations = cachedAccommodations
                
                // Mettre à jour en arrière-plan
                Task {
                    await refreshAccommodations()
                }
                return
            }
            
            // Pas de cache : charger depuis l'API
            await refreshAccommodations()
        } catch {
            print("❌ [AccommodationsListView] Error loading accommodations: \(error.localizedDescription)")
            // En cas d'erreur, utiliser les données mockées comme fallback
            accommodations = generateMockAccommodations(
                type: accommodationType,
                destination: destination?.name ?? destinationId
            )
        }
    }
    
    private func refreshAccommodations() async {
        do {
            // Obtenir le code de la ville depuis la destination
            // Utiliser le nom de la ville ou l'ID comme fallback
            let cityName = destination?.city ?? destination?.name ?? destinationId
            
            // Convertir le nom de la ville en code d'aéroport si possible
            // Sinon utiliser le destinationId qui pourrait être un code d'aéroport
            let locationCode: String
            if let city = destination?.city, let code = getCityCode(from: city) {
                locationCode = code
            } else if destinationId.count == 3 {
                // Si destinationId est un code d'aéroport (3 lettres)
                locationCode = destinationId.uppercased()
            } else {
                // Sinon utiliser le nom de la ville
                locationCode = cityName
            }
            
            // Appeler l'API pour récupérer les hôtels
            let response = try await CatalogService.shared.searchHotels(
                locationCode: locationCode,
                checkInDate: getDefaultCheckInDate(),
                checkOutDate: getDefaultCheckOutDate(),
                adults: 2,
                accommodationType: accommodationType
            )
            
            accommodations = response.data
            
            // Sauvegarder dans le cache avec une clé basée sur le nom de la ville
            let cacheKey = cityName.lowercased().replacingOccurrences(of: " ", with: "_")
            let cache = AppDataCache.shared.hotelsCache(for: cacheKey, type: accommodationType)
            cache.store(response.data)
            
            print("✅ [AccommodationsListView] Loaded \(response.data.count) accommodations from API for \(locationCode)")
        } catch {
            print("❌ [AccommodationsListView] Error fetching accommodations from API: \(error.localizedDescription)")
            // En cas d'erreur API, utiliser les données mockées comme fallback
            if accommodations.isEmpty {
                accommodations = generateMockAccommodations(
                    type: accommodationType,
                    destination: destination?.name ?? destinationId
                )
            }
        }
    }
    
    private func getCityCode(from cityName: String) -> String? {
        // Mapper les noms de villes vers leurs codes d'aéroport
        let cityMap: [String: String] = [
            "paris": "CDG",
            "london": "LHR",
            "barcelona": "BCN",
            "madrid": "MAD",
            "rome": "FCO",
            "dubai": "DXB",
            "amsterdam": "AMS",
            "frankfurt": "FRA",
            "munich": "MUC",
            "istanbul": "IST",
            "cairo": "CAI",
            "tunis": "TUN",
            "berlin": "BER",
            "vienna": "VIE",
            "prague": "PRG",
            "budapest": "BUD",
            "athens": "ATH",
            "lisbon": "LIS",
            "copenhagen": "CPH",
            "stockholm": "STO",
            "oslo": "OSL",
            "helsinki": "HEL",
            "dublin": "DUB",
            "edinburgh": "EDI",
            "zurich": "ZUR",
            "brussels": "BRU"
        ]
        
        return cityMap[cityName.lowercased()]
    }
    
    private func getDefaultCheckInDate() -> String {
        let calendar = Calendar.current
        let tomorrow = calendar.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        return formatter.string(from: tomorrow)
    }
    
    private func getDefaultCheckOutDate() -> String {
        let calendar = Calendar.current
        let inThreeDays = calendar.date(byAdding: .day, value: 3, to: Date()) ?? Date()
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        return formatter.string(from: inThreeDays)
    }
    
    private func generateMockAccommodations(type: String, destination: String) -> [Accommodation] {
        let names: [String]
        let amenitiesList: [[String]]
        let defaultImages: [String]
        
        // Define a common set of locations
        let locations = [
            "Centre-ville",
            "Près de l'aéroport",
            "Zone touristique",
            "Quartier historique",
            "Bord de mer"
        ]
        
        switch type {
        case "hotel":
            names = [
                "\(destination) Grand Hotel",
                "\(destination) Boutique Hotel",
                "\(destination) City Center Hotel",
                "\(destination) Luxury Suites",
                "\(destination) Business Hotel"
            ]
            amenitiesList = [
                ["WiFi", "Piscine", "Gym"],
                ["WiFi", "Petit-déjeuner", "Parking"],
                ["WiFi", "Spa", "Restaurant"],
                ["WiFi", "Service en chambre", "Concierge"],
                ["WiFi", "Salles de réunion", "Navette aéroport"]
            ]
            defaultImages = [
                "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800&h=600&fit=crop&q=80"
            ]
        case "airbnb":
            names = [
                "Appartement cosy à \(destination)",
                "Loft moderne avec vue sur \(destination)",
                "Maison de ville spacieuse à \(destination)",
                "Studio charmant près du centre",
                "Villa avec piscine privée"
            ]
            amenitiesList = [
                ["WiFi", "Cuisine équipée", "Lave-linge"],
                ["WiFi", "Parking gratuit", "TV connectée"],
                ["WiFi", "Jardin", "Barbecue"],
                ["WiFi", "Climatisation", "Terrasse"],
                ["WiFi", "Piscine", "Cheminée"]
            ]
            defaultImages = [
                "https://images.unsplash.com/photo-1522706596130-f2038933b934?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1554995207-c18c5445d06b?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1507147361864-70678d781b49?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1505691938895-1758d5bb2d7c?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1502005229762-cf17864e4289?w=800&h=600&fit=crop&q=80"
            ]
        case "hostel":
            names = [
                "Auberge de jeunesse \(destination) Centre",
                "The Backpackers Hostel \(destination)",
                "Auberge Économique \(destination)",
                "Hostel World \(destination)",
                "Auberge Urbaine \(destination)"
            ]
            amenitiesList = [
                ["WiFi gratuit", "Salle commune", "Cuisine partagée"],
                ["WiFi gratuit", "Petit-déjeuner inclus", "Casier sécurisé"],
                ["WiFi gratuit", "Réception 24h/24", "Visites organisées"],
                ["WiFi gratuit", "Laverie", "Cartes de la ville"],
                ["WiFi gratuit", "Bar", "Événements sociaux"]
            ]
            defaultImages = [
                "https://images.unsplash.com/photo-1541728343110-3375865a7d65?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1549488349-f815049969f1?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1601006502759-dd55e5330a84?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                "https://plus.unsplash.com/photos/b_xO19wPqDk?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1502444330042-d1a293a77385?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
            ]
        case "resort":
            names = [
                "\(destination) Luxury Resort & Spa",
                "The Grand \(destination) Resort",
                "\(destination) Beachfront Resort",
                "All-Inclusive \(destination) Resort",
                "Mountain View Resort \(destination)"
            ]
            amenitiesList = [
                ["Piscine", "Spa", "Restaurant gastronomique"],
                ["Plage privée", "Activités nautiques", "Kids club"],
                ["Golf", "Tennis", "Centre de fitness"],
                ["Bar", "Divertissements", "Transfert aéroport"],
                ["Randonnée", "Vue panoramique", "Jacuzzi extérieur"]
            ]
            defaultImages = [
                "https://images.unsplash.com/photo-1571896349842-33c89424de2d?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1563911302983-4437882c7f55?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1544157833-80f074d0d62d?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1596392927891-b3b0d5c4b1b3?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1574639433434-c764e43e72dc?q=80&w=800&h=600&fit=crop&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
            ]
        case "apartment":
            names = [
                "Appartement moderne à \(destination)",
                "Loft spacieux au cœur de \(destination)",
                "Studio élégant avec vue",
                "Appartement familial près des attractions",
                "Résidence urbaine à \(destination)"
            ]
            amenitiesList = [
                ["WiFi", "Cuisine équipée", "Lave-linge"],
                ["WiFi", "Balcon", "Parking souterrain"],
                ["WiFi", "Climatisation", "Smart TV"],
                ["WiFi", "Deux chambres", "Adapté aux enfants"],
                ["WiFi", "Terrasse sur le toit", "Sécurité 24h/24"]
            ]
            defaultImages = [
                "https://images.unsplash.com/photo-1502672260266-de6074212a45?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1538600078204-aa2662c5b0b4?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1512918728675-edcdab23c7fd?w=800&h=600&fit=crop&q=80",
                "https://images.unsplash.com/photo-1540518614846-7eded433c457?w=800&h=600&fit=crop&q=80"
            ]
        default:
            names = [
                "\(destination) Hébergement Standard",
                "\(destination) Logement Confort",
                "\(destination) Résidence Basique"
            ]
            amenitiesList = [
                ["WiFi"],
                ["Parking"],
                ["TV"]
            ]
            defaultImages = [
                "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&h=600&fit=crop&q=80"
            ]
        }
        
        return (0..<5).map { index in
            Accommodation(
                id: "\(type)_\(index)",
                name: names[index % names.count],
                type: type,
                price: Double.random(in: 50...300),
                currency: "EUR",
                rating: Double.random(in: 3.5...5.0),
                imageUrl: defaultImages[index % defaultImages.count],
                location: locations[index % locations.count],
                address: nil,
                cityCode: nil,
                description: nil,
                photos: [defaultImages[index % defaultImages.count]],
                reviews: [],
                amenities: amenitiesList[index % amenitiesList.count],
                contact: nil,
                latitude: nil,
                longitude: nil,
                checkInTime: nil,
                checkOutTime: nil,
                roomType: nil,
                boardType: nil,
                guestRating: nil,
                userRatingsTotal: nil,
                pricePerNight: nil,
                totalPrice: nil
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
                // Hotel image
                hotelImageView
                    .frame(height: 180)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                
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
    
    /// Image de l'hôtel avec chargement depuis l'API
    private var hotelImageView: some View {
        Group {
            // Prioriser imageUrl, sinon utiliser la première photo
            let imageUrl = accommodation.imageUrl ?? accommodation.photos.first
            
            if let imageUrl = imageUrl, !imageUrl.isEmpty, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .transition(.opacity.combined(with: .scale(scale: 0.95)))
                    case .failure(_):
                        // Placeholder en cas d'erreur de chargement
                        placeholderView
                    case .empty:
                        // Pendant le chargement
                        loadingView
                    @unknown default:
                        placeholderView
                    }
                }
                .animation(.easeInOut(duration: 0.3), value: imageUrl)
            } else {
                // Pas d'URL d'image disponible
                placeholderView
            }
        }
    }
    
    private var placeholderView: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(ThemeColors.surface(colorScheme))
            .overlay(
                VStack(spacing: 8) {
                    Image(systemName: "photo.fill")
                        .font(.system(size: 48))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    Text("Image non disponible")
                        .font(.caption)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
            )
    }
    
    private var loadingView: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(ThemeColors.surface(colorScheme))
            .overlay(
                ProgressView()
                    .tint(ThemeColors.accent())
            )
    }
}

#Preview {
    AccommodationsListView(
        destinationId: "test",
        destination: nil,
        accommodationType: "hotel"
    )
}

