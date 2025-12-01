//
//  FlightDetailScreen.swift
//  WayFinder
//
//  Created by sarrachmek on 19/11/2025.
//

import SwiftUI

struct FlightDetailScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = FlightDetailViewModel()
    let destinationId: String
    let destination: FlightDestination?
    @State private var loadedDestination: FlightDestination?
    @State private var showReservation = false
    @State private var toastMessage: String?
    @State private var toastType: ToastView.ToastType = .error
    
    init(destinationId: String, destination: FlightDestination? = nil) {
        self.destinationId = destinationId
        self.destination = destination
    }
    
    var body: some View {
        GeometryReader { geometry in
            // Responsive header height & overlap so it fits on all iPhone sizes
            let headerHeight = min(max(geometry.size.height * 0.35, 260), 380)
            let cardOverlap = min(headerHeight * 0.18, 60)
            
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = viewModel.errorMessage {
                    // Error shown as toast overlay, not inline
                    VStack(spacing: 16) {
                        Button(String(localized: "generic_retry")) {
                            Task {
                                await viewModel.loadDestination(id: destinationId)
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding()
                    .onAppear {
                        toastType = .error
                        toastMessage = error
                    }
                } else if let destination = destination ?? loadedDestination ?? viewModel.destination {
                    // Scrollable content - full screen width and height with safe area respect
                    ScrollView(showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 0) {
                            // Header Image with navigation buttons overlay - Full screen at top
                            ZStack(alignment: .top) {
                                // Image - takes full width including safe areas
                                if let imageUrl = destination.imageUrl, !imageUrl.isEmpty, let url = URL(string: imageUrl) {
                                    AsyncImage(url: url) { phase in
                                        switch phase {
                                        case .empty:
                                            LinearGradient(
                                                colors: [Color(red: 0x4A/255.0, green: 0x90/255.0, blue: 0xE2/255.0), Color(red: 0x2E/255.0, green: 0x5C/255.0, blue: 0x8A/255.0)],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                            .frame(height: headerHeight)
                                        case .success(let image):
                                            image
                                                .resizable()
                                                .scaledToFill()
                                                .frame(height: headerHeight)
                                                .clipped()
                                        case .failure:
                                            LinearGradient(
                                                colors: [Color(red: 0x4A/255.0, green: 0x90/255.0, blue: 0xE2/255.0), Color(red: 0x2E/255.0, green: 0x5C/255.0, blue: 0x8A/255.0)],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                            .frame(height: headerHeight)
                                        @unknown default:
                                            LinearGradient(
                                                colors: [Color(red: 0x4A/255.0, green: 0x90/255.0, blue: 0xE2/255.0), Color(red: 0x2E/255.0, green: 0x5C/255.0, blue: 0x8A/255.0)],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                            .frame(height: headerHeight)
                                        }
                                    }
                                } else {
                                    // Gradient fallback si pas d'image
                                    LinearGradient(
                                        colors: [Color(red: 0x4A/255.0, green: 0x90/255.0, blue: 0xE2/255.0), Color(red: 0x2E/255.0, green: 0x5C/255.0, blue: 0x8A/255.0)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                    .frame(height: headerHeight)
                                }
                                
                                // Navigation buttons overlay on image (en haut comme Android) - respect safe area
                                HStack {
                                    Button(action: { dismiss() }) {
                                        Image(systemName: "chevron.left")
                                            .font(.system(size: 20, weight: .semibold))
                                            .foregroundColor(.white)
                                            .frame(width: 48, height: 48)
                                            .background(Color.black.opacity(0.3))
                                            .clipShape(Circle())
                                    }
                                    .buttonStyle(.plain)
                                    
                                    Spacer()
                                    
                                    HStack(spacing: 12) {
                                        Button(action: {
                                            // Share action
                                        }) {
                                            Image(systemName: "square.and.arrow.up")
                                                .font(.system(size: 20, weight: .semibold))
                                                .foregroundColor(.white)
                                                .frame(width: 48, height: 48)
                                                .background(Color.black.opacity(0.3))
                                                .clipShape(Circle())
                                        }
                                        .buttonStyle(.plain)
                                        
                                        Button(action: {
                                            // Favorite action
                                        }) {
                                            Image(systemName: "heart")
                                                .font(.system(size: 20, weight: .semibold))
                                                .foregroundColor(.white)
                                                .frame(width: 48, height: 48)
                                                .background(Color.black.opacity(0.3))
                                                .clipShape(Circle())
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.top, geometry.safeAreaInsets.top + 8)
                                .padding(.leading, geometry.safeAreaInsets.leading + 16)
                                .padding(.trailing, geometry.safeAreaInsets.trailing + 16)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: headerHeight)
                            .ignoresSafeArea(edges: .horizontal)
                                
                                // Content Card with rounded top corners (overlaps image)
                                VStack(alignment: .leading, spacing: 0) {
                                    // Destination Title, Country, Price
                                    Text(destination.name)
                                        .font(.system(size: 28, weight: .bold))
                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                        .padding(.top, 16)
                                        .padding(.horizontal, 20)
                                    
                                    if !destination.country.isEmpty {
                                        Text(destination.country)
                                            .font(.system(size: 18))
                                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                            .padding(.top, 4)
                                            .padding(.horizontal, 20)
                                    }
                                    
                                    if let price = destination.price, price > 0 {
                                        Text("\(Int(price)) \(destination.currency)")
                                            .font(.system(size: 24, weight: .bold))
                                            .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                                            .padding(.top, 8)
                                            .padding(.horizontal, 20)
                                    }
                                    
                                    Spacer()
                                        .frame(height: 16)
                                    
                                    // Description du pays - TOUJOURS affichée (comme Android)
                                    Text(getDescriptionText(for: destination))
                                        .font(.system(size: 16))
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                        .lineSpacing(8)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .multilineTextAlignment(.leading)
                                        .padding(.vertical, 8)
                                        .padding(.horizontal, 20)
                                        .fixedSize(horizontal: false, vertical: true)
                                    
                                    // Spacing between description and amenities
                                    Spacer()
                                        .frame(height: 24)
                                    
                                    // Équipements disponibles - avec bordure grise (full width)
                                    VStack(alignment: .leading, spacing: 0) {
                                        Text("flight_amenities_title")
                                            .font(.system(size: 18, weight: .bold))
                                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                        
                                        Spacer()
                                            .frame(height: 24)
                                        
                                        HStack {
                                            Spacer()
                                            FeatureItem(name: String(localized: "amenity_sunny"), icon: "sun.max.fill")
                                            Spacer()
                                            FeatureItem(name: String(localized: "amenity_restaurant"), icon: "fork.knife")
                                            Spacer()
                                            FeatureItem(name: String(localized: "amenity_wifi"), icon: "wifi")
                                            Spacer()
                                            FeatureItem(name: String(localized: "amenity_cafe"), icon: "cup.and.saucer.fill")
                                            Spacer()
                                            FeatureItem(name: String(localized: "amenity_business"), icon: "building.2.fill")
                                            Spacer()
                                        }
                                    }
                                    .padding(20)
                                    .frame(maxWidth: .infinity)
                                    .background(
                                        RoundedRectangle(cornerRadius: 16)
                                            .fill(ThemeColors.surface(colorScheme))
                                    )
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 16)
                                            .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                                    )
                                    
                                    // Spacing between amenities and flight info
                                    Spacer()
                                        .frame(height: 24)
                                    
                                    // Flight Information - White card with gray border (full width)
                                    VStack(alignment: .leading, spacing: 16) {
                                        Text("flight_info_title")
                                            .font(.system(size: 20, weight: .bold))
                                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                        
                                        // Departure and Arrival - Dates
                                        HStack {
                                            VStack(alignment: .leading, spacing: 4) {
                                                Text("flight_departure")
                                                    .font(.system(size: 14))
                                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                                
                                                if let departureDate = destination.departureDate {
                                                    Text(formatDate(departureDate, format: "date"))
                                                        .font(.system(size: 18, weight: .semibold))
                                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                                        .lineLimit(1)
                                                        .minimumScaleFactor(0.8)
                                                } else {
                                                    Text("generic_not_available")
                                                        .font(.system(size: 18, weight: .semibold))
                                                }
                                            }
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            
                                            Image(systemName: "airplane")
                                                .font(.system(size: 32))
                                                .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                                                .frame(width: 40)
                                            
                                            VStack(alignment: .trailing, spacing: 4) {
                                                Text("flight_arrival")
                                                    .font(.system(size: 14))
                                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                                
                                                if let arrivalDate = destination.arrivalDate {
                                                    Text(formatDate(arrivalDate, format: "date"))
                                                        .font(.system(size: 18, weight: .semibold))
                                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                                        .lineLimit(1)
                                                        .minimumScaleFactor(0.8)
                                                } else {
                                                    Text("generic_not_available")
                                                        .font(.system(size: 18, weight: .semibold))
                                                }
                                            }
                                            .frame(maxWidth: .infinity, alignment: .trailing)
                                        }
                                        
                                        // Times (heures) - directly below dates
                                        HStack {
                                            if let departureDate = destination.departureDate {
                                                Text(formatDate(departureDate, format: "time"))
                                                    .font(.system(size: 16))
                                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                                    .frame(maxWidth: .infinity, alignment: .leading)
                                            } else {
                                                Spacer()
                                            }
                                            
                                            Spacer()
                                                .frame(width: 40)
                                            
                                            if let arrivalDate = destination.arrivalDate {
                                                Text(formatDate(arrivalDate, format: "time"))
                                                    .font(.system(size: 16))
                                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                                    .frame(maxWidth: .infinity, alignment: .trailing)
                                            } else {
                                                Spacer()
                                            }
                                        }
                                        .padding(.top, 4)
                                        
                                        // Airline and Duration - directly below times
                                        HStack {
                                            VStack(alignment: .leading, spacing: 4) {
                                                Text("flight_airline")
                                                    .font(.system(size: 14))
                                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                                Text(destination.airline ?? "N/A")
                                                    .font(.system(size: 16, weight: .medium))
                                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                                    .lineLimit(1)
                                            }
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            
                                            VStack(alignment: .trailing, spacing: 4) {
                                                Text("flight_duration")
                                                    .font(.system(size: 14))
                                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                                Text(calculateDuration(departureDate: destination.departureDate, arrivalDate: destination.arrivalDate))
                                                    .font(.system(size: 16, weight: .medium))
                                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                                    .lineLimit(1)
                                                    .minimumScaleFactor(0.8)
                                            }
                                            .frame(maxWidth: .infinity, alignment: .trailing)
                                        }
                                        .padding(.top, 4)
                                    }
                                    .padding(20)
                                    .frame(maxWidth: .infinity)
                                    .background(
                                        RoundedRectangle(cornerRadius: 16)
                                            .fill(ThemeColors.surface(colorScheme))
                                    )
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 16)
                                            .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                                    )
                                    
                                    // Spacing before reviews
                                    Spacer()
                                        .frame(height: 24)
                                    
                                    // Reviews Section
                                    ReviewsSection(itemType: "flight", itemId: destination.id)
                                        .padding(.horizontal, 20)
                                    
                                    // Spacing before button
                                    Spacer()
                                        .frame(height: 24)
                                    
                                    // Book button - inside ScrollView (scrolls with content)
                                    NavigationLink(destination: ReservationScreen(
                                        destinationId: destination.id,
                                        destination: destination,
                                        onBackToHome: {
                                            // Fermer FlightDetailScreen pour revenir à Home
                                            // Utiliser un petit délai pour s'assurer que ReservationScreen est fermé d'abord
                                            Task { @MainActor in
                                                try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 secondes
                                                // Fermer FlightDetailScreen et revenir à HomeScreen
                                                dismiss()
                                            }
                                        }
                                    )) {
                                        Text("flight_book")
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundColor(.black)
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 56)
                                            .background(Color(red: 1.0, green: 0.757, blue: 0.027))
                                            .clipShape(RoundedRectangle(cornerRadius: 16))
                                            .shadow(color: Color(red: 1.0, green: 0.757, blue: 0.027).opacity(0.3), radius: 8, x: 0, y: 4)
                                    }
                                    .buttonStyle(.plain)
                                    .padding(.horizontal, 20)
                                    .padding(.bottom, geometry.safeAreaInsets.bottom + 20)
                                }
                                .padding(.top, 0)
                                .background(
                                    RoundedRectangle(cornerRadius: 32, style: .continuous)
                                        .fill(ThemeColors.background(colorScheme))
                                )
                                .offset(y: -cardOverlap) // Overlap image slightly, responsive
                        }
                        .background(ThemeColors.background(colorScheme))
                    }
                    .padding(.leading, geometry.safeAreaInsets.leading)
                    .padding(.trailing, geometry.safeAreaInsets.trailing)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .navigationBarHidden(true)
            .task {
                if destination == nil && loadedDestination == nil {
                    await viewModel.loadDestination(id: destinationId)
                    loadedDestination = viewModel.destination
                }
            }
            .onAppear {
                if let destination = destination {
                    loadedDestination = destination
                }
            }
            .toast(message: $toastMessage, type: $toastType)
        }
    }
    
    private func formatDate(_ dateString: String, format: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = formatter.date(from: dateString) ?? ISO8601DateFormatter().date(from: dateString) {
            let displayFormatter = DateFormatter()
            if format == "date" {
                displayFormatter.dateFormat = "yyyy-MM-dd"
            } else {
                displayFormatter.dateFormat = "HH:mm"
            }
            return displayFormatter.string(from: date)
        }
        
        // Fallback: try to parse manually
        if let tIndex = dateString.firstIndex(of: "T") {
            if format == "date" {
                return String(dateString[..<tIndex])
            } else {
                let timePart = String(dateString[dateString.index(after: tIndex)...])
                let timeComponents = timePart.components(separatedBy: ":")
                if timeComponents.count >= 2 {
                    return "\(timeComponents[0]):\(timeComponents[1])"
                }
            }
        }
        
        return dateString
    }
    
    private func calculateDuration(departureDate: String?, arrivalDate: String?) -> String {
        guard let departureStr = departureDate,
              let arrivalStr = arrivalDate else {
            return "~4h 30min"
        }
        
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        guard let departure = formatter.date(from: departureStr) ?? ISO8601DateFormatter().date(from: departureStr),
              let arrival = formatter.date(from: arrivalStr) ?? ISO8601DateFormatter().date(from: arrivalStr) else {
            return "~4h 30min"
        }
        
        let duration = arrival.timeIntervalSince(departure)
        let hours = Int(duration / 3600)
        let minutes = Int((duration.truncatingRemainder(dividingBy: 3600)) / 60)
        
        if hours > 0 && minutes > 0 {
            return "~\(hours)h \(minutes)min"
        } else if hours > 0 {
            return "~\(hours)h"
        } else {
            return "~\(minutes)min"
        }
    }
    
    // Get description text (same logic as Android - TOUJOURS retourner une description)
    private func getDescriptionText(for destination: FlightDestination) -> String {
        // D'abord, essayer d'utiliser la description du backend
        // Mais ignorer si c'est juste "Flight to..." (ce n'est pas une vraie description du pays)
        if let description = destination.description, 
           !description.isEmpty,
           !description.lowercased().hasPrefix("flight to") {
            return description
        }
        
        // Si pas de description du backend valide, utiliser des descriptions par défaut basées sur le pays/ville
        // Utiliser Bundle.main.localizedString pour respecter la langue choisie (comme pour le greeting)
        let countryLower = destination.country.lowercased()
        let nameLower = destination.name.lowercased()
        let cityLower = destination.city.lowercased()
        
        if countryLower.contains("france") || nameLower.contains("paris") || cityLower.contains("paris") {
            return Bundle.main.localizedString(forKey: "description_paris", value: nil, table: nil)
        } else if countryLower.contains("italie") || countryLower.contains("italy") || nameLower.contains("rome") || cityLower.contains("rome") {
            return Bundle.main.localizedString(forKey: "description_rome", value: nil, table: nil)
        } else if countryLower.contains("espagne") || countryLower.contains("spain") || nameLower.contains("madrid") || nameLower.contains("barcelone") || cityLower.contains("madrid") || cityLower.contains("barcelone") {
            return Bundle.main.localizedString(forKey: "description_spain", value: nil, table: nil)
        } else {
            let defaultDescription = Bundle.main.localizedString(forKey: "description_default", value: nil, table: nil)
            return String(format: defaultDescription, destination.name)
        }
    }
}

// MARK: - FeatureItem
struct FeatureItem: View {
    let name: String
    let icon: String
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
            Text(name)
                .font(.system(size: 12))
                .foregroundStyle(Color.gray)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - GlassCard
struct GlassCard<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme
    let content: Content
    
    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
    
    var body: some View {
        ZStack {
            // Glass effect background (exactement comme Android: F5F5F5, E8E8E8, F5F5F5)
            RoundedRectangle(cornerRadius: 16)
                .fill(
                    LinearGradient(
                        colors: [
                            Color(red: 0xF5/255.0, green: 0xF5/255.0, blue: 0xF5/255.0).opacity(0.9),
                            Color(red: 0xE8/255.0, green: 0xE8/255.0, blue: 0xE8/255.0).opacity(0.85),
                            Color(red: 0xF5/255.0, green: 0xF5/255.0, blue: 0xF5/255.0).opacity(0.9)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            
            // Overlay gradient (exactement comme Android: FFFFFF avec alpha 0.6 et 0.5)
            RoundedRectangle(cornerRadius: 16)
                .fill(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(0.6),
                            Color.white.opacity(0.5)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            
            // Border gradient (exactement comme Android: CCCCCC, DDDDDD, CCCCCC)
            RoundedRectangle(cornerRadius: 16)
                .stroke(
                    LinearGradient(
                        colors: [
                            Color(red: 0xCC/255.0, green: 0xCC/255.0, blue: 0xCC/255.0).opacity(0.5),
                            Color(red: 0xDD/255.0, green: 0xDD/255.0, blue: 0xDD/255.0).opacity(0.4),
                            Color(red: 0xCC/255.0, green: 0xCC/255.0, blue: 0xCC/255.0).opacity(0.5)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1
                )
        }
        .overlay {
            content
        }
    }
}

// ViewModel pour charger les détails
@MainActor
class FlightDetailViewModel: ObservableObject {
    @Published var destination: FlightDestination?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    func loadDestination(id: String) async {
        isLoading = true
        errorMessage = nil
        
        // Pour l'instant, on utilise l'ID pour créer un placeholder
        // En production, vous devriez faire un appel API pour charger les détails
        // destination = try await catalogService.getDestinationDetails(id: id)
        
        isLoading = false
    }
}

// MARK: - Preview
#Preview {
    NavigationStack {
        FlightDetailScreen(
            destinationId: "1",
            destination: FlightDestination(
                id: "1",
                name: "Rome",
                city: "Rome",
                country: "Italy",
                imageUrl: "https://images.unsplash.com/photo-1529260830199-42c24126f198?w=800",
                price: 450.0,
                currency: "EUR",
                description: "Découvrez la ville éternelle",
                departureDate: "2025-12-03T07:55:00",
                arrivalDate: "2025-12-03T09:20:00",
                airline: "TU"
            )
        )
    }
}

