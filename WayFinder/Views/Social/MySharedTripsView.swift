import SwiftUI

@MainActor
struct MySharedTripsView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = MySharedTripsViewModel()
    
    var body: some View {
        NavigationView {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                if viewModel.isLoading && viewModel.sharedTrips.isEmpty {
                    ProgressView()
                        .scaleEffect(1.5)
                } else if viewModel.sharedTrips.isEmpty && !viewModel.isLoading {
                    emptyState
                } else {
                    List {
                        ForEach(viewModel.sharedTrips) { trip in
                            SharedTripCard(trip: trip, onDelete: {
                                Task {
                                    await viewModel.deleteTrip(trip.id)
                                }
                            })
                            .listRowInsets(EdgeInsets(top: 8, leading: 20, bottom: 8, trailing: 20))
                            .listRowBackground(Color.clear)
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                Button(role: .destructive) {
                                    Task {
                                        await viewModel.deleteTrip(trip.id)
                                    }
                                } label: {
                                    Label("Supprimer", systemImage: "trash")
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
                
                if let error = viewModel.errorMessage {
                    VStack {
                        Spacer()
                        Text(error)
                            .foregroundColor(.red)
                            .padding()
                            .background(ThemeColors.surface(colorScheme))
                            .cornerRadius(12)
                            .padding()
                    }
                }
            }
            .navigationTitle(String(localized: "profile_my_shared_trips"))
            .navigationBarTitleDisplayMode(.inline)
            .task {
                await viewModel.loadSharedTrips()
            }
            .refreshable {
                await viewModel.loadSharedTrips()
            }
            .onAppear {
                // Recharger quand on revient sur l'écran (après avoir partagé un voyage par exemple)
                Task {
                    await viewModel.loadSharedTrips()
                }
            }
        }
    }
    
    private var emptyState: some View {
        VStack(spacing: 20) {
            Image(systemName: "map.fill")
                .font(.system(size: 64))
                .foregroundColor(ThemeColors.accent().opacity(0.6))
            
            Text(String(localized: "my_shared_trips_empty_title"))
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(ThemeColors.primaryText(colorScheme))
            
            Text(String(localized: "my_shared_trips_empty_message"))
                .font(.body)
                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .padding(32)
    }
}

@MainActor
final class MySharedTripsViewModel: ObservableObject {
    @Published var sharedTrips: [SharedTrip] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let socialService = SocialService.shared
    private let userService = UserService.shared
    
    func loadSharedTrips() async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Récupérer le profil pour obtenir l'ID utilisateur
            let profile = try await userService.fetchProfile()
            
            // Récupérer l'ID de l'utilisateur connecté
            guard let userId = profile.id else {
                errorMessage = "Utilisateur non connecté"
                isLoading = false
                print("❌ [MySharedTripsViewModel] No user ID in profile")
                return
            }
            
            print("✅ [MySharedTripsViewModel] Loading shared trips for user: \(userId)")
            let trips = try await socialService.getUserSharedTrips(userId: userId)
            sharedTrips = trips
            print("✅ [MySharedTripsViewModel] Loaded \(trips.count) shared trips")
            if trips.isEmpty {
                print("⚠️ [MySharedTripsViewModel] No trips found. This may be normal if no trips have been shared yet.")
            }
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [MySharedTripsViewModel] Error loading shared trips: \(error.localizedDescription)")
        }
        
        isLoading = false
    }
    
    func deleteTrip(_ tripId: String) async {
        do {
            try await socialService.deleteSharedTrip(id: tripId)
            // Retirer le voyage de la liste localement
            sharedTrips.removeAll { $0.id == tripId }
            print("✅ [MySharedTripsViewModel] Deleted trip: \(tripId)")
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [MySharedTripsViewModel] Error deleting trip: \(error.localizedDescription)")
        }
    }
}

struct SharedTripCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let trip: SharedTrip
    let onDelete: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Images - Affichage amélioré
            if !trip.images.isEmpty {
                if let firstImageUrl = trip.images.first, let url = URL(string: firstImageUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                        case .failure, .empty:
                            RoundedRectangle(cornerRadius: 12)
                                .fill(ThemeColors.surface(colorScheme))
                                .overlay(
                                    Image(systemName: "photo")
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                        .font(.system(size: 40))
                                )
                        @unknown default:
                            RoundedRectangle(cornerRadius: 12)
                                .fill(ThemeColors.surface(colorScheme))
                        }
                    }
                    .frame(height: 200)
                    .frame(maxWidth: .infinity)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            
            // Title
            Text(trip.title)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(ThemeColors.primaryText(colorScheme))
            
            // Description
            if let description = trip.description, !description.isEmpty {
                Text(description)
                    .font(.system(size: 14))
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    .lineLimit(3)
            }
            
            // Tags
            if !trip.tags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(trip.tags, id: \.self) { tag in
                            Text("#\(tag)")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(ThemeColors.accent())
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(
                                    Capsule()
                                        .fill(ThemeColors.accent().opacity(0.1))
                                )
                        }
                    }
                }
            }
            
            // Stats
            HStack(spacing: 20) {
                HStack(spacing: 6) {
                    Image(systemName: trip.likesCount > 0 ? "heart.fill" : "heart")
                        .foregroundColor(trip.likesCount > 0 ? .red : ThemeColors.secondaryText(colorScheme))
                        .font(.system(size: 16))
                    Text("\(trip.likesCount)")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                }
                
                HStack(spacing: 6) {
                    Image(systemName: trip.commentsCount > 0 ? "bubble.left.and.bubble.right.fill" : "bubble.left.and.bubble.right")
                        .foregroundColor(trip.commentsCount > 0 ? ThemeColors.accent() : ThemeColors.secondaryText(colorScheme))
                        .font(.system(size: 16))
                    Text("\(trip.commentsCount)")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                }
                
                Spacer()
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(ThemeColors.surface(colorScheme))
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 8, x: 0, y: 4)
        )
    }
}

