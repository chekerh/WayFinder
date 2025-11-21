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
    
    init(destinationId: String, destination: FlightDestination? = nil) {
        self.destinationId = destinationId
        self.destination = destination
    }
    
    var body: some View {
        ZStack {
            Color(red: 0.918, green: 0.949, blue: 1.0) // #EAF2FF
                .ignoresSafeArea()
            
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.errorMessage {
                VStack(spacing: 16) {
                    Text("Erreur")
                        .font(.headline)
                    Text(error)
                        .font(.subheadline)
                        .multilineTextAlignment(.center)
                    Button("Réessayer") {
                        Task {
                            await viewModel.loadDestination(id: destinationId)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()
            } else if let destination = destination ?? loadedDestination ?? viewModel.destination {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 24) {
                        // Destination Image Header
                        ZStack(alignment: .topLeading) {
                            Group {
                                if let imageUrl = destination.imageUrl, let url = URL(string: imageUrl) {
                                    AsyncImage(url: url) { phase in
                                        switch phase {
                                        case .success(let image):
                                            image
                                                .resizable()
                                                .scaledToFill()
                                        case .failure, .empty:
                                            Rectangle()
                                                .fill(
                                                    LinearGradient(
                                                        colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)],
                                                        startPoint: .topLeading,
                                                        endPoint: .bottomTrailing
                                                    )
                                                )
                                        @unknown default:
                                            Rectangle()
                                                .fill(
                                                    LinearGradient(
                                                        colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)],
                                                        startPoint: .topLeading,
                                                        endPoint: .bottomTrailing
                                                    )
                                                )
                                        }
                                    }
                                } else {
                                    Rectangle()
                                        .fill(
                                            LinearGradient(
                                                colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                        )
                                }
                            }
                            .frame(height: 280)
                            .clipShape(RoundedRectangle(cornerRadius: 20))
                            
                            // Back button
                            Button(action: { dismiss() }) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(.white)
                                    .frame(width: 44, height: 44)
                                    .background(Color.black.opacity(0.35))
                                    .clipShape(Circle())
                            }
                            .padding(16)
                            .buttonStyle(.plain)
                        }
                        .padding(.top, -24)
                        
                        // Flight Information Card
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Informations du vol")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            // Departure and Arrival
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Départ")
                                        .font(.caption)
                                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                    
                                    if let departureDate = destination.departureDate {
                                        Text(formatDate(departureDate, format: "date"))
                                            .font(.system(size: 18, weight: .semibold))
                                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                        
                                        Text(formatDate(departureDate, format: "time"))
                                            .font(.system(size: 16))
                                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                    } else {
                                        Text("N/A")
                                            .font(.system(size: 18, weight: .semibold))
                                    }
                                }
                                
                                Spacer()
                                
                                Image(systemName: "airplane")
                                    .font(.system(size: 32))
                                    .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                                
                                Spacer()
                                
                                VStack(alignment: .trailing, spacing: 4) {
                                    Text("Arrivée")
                                        .font(.caption)
                                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                    
                                    if let arrivalDate = destination.arrivalDate {
                                        Text(formatDate(arrivalDate, format: "date"))
                                            .font(.system(size: 18, weight: .semibold))
                                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                        
                                        Text(formatDate(arrivalDate, format: "time"))
                                            .font(.system(size: 16))
                                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                    } else {
                                        Text("N/A")
                                            .font(.system(size: 18, weight: .semibold))
                                    }
                                }
                            }
                            
                            Divider()
                            
                            // Airline and Duration
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Compagnie aérienne")
                                        .font(.caption)
                                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                    Text(destination.airline ?? "N/A")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                }
                                
                                Spacer()
                                
                                VStack(alignment: .trailing, spacing: 4) {
                                    Text("Durée")
                                        .font(.caption)
                                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                    Text(calculateDuration(departureDate: destination.departureDate, arrivalDate: destination.arrivalDate))
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                }
                            }
                        }
                        .padding(20)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color.white)
                        )
                        .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
                        
                        // About Card
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 8) {
                                Image(systemName: "location.fill")
                                    .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                                Text("À propos")
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                            
                            Text("Flight to \(destination.city) via \(destination.airline ?? "N/A")")
                                .font(.body)
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                .lineSpacing(6)
                        }
                        .padding(20)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color.white)
                        )
                        .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
                        
                        // Travel Tips Card
                        VStack(alignment: .leading, spacing: 16) {
                            HStack {
                                HStack(spacing: 8) {
                                    Image(systemName: "lightbulb.fill")
                                        .foregroundColor(Color(red: 1.0, green: 0.757, blue: 0.027))
                                    Text("Conseils de voyage")
                                        .font(.system(size: 20, weight: .bold))
                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                }
                                
                                Spacer()
                                
                                NavigationLink(destination: EmptyView()) {
                                    Text("Voir tout →")
                                        .font(.body)
                                        .foregroundStyle(Color(red: 0.098, green: 0.463, blue: 0.824))
                                }
                                .buttonStyle(.plain)
                            }
                            
                            VStack(spacing: 12) {
                                Text("Impossible de charger les conseils")
                                    .font(.body)
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                
                                Button(action: {
                                    // TODO: Generate travel tips
                                }) {
                                    Text("Générer les conseils")
                                        .font(.body)
                                        .foregroundStyle(Color(red: 0.098, green: 0.463, blue: 0.824))
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(20)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color.white)
                        )
                        .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
                        
                        // Reserve Button
                        NavigationLink(destination: ReservationScreen(destinationId: destination.id, destination: destination)) {
                            Text("Réserver maintenant")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(Color(red: 0.098, green: 0.463, blue: 0.824))
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                        .buttonStyle(.plain)
                        .padding(.top, 8)
                        .padding(.bottom, 32)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 24)
                    .padding(.bottom, 24)
                }
                .safeAreaPadding(.horizontal)
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

