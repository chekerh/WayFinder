import SwiftUI
import AVKit
import UIKit

@MainActor
struct JourneyFeedView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var journeyViewModel = JourneyViewModel()
    @StateObject private var profileViewModel = ProfileViewModel()
    @StateObject private var destinationVideoViewModel = DestinationVideoViewModel()
    
    @State private var selectedJourney: Journey?
    @State private var showVideoPlayer = false
    @State private var videoUrlForPlayer: String?
    @State private var journeyForComments: Journey?
    @State private var showDeleteConfirmation = false
    @State private var journeyToDelete: Journey?
    
    private var currentUserId: String? {
        profileViewModel.userId
    }
    
    @ViewBuilder
    private var contentView: some View {
        if journeyViewModel.isLoading {
            loadingView
        } else if let error = journeyViewModel.errorMessage {
            errorView(error: error)
        } else if journeyViewModel.journeys.isEmpty {
            emptyView
        } else {
            journeysListView
        }
    }
    
    private var loadingView: some View {
        ProgressView()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    private func errorView(error: String) -> some View {
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
                    await journeyViewModel.loadJourneys()
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
    }
    
    private var emptyView: some View {
        VStack(spacing: 16) {
            Image(systemName: "square.and.arrow.up")
                .font(.system(size: 48))
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            Text("Aucun voyage partagé")
                .font(.headline)
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Text("Soyez le premier à partager votre voyage !")
                .font(.subheadline)
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    private var journeysListView: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                // Destination Videos Section (only for current user)
                if let currentUserId = currentUserId,
                   case .success(let destinations) = destinationVideoViewModel.uiState,
                   !destinations.isEmpty {
                    
                    Text(String(localized: "destination_videos_title"))
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                    
                    ForEach(destinations) { destination in
                        DestinationVideoCard(
                            destination: destination,
                            onGenerateClick: {
                                Task {
                                    await destinationVideoViewModel.generateVideo(userId: currentUserId, destination: destination.destination)
                                }
                            },
                            onVideoClick: {
                                if let videoUrl = destination.videoUrl {
                                    videoUrlForPlayer = videoUrl
                                    showVideoPlayer = true
                                }
                            }
                        )
                        .padding(.horizontal, 20)
                    }
                    
                    Divider()
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                    
                    Text(String(localized: "journey_all_journeys"))
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                }
                
                ForEach(journeyViewModel.journeys) { journey in
                    journeyCard(for: journey)
                        .id(journey.id)
                        .padding(.horizontal, 20)
                }
            }
            .padding(.vertical, 16)
            .padding(.bottom, 140)
        }
    }
    
    private func journeyCard(for journey: Journey) -> some View {
        JourneyCard(
            journey: journey,
            currentUserId: currentUserId,
            onLikeClick: {
                Task {
                    await journeyViewModel.likeJourney(journeyId: journey.id)
                }
            },
            onCommentClick: {
                journeyForComments = journey
            },
            onImageClick: {
                selectedJourney = journey
            },
            onVideoClick: {
                selectedJourney = journey
                showVideoPlayer = true
            },
            onGenerateVideoClick: {
                Task {
                    do {
                        try await journeyViewModel.regenerateVideo(journeyId: journey.id)
                        // Reload journeys to see updated video status (processing)
                        await journeyViewModel.loadJourneys()
                    } catch {
                        print("❌ [JourneyFeedView] Error generating video: \(error.localizedDescription)")
                    }
                }
            },
            onDeleteClick: {
                journeyToDelete = journey
                showDeleteConfirmation = true
            }
        )
    }
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            contentView
        }
        .navigationTitle("Voyages partagés")
        .navigationBarTitleDisplayMode(.large)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                }
            }
        }
            .alert("Supprimer le voyage", isPresented: $showDeleteConfirmation) {
                Button("Supprimer", role: .destructive) {
                    if let journey = journeyToDelete {
                        Task {
                            await deleteJourney(journey)
                        }
                    }
                }
                Button("Annuler", role: .cancel) { }
            } message: {
                Text("Êtes-vous sûr de vouloir supprimer ce voyage ? Cette action est irréversible.")
            }
            .fullScreenCover(isPresented: $showVideoPlayer) {
                if let videoUrl = videoUrlForPlayer ?? selectedJourney?.videoUrl,
                   let url = URL(string: videoUrl) {
                    VideoPlayer(player: AVPlayer(url: url))
                        .ignoresSafeArea()
                        .overlay(alignment: .topTrailing) {
                            Button("Fermer") {
                                showVideoPlayer = false
                                videoUrlForPlayer = nil
                            }
                            .padding()
                            .background(Color.black.opacity(0.5))
                            .foregroundColor(.white)
                            .clipShape(Capsule())
                        }
                }
            }
            .task {
                await journeyViewModel.loadJourneys()
                await profileViewModel.loadProfile()
                
                // Load destination videos for current user (after profile is loaded)
                if let userId = profileViewModel.userId {
                    await destinationVideoViewModel.loadUserDestinations(userId: userId)
                    
                    // Start polling for processing videos
                    await startPollingForProcessingVideos(userId: userId)
                }
            }
        .refreshable {
            await journeyViewModel.loadJourneys()
            if let userId = currentUserId {
                await destinationVideoViewModel.loadUserDestinations(userId: userId)
            }
        }
        .sheet(item: $journeyForComments) { journey in
            JourneyCommentsSheet(
                journey: journey,
                onClose: { journeyForComments = nil },
                onJourneyUpdated: { updatedJourney in
                    journeyViewModel.updateJourney(updatedJourney)
                }
            )
            .presentationDetents([.large])
        }
    }
    
    private func deleteJourney(_ journey: Journey) async {
        await journeyViewModel.deleteJourney(journeyId: journey.id)
        // Réinitialiser la référence pour fermer l'alerte
        journeyToDelete = nil
    }
    
    private func startPollingForProcessingVideos(userId: String) async {
        // Start polling task in background
        Task {
            while !Task.isCancelled {
                // Wait 5 seconds before checking
                try? await Task.sleep(nanoseconds: 5_000_000_000)
                
                // Check if there are any processing videos
                let destinations: [DestinationWithVideoStatus]
                if case .success(let dests) = destinationVideoViewModel.uiState {
                    destinations = dests
                } else {
                    break // Exit if state is not success
                }
                
                let processingDestinations = destinations.filter { $0.videoStatus == "processing" }
                
                // If no processing videos, stop polling
                if processingDestinations.isEmpty {
                    break
                }
                
                // Check status for each processing destination
                for destination in processingDestinations {
                    await destinationVideoViewModel.checkVideoStatus(userId: userId, destination: destination.destination)
                }
                
                // Reload destinations to get updated list
                await destinationVideoViewModel.loadUserDestinations(userId: userId)
            }
        }
    }
}

private struct JourneyCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let journey: Journey
    let currentUserId: String?
    let onLikeClick: () -> Void
    let onCommentClick: () -> Void
    let onImageClick: () -> Void
    let onVideoClick: () -> Void
    let onGenerateVideoClick: () -> Void
    let onDeleteClick: () -> Void
    
    // Remove fixed height - let images adapt dynamically
    
    private var isOwnJourney: Bool {
        currentUserId != nil && journey.userId == currentUserId
    }
    
    @ViewBuilder
    private var videoStatusView: some View {
        if isOwnJourney {
            // For own journeys: show video if completed, or button if pending/failed
            if journey.videoStatus == "completed", let videoUrl = journey.videoUrl, !videoUrl.isEmpty {
                videoCompletedButton
            } else if journey.videoStatus == "pending" || journey.videoStatus == "failed" {
                // Show "Generate Video" button for own journeys when video is not yet generated or failed
                generateVideoButton
            }
            // Do not show processing indicator - video will appear automatically when ready
        } else {
            // For other users' journeys: show video status only when video is fully ready
            if journey.videoStatus == "completed", let videoUrl = journey.videoUrl, !videoUrl.isEmpty {
                videoCompletedButton
            }
            // Do not show anything for processing, pending, or failed states
        }
    }
    
    private var videoCompletedButton: some View {
        Button(action: onVideoClick) {
            HStack(spacing: 8) {
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 20))
                Text("Vidéo AI générée")
                    .font(.system(size: 14, weight: .medium))
            }
            .foregroundColor(Color(red: 0.29, green: 0.56, blue: 0.89)) // Android: 0xFF4A90E2
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(red: 0.29, green: 0.56, blue: 0.89).opacity(0.1)) // Android: 0xFF4A90E2 with alpha 0.1
            )
        }
    }
    
    private var generateVideoButton: some View {
        Button(action: onGenerateVideoClick) {
            HStack(spacing: 8) {
                Image(systemName: "video.fill")
                    .font(.system(size: 18))
                Text(journey.videoStatus == "failed" ? "Régénérer la vidéo" : "Générer ma vidéo")
                    .font(.system(size: 15, weight: .semibold))
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(red: 0.29, green: 0.56, blue: 0.89)) // Android: 0xFF4A90E2
            )
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // User Header
            HStack {
                // Profile Image
                if let user = journey.user, let profileImageUrl = user.profileImageUrl {
                    AsyncImage(url: URL(string: profileImageUrl)) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                        case .failure, .empty:
                            Circle()
                                .fill(ThemeColors.surface(colorScheme))
                                .overlay(
                                    Image(systemName: "person.fill")
                                        .foregroundColor(ThemeColors.accent())
                                )
                        @unknown default:
                            Circle()
                                .fill(ThemeColors.surface(colorScheme))
                        }
                    }
                    .frame(width: 40, height: 40)
                    .clipShape(Circle())
                } else {
                    Circle()
                        .fill(ThemeColors.surface(colorScheme))
                        .frame(width: 40, height: 40)
                        .overlay(
                            Image(systemName: "person.fill")
                                .foregroundColor(ThemeColors.accent())
                        )
                }
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(journey.user?.username ?? "User")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                    // Afficher le nom complet de la destination (ex: "Barcelone, Espagne")
                    Text(DestinationHelper.getFullDestinationName(from: journey.destination))
                        .font(.system(size: 13))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                }
                
                Spacer()
                
                // Delete button (only for own journeys)
                if isOwnJourney {
                    Button(action: onDeleteClick) {
                        Image(systemName: "trash")
                            .font(.system(size: 16))
                            .foregroundColor(Color(red: 0.91, green: 0.12, blue: 0.39))
                    }
                }
            }
            
            // Description
            if let description = journey.description, !description.isEmpty {
                Text(description)
                    .font(.system(size: 15))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
            }
            
            // Images
            if !journey.imageUrls.isEmpty {
                let firstImageUrl = journey.imageUrls.first!
                let imageUrl = firstImageUrl.hasPrefix("http") ? firstImageUrl : "https://wayfinder-api-w92x.onrender.com\(firstImageUrl)"
                
                ZStack(alignment: .topTrailing) {
                    AsyncImage(url: URL(string: imageUrl)) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFit()
                                .aspectRatio(contentMode: .fit)
                        case .failure, .empty:
                            RoundedRectangle(cornerRadius: 12)
                                .fill(ThemeColors.surface(colorScheme))
                                .frame(height: 200)
                                .overlay(
                                    ProgressView()
                                )
                        @unknown default:
                            RoundedRectangle(cornerRadius: 12)
                                .fill(ThemeColors.surface(colorScheme))
                                .frame(height: 200)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(maxHeight: 500)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .onTapGesture {
                        onImageClick()
                    }
                    
                    // Image count badge
                    if journey.imageUrls.count > 1 {
                        Text("+\(journey.imageUrls.count - 1)")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(
                                Capsule()
                                    .fill(Color.black.opacity(0.6))
                            )
                            .padding(8)
                    }
                }
            }
            
            // Video Status / Generate Button
            videoStatusView
            
            // Tags
            if !journey.tags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(journey.tags.prefix(3), id: \.self) { tag in
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
            
            // Actions
            HStack(spacing: 24) {
                Button(action: onLikeClick) {
                    HStack(spacing: 6) {
                        Image(systemName: journey.isLiked ? "heart.fill" : "heart")
                            .font(.system(size: 18))
                            .foregroundColor(journey.isLiked ? Color(red: 0.91, green: 0.12, blue: 0.39) : ThemeColors.secondaryText(colorScheme))
                        Text("\(journey.likesCount)")
                            .font(.system(size: 15))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                }
                .buttonStyle(.plain)
                
                Button(action: onCommentClick) {
                    HStack(spacing: 6) {
                        Image(systemName: "bubble.right")
                            .font(.system(size: 18))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        Text("\(journey.commentsCount)")
                            .font(.system(size: 15))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(ThemeColors.surface(colorScheme))
        )
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 10, x: 0, y: 6)
    }
}

