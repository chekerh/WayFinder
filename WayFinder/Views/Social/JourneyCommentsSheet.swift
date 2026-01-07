import SwiftUI

struct JourneyCommentsSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel: JourneyCommentsViewModel
    let onClose: () -> Void
    let onJourneyUpdated: (Journey) -> Void
    
    init(journey: Journey, onClose: @escaping () -> Void, onJourneyUpdated: @escaping (Journey) -> Void) {
        _viewModel = StateObject(wrappedValue: JourneyCommentsViewModel(journey: journey))
        self.onClose = onClose
        self.onJourneyUpdated = onJourneyUpdated
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Capsule()
                    .fill(Color.gray.opacity(0.4))
                    .frame(width: 48, height: 5)
                    .padding(.top, 12)
                    .padding(.bottom, 8)
                
                ScrollView {
                    VStack(spacing: 20) {
                        journeySummaryCard
                        commentsSection
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
                }
                .background(ThemeColors.background(colorScheme))
                
                commentInputBar
                    .background(ThemeColors.surface(colorScheme))
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 16)
            }
            .background(ThemeColors.background(colorScheme).ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: close) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                }
            }
            .navigationTitle(Text("Commentaires"))
            .navigationBarTitleDisplayMode(.inline)
        }
        .onChange(of: viewModel.journey) { _, updatedJourney in
            onJourneyUpdated(updatedJourney)
        }
        .onDisappear {
            onClose()
        }
        .task {
            await viewModel.refresh()
        }
    }
    
    private var journeySummaryCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                JourneyAvatarView(
                    urlString: viewModel.journey.user?.profileImageUrl,
                    placeholderSeed: viewModel.journey.user?.id ?? viewModel.journey.userId
                )
                    .frame(width: 44, height: 44)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(viewModel.journey.user?.username ?? "User")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                    Text(DestinationHelper.getFullDestinationName(from: viewModel.journey.destination))
                        .font(.system(size: 13))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                }
                
                Spacer()
                
                Button(action: {
                    Task { await viewModel.toggleLike() }
                }) {
                    Image(systemName: viewModel.journey.isLiked ? "heart.fill" : "heart")
                        .foregroundColor(viewModel.journey.isLiked ? Color(red: 0.91, green: 0.12, blue: 0.39) : ThemeColors.secondaryText(colorScheme))
                }
            }
            
            if let description = viewModel.journey.description, !description.isEmpty {
                Text(description)
                    .font(.system(size: 15))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
            }
            
            if let firstImage = viewModel.journey.imageUrls.first {
                let resolved = firstImage.hasPrefix("http") ? firstImage : "https://wayfinder-api-w92x.onrender.com\(firstImage)"
                
                AsyncImage(url: URL(string: resolved)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    case .failure, .empty:
                        RoundedRectangle(cornerRadius: 16)
                            .fill(ThemeColors.surface(colorScheme))
                            .overlay(ProgressView())
                    @unknown default:
                        RoundedRectangle(cornerRadius: 16)
                            .fill(ThemeColors.surface(colorScheme))
                    }
                }
                .frame(height: min(320, UIScreen.main.bounds.width * 0.65))
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            
            HStack(spacing: 16) {
                Label("\(viewModel.journey.likesCount)", systemImage: "heart.fill")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                Label("\(viewModel.journey.commentsCount)", systemImage: "bubble.right.fill")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(ThemeColors.surface(colorScheme))
        )
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.08), radius: 16, x: 0, y: 8)
    }
    
    private var commentsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Commentaires (\(viewModel.comments.count))")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(ThemeColors.primaryText(colorScheme))
            
            if viewModel.isLoadingComments {
                ProgressView()
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 20)
            } else if let errorMessage = viewModel.errorMessage {
                VStack(spacing: 12) {
                    Text("Erreur lors du chargement des commentaires")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.red)
                    Text(errorMessage)
                        .font(.system(size: 13))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        .multilineTextAlignment(.center)
                    Button(action: {
                        Task { await viewModel.loadComments() }
                    }) {
                        Text("Réessayer")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(ThemeColors.accent())
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .center)
            } else if viewModel.comments.isEmpty {
                Text("Aucun commentaire pour le moment. Soyez le premier à réagir !")
                    .font(.system(size: 15))
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    .padding(.vertical, 12)
                    .frame(maxWidth: .infinity, alignment: .center)
            } else {
                VStack(spacing: 12) {
                    ForEach(viewModel.comments) { comment in
                        JourneyCommentRow(comment: comment)
                    }
                }
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(ThemeColors.surface(colorScheme))
        )
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.08), radius: 16, x: 0, y: 8)
    }
    
    private var commentInputBar: some View {
        HStack(spacing: 12) {
            TextField("Ajouter un commentaire...", text: $viewModel.commentText, axis: .vertical)
                .textFieldStyle(.plain)
                .foregroundColor(ThemeColors.primaryText(colorScheme))
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 24)
                        .fill(ThemeColors.surface(colorScheme))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(ThemeColors.border(colorScheme), lineWidth: 1)
                )
            
            Button(action: {
                Task { await viewModel.submitComment() }
            }) {
                Image(systemName: "paperplane.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(viewModel.commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? ThemeColors.secondaryText(colorScheme).opacity(0.3) : ThemeColors.accent())
                    .padding(12)
            }
            .disabled(viewModel.commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isSubmittingComment)
        }
    }
    
    private func close() {
        dismiss()
    }
}

private struct JourneyAvatarView: View {
    @Environment(\.colorScheme) private var colorScheme
    let urlString: String?
    let placeholderSeed: String?
    
    init(urlString: String?, placeholderSeed: String? = nil) {
        self.urlString = urlString
        self.placeholderSeed = placeholderSeed
    }
    
    var body: some View {
        if let url = resolvedURL {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                default:
                    placeholder
                }
            }
            .clipShape(Circle())
        } else {
            placeholder
        }
    }
    
    private var resolvedURL: URL? {
        guard let raw = urlString, !raw.isEmpty, raw != "null" else {
            // Si pas d'URL, utiliser un placeholder basé sur le seed
            return placeholderURL
        }
        
        // Nettoyer l'URL (supprimer les espaces, etc.)
        let cleaned = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // Si c'est déjà une URL complète, l'utiliser directement
        if cleaned.hasPrefix("http://") || cleaned.hasPrefix("https://") {
            return URL(string: cleaned)
        }
        
        // Si c'est une URL relative, ajouter le domaine
        // Supprimer le slash initial s'il existe pour éviter les doubles slashes
        let path = cleaned.hasPrefix("/") ? String(cleaned.dropFirst()) : cleaned
        let resolved = "https://wayfinder-api-w92x.onrender.com/\(path)"
        
        print("🖼️ [JourneyAvatarView] Resolving image URL: '\(raw)' -> '\(resolved)'")
        return URL(string: resolved)
    }
    
    private var placeholderURL: URL? {
        // Ne pas utiliser de placeholder random, retourner nil pour afficher le placeholder par défaut
        // Cela permet d'afficher l'icône person par défaut au lieu d'une image random
        return nil
    }
    
    private var placeholder: some View {
        Circle()
            .fill(ThemeColors.surface(colorScheme))
            .overlay(
                Image(systemName: "person.fill")
                    .foregroundColor(ThemeColors.accent())
            )
    }
}

private struct JourneyCommentRow: View {
    @Environment(\.colorScheme) private var colorScheme
    let comment: JourneyComment
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            JourneyAvatarView(
                urlString: comment.user?.profileImageUrl,
                placeholderSeed: comment.user?.id ?? comment.userId
            )
                .frame(width: 36, height: 36)
                .onAppear {
                    // Debug: Log pour voir l'URL de l'image
                    if let profileUrl = comment.user?.profileImageUrl {
                        print("🖼️ [JourneyCommentRow] Comment by \(comment.user?.username ?? "unknown") - Profile URL: \(profileUrl)")
                    } else {
                        print("⚠️ [JourneyCommentRow] Comment by \(comment.user?.username ?? "unknown") - No profile URL")
                    }
                }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(comment.displayName)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                Text(comment.content)
                    .font(.system(size: 14))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                if let createdAt = comment.createdAt, !createdAt.isEmpty {
                    Text(createdAt.relativeDateString())
                        .font(.system(size: 12))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(ThemeColors.surface(colorScheme).opacity(0.7))
        )
    }
}

private extension JourneyComment {
    var displayName: String {
        if let user = user {
            if let first = user.firstName, !first.isEmpty,
               let last = user.lastName, !last.isEmpty {
                return "\(first) \(last)"
            }
            if let first = user.firstName, !first.isEmpty {
                return first
            }
            if !user.username.isEmpty {
                return user.username
            }
        }
        return "Voyageur"
    }
}

private extension String {
    func relativeDateString() -> String {
        let fractionalFormatter = ISO8601DateFormatter()
        fractionalFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let standardFormatter = ISO8601DateFormatter()
        standardFormatter.formatOptions = [.withInternetDateTime]
        let parsedDate = fractionalFormatter.date(from: self) ?? standardFormatter.date(from: self)
        
        guard let date = parsedDate else {
            return self
        }
        
        let relativeFormatter = RelativeDateTimeFormatter()
        relativeFormatter.locale = Locale.current
        relativeFormatter.unitsStyle = .abbreviated
        return relativeFormatter.string(for: date) ?? self
    }
}

