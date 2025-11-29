import SwiftUI
import AVKit
import UIKit

@MainActor
struct JourneyFeedView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var journeyViewModel = JourneyViewModel()
    @StateObject private var profileViewModel = ProfileViewModel()
    
    @State private var selectedJourney: Journey?
    @State private var showVideoPlayer = false
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
                ForEach(journeyViewModel.journeys) { journey in
                    journeyCard(for: journey)
                        .id(journey.id)
                }
            }
            .padding(.horizontal, 20)
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
                if let journey = selectedJourney, let videoUrl = journey.videoUrl, let url = URL(string: videoUrl) {
                    VideoPlayer(player: AVPlayer(url: url))
                        .ignoresSafeArea()
                        .overlay(alignment: .topTrailing) {
                            Button("Fermer") {
                                showVideoPlayer = false
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
            }
        .refreshable {
            await journeyViewModel.loadJourneys()
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
}

private struct JourneyCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let journey: Journey
    let currentUserId: String?
    let onLikeClick: () -> Void
    let onCommentClick: () -> Void
    let onImageClick: () -> Void
    let onVideoClick: () -> Void
    let onDeleteClick: () -> Void
    
    // Remove fixed height - let images adapt dynamically
    
    private var isOwnJourney: Bool {
        currentUserId != nil && journey.userId == currentUserId
    }
    
    @ViewBuilder
    private var videoStatusView: some View {
        // Video status view removed - no longer showing video generation button
        if journey.videoStatus == "completed", journey.videoUrl != nil {
            videoCompletedButton
        } else if journey.videoStatus == "processing" {
            videoProcessingView
        }
        // Removed: generateVideoButton and related logic
    }
    
    private var videoCompletedButton: some View {
        Button(action: onVideoClick) {
            HStack {
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 20))
                Text("Vidéo AI générée")
                    .font(.system(size: 14, weight: .medium))
            }
            .foregroundColor(ThemeColors.accent())
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(ThemeColors.accent().opacity(0.1))
            )
        }
    }
    
    private var videoProcessingView: some View {
        HStack {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: Color(red: 1.0, green: 0.65, blue: 0.15)))
                .scaleEffect(0.8)
            Text("Génération de la vidéo en cours...")
                .font(.system(size: 14))
                .foregroundColor(Color(red: 1.0, green: 0.65, blue: 0.15))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(red: 1.0, green: 0.65, blue: 0.15).opacity(0.1))
        )
    }
    
    // Removed: generateVideoButton - no longer needed
    
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

