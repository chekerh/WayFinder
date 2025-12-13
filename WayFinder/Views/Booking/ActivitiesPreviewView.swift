//
//  ActivitiesPreviewView.swift
//  WayFinder
//
//  Activities preview screen before final booking, matching Android ActivitiesPreviewScreen
//

import SwiftUI

struct ActivitiesPreviewView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ActivitiesPreviewViewModel()
    
    let destination: FlightDestination?
    let accommodation: Accommodation?
    let onConfirmBooking: () -> Void
    let onExploreActivities: () -> Void
    
    private var destinationName: String {
        destination?.name ?? accommodation?.location ?? "Destination"
    }
    
    private var destinationCity: String {
        destination?.city ?? accommodation?.location?.components(separatedBy: ",").first ?? "Paris"
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                }
                
                Spacer()
                
                Text("Activités à \(destinationName)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                    .lineLimit(1)
                
                Spacer()
                
                Color.clear.frame(width: 20, height: 20)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(ThemeColors.surface(colorScheme))
            
            ScrollView {
                VStack(spacing: 24) {
                    Text("Découvrez ce que vous pouvez faire à \(destinationName)!")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                        .multilineTextAlignment(.center)
                        .padding(.top, 16)
                    
                    // Activities carousel
                    if viewModel.isLoading {
                        ProgressView()
                            .frame(height: 250)
                    } else if let error = viewModel.errorMessage {
                        Text("Erreur: \(error)")
                            .foregroundColor(.red)
                            .padding()
                    } else if viewModel.activities.isEmpty {
                        Text("Aucune activité trouvée pour le moment.")
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .padding()
                    } else {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 16) {
                                ForEach(viewModel.activities, id: \.xid) { activity in
                                    ActivityPreviewCard(activity: activity)
                                }
                            }
                            .padding(.horizontal, 24)
                        }
                    }
                    
                    // Arrive Mode teaser
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 8) {
                            Text("✨")
                                .font(.system(size: 24))
                            Text("Mode Arrive")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(Color.purple)
                        }
                        
                        Text("Dès votre arrivée, WayFinder activera des fonctionnalités exclusives:")
                            .font(.system(size: 14))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        
                        VStack(alignment: .leading, spacing: 8) {
                            ArriveModeFeatureRow(text: "🗺️ Navigation AR en temps réel")
                            ArriveModeFeatureRow(text: "🎯 Recommandations personnalisées")
                            ArriveModeFeatureRow(text: "🤖 Assistant IA vocal")
                            ArriveModeFeatureRow(text: "📸 Organisation automatique de photos")
                        }
                    }
                    .padding(20)
                    .background(Color.purple.opacity(0.1))
                    .cornerRadius(20)
                    .padding(.horizontal, 24)
                    
                    Spacer().frame(height: 20)
                }
            }
            
            // Action buttons
            VStack(spacing: 12) {
                Button(action: onConfirmBooking) {
                    Text("Confirmer la réservation")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(ThemeColors.primary(colorScheme))
                        .cornerRadius(16)
                }
                
                Button(action: onExploreActivities) {
                    Text("Explorer toutes les activités")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(ThemeColors.primary(colorScheme))
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.clear)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(ThemeColors.primary(colorScheme), lineWidth: 2)
                        )
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
            .background(ThemeColors.surface(colorScheme))
        }
        .background(ThemeColors.background(colorScheme))
        .navigationBarHidden(true)
        .task {
            await viewModel.loadActivities(city: destinationCity)
        }
    }
}

struct ActivityPreviewCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let activity: OpenTripMapPlace
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Image
            AsyncImage(url: URL(string: activity.preview?.source ?? "")) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                case .failure, .empty:
                    LinearGradient(
                        colors: [.blue.opacity(0.6), .purple.opacity(0.6)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                @unknown default:
                    Color.gray.opacity(0.3)
                }
            }
            .frame(width: 200, height: 120)
            .clipped()
            .cornerRadius(16, corners: [.topLeft, .topRight])
            
            // Content
            VStack(alignment: .leading, spacing: 8) {
                Text(activity.name)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                    .lineLimit(1)
                
                if let kinds = activity.kinds {
                    HStack(spacing: 4) {
                        Image(systemName: "tag.fill")
                            .font(.system(size: 12))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        Text(kinds.components(separatedBy: ",").first?.capitalized ?? "")
                            .font(.system(size: 12))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .lineLimit(1)
                    }
                }
                
                if let rate = activity.rate {
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.yellow)
                        Text("\(rate)")
                            .font(.system(size: 12))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                }
            }
            .padding(12)
        }
        .frame(width: 200, height: 220)
        .background(ThemeColors.surface(colorScheme))
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.1), radius: 8, y: 4)
    }
}

struct ArriveModeFeatureRow: View {
    let text: String
    
    var body: some View {
        Text(text)
            .font(.system(size: 13))
            .foregroundColor(.gray)
    }
}

// Custom corner radius modifier
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners
    
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

// ViewModel
@MainActor
class ActivitiesPreviewViewModel: ObservableObject {
    @Published var activities: [OpenTripMapPlace] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    func loadActivities(city: String, limit: Int = 5) async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Use the catalog service to fetch activities
            let response = try await CatalogService.shared.getActivities(city: city, limit: limit)
            activities = response.data
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
}

#Preview {
    ActivitiesPreviewView(
        destination: FlightDestination(
            id: "1",
            name: "Paris",
            city: "Paris",
            country: "France",
            imageUrl: nil,
            price: 350,
            currency: "EUR",
            description: nil,
            departureDate: nil,
            arrivalDate: nil,
            airline: nil
        ),
        accommodation: nil,
        onConfirmBooking: {},
        onExploreActivities: {}
    )
}

