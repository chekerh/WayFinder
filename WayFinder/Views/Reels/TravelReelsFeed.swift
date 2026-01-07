import SwiftUI

/// Embedded preview section for reels feed in HomeScreen
struct TravelReelsFeed: View {
    @StateObject private var viewModel = ReelsViewModel()
    @StateObject private var discussionViewModel = DiscussionViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Binding var navigateToReels: Bool
    @State private var showCreateReelSheet = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Section header
            HStack {
                Text("reels_discover_travels")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                
                Spacer()
                
                Button(action: {
                    navigateToReels = true
                }) {
                    Text("reels_see_all")
                        .font(.body)
                        .foregroundStyle(Color(red: 0.098, green: 0.463, blue: 0.824))
                }
            }
            .padding(.horizontal, 24)
            
            switch viewModel.uiState {
            case .loading:
                // Loading skeleton
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(0..<3, id: \.self) { _ in
                            ReelPreviewCardSkeleton()
                        }
                    }
                    .padding(.horizontal, 24)
                }
            case .success(let items, _):
                if items.isEmpty {
                    // Empty state
                    VStack(spacing: 16) {
                        Text("reels_no_content")
                            .font(.body)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 200)
                    .padding(.horizontal, 24)
                } else {
                    // Show preview cards (first 4 items) with Create Reel card as first item
                    let previewItems = Array(items.prefix(4))
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            // Create Reel Card (first item)
                            CreateReelCard(onTap: {
                                showCreateReelSheet = true
                            })
                            
                            // Other reel preview cards
                            ForEach(previewItems) { item in
                                ReelPreviewCard(
                                    item: item,
                                    onClick: {
                                        navigateToReels = true
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 24)
                    }
                }
            case .error(let message):
                VStack(spacing: 16) {
                    Text(message)
                        .font(.body)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        .multilineTextAlignment(.center)
                    
                    Button(action: {
                        viewModel.retry()
                    }) {
                        Text("reels_retry")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(ThemeColors.accent())
                            .clipShape(Capsule())
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 200)
                .padding(.horizontal, 24)
            case .idle:
                EmptyView()
            }
            
            // Community Discussions Section (underneath reels)
            Spacer()
                .frame(height: 24)
            
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("home_community_discussions")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    Spacer()
                    
                    Button(action: {
                        // Navigate to full discussions view
                    }) {
                        Text("reels_see_all")
                            .font(.body)
                            .foregroundStyle(Color(red: 0.098, green: 0.463, blue: 0.824))
                    }
                }
                .padding(.horizontal, 24)
                
                // Discussion posts preview
                DiscussionPostsPreview(viewModel: discussionViewModel)
                    .padding(.horizontal, 24)
            }
        }
        .task {
            await viewModel.loadReelsFeed(refresh: true)
            await discussionViewModel.loadPosts()
        }
        .sheet(isPresented: $showCreateReelSheet) {
            CreateReelSheet(
                onReelCreated: {
                    showCreateReelSheet = false
                    // Refresh reels feed
                    Task {
                        await viewModel.loadReelsFeed(refresh: true)
                    }
                },
                onDismiss: {
                    showCreateReelSheet = false
                }
            )
        }
    }
}

struct ReelPreviewCard: View {
    let item: ReelContentItem
    let onClick: () -> Void
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        Button(action: onClick) {
            ZStack(alignment: .bottomLeading) {
                // Thumbnail/Image
                if let thumbnailUrl = item.thumbnailUrl, !thumbnailUrl.isEmpty {
                    AsyncImage(url: buildImageURL(from: thumbnailUrl)) { phase in
                        switch phase {
                        case .empty:
                            ProgressView()
                                .frame(width: 160, height: 240)
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        case .failure:
                            gradientPlaceholder
                        @unknown default:
                            gradientPlaceholder
                        }
                    }
                    .frame(width: 160, height: 240)
                    .clipped()
                } else {
                    gradientPlaceholder
                        .frame(width: 160, height: 240)
                }
                
                // Gradient overlay
                LinearGradient(
                    colors: [
                        Color.clear,
                        Color.black.opacity(0.6)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(width: 160, height: 240)
                
                // Video play icon
                if item.isVideo {
                    Image(systemName: "play.circle.fill")
                        .font(.system(size: 40))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                
                // Bottom content
                VStack(alignment: .leading, spacing: 4) {
                    // Creator info
                    HStack(spacing: 6) {
                        if let avatarUrl = item.creatorAvatar, !avatarUrl.isEmpty, let url = buildImageURL(from: avatarUrl) {
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .empty:
                                    Circle()
                                        .fill(Color.white.opacity(0.3))
                                        .frame(width: 20, height: 20)
                                case .success(let image):
                                    image
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                case .failure:
                                    Circle()
                                        .fill(Color.white.opacity(0.3))
                                        .frame(width: 20, height: 20)
                                @unknown default:
                                    Circle()
                                        .fill(Color.white.opacity(0.3))
                                        .frame(width: 20, height: 20)
                                }
                            }
                            .frame(width: 20, height: 20)
                            .clipShape(Circle())
                        }
                        
                        Text(item.creatorName)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.white)
                            .lineLimit(1)
                    }
                    
                    // Engagement metrics
                    HStack(spacing: 12) {
                        HStack(spacing: 4) {
                            Image(systemName: "heart.fill")
                                .font(.system(size: 12))
                                .foregroundColor(.white)
                            Text(formatCount(item.likesCount))
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.9))
                        }
                        HStack(spacing: 4) {
                            Image(systemName: "eye.fill")
                                .font(.system(size: 12))
                                .foregroundColor(.white)
                            Text(formatCount(item.viewsCount))
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.9))
                        }
                    }
                }
                .padding(12)
            }
        }
        .buttonStyle(.plain)
        .frame(width: 160, height: 240)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 4, x: 0, y: 2)
    }
    
    private var gradientPlaceholder: some View {
        LinearGradient(
            colors: [
                Color(red: 0.098, green: 0.463, blue: 0.824),
                Color(red: 0.259, green: 0.647, blue: 0.980)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
    
    private func buildImageURL(from urlString: String) -> URL? {
        if urlString.hasPrefix("http://") || urlString.hasPrefix("https://") {
            return URL(string: urlString)
        }
        let baseURL = "https://wayfinder-api-w92x.onrender.com"
        // Ensure urlString starts with / if it doesn't already
        let path = urlString.hasPrefix("/") ? urlString : "/\(urlString)"
        return URL(string: "\(baseURL)\(path)")
    }
    
    private func formatCount(_ count: Int) -> String {
        if count >= 1_000_000 {
            let value = Float(count) / 1_000_000.0
            return String(format: value.truncatingRemainder(dividingBy: 1) == 0 ? "%.0fM" : "%.1fM", value)
        } else if count >= 1_000 {
            let value = Float(count) / 1_000.0
            return String(format: value.truncatingRemainder(dividingBy: 1) == 0 ? "%.0fK" : "%.1fK", value)
        } else {
            return "\(count)"
        }
    }
}

struct ReelPreviewCardSkeleton: View {
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        RoundedRectangle(cornerRadius: 16)
            .fill(ThemeColors.surface(colorScheme))
            .frame(width: 160, height: 240)
            .overlay {
                ProgressView()
            }
    }
}

struct DiscussionPostsPreview: View {
    @ObservedObject var viewModel: DiscussionViewModel
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding()
            } else if let error = viewModel.errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .padding()
            } else if viewModel.posts.isEmpty {
                Text("No discussions yet")
                    .font(.caption)
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    .frame(maxWidth: .infinity)
                    .padding()
            } else {
                ForEach(viewModel.posts.prefix(3)) { post in
                    CompactDiscussionPostCard(post: post)
                }
            }
        }
    }
}

struct CompactDiscussionPostCard: View {
    let post: DiscussionPost
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                if let avatarUrl = post.user.profileImageUrl, !avatarUrl.isEmpty {
                    AsyncImage(url: buildImageURL(from: avatarUrl)) { phase in
                        switch phase {
                        case .empty:
                            Circle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(width: 32, height: 32)
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        case .failure:
                            Circle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(width: 32, height: 32)
                        @unknown default:
                            Circle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(width: 32, height: 32)
                        }
                    }
                    .frame(width: 32, height: 32)
                    .clipShape(Circle())
                }
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(post.user.username ?? "User")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    if let destination = post.destination {
                        Text(destination)
                            .font(.system(size: 12))
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                }
                
                Spacer()
            }
            
            Text(post.content)
                .font(.system(size: 14))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                .lineLimit(2)
            
            HStack(spacing: 16) {
                HStack(spacing: 4) {
                    Image(systemName: "heart")
                        .font(.system(size: 12))
                    Text("\(post.likesCount)")
                        .font(.system(size: 12))
                }
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                
                HStack(spacing: 4) {
                    Image(systemName: "bubble.right")
                        .font(.system(size: 12))
                    Text("\(post.commentsCount)")
                        .font(.system(size: 12))
                }
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            }
        }
        .padding(12)
        .background(ThemeColors.surface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
    
    private func buildImageURL(from urlString: String) -> URL? {
        if urlString.hasPrefix("http://") || urlString.hasPrefix("https://") {
            return URL(string: urlString)
        }
        let baseURL = "https://wayfinder-api-w92x.onrender.com"
        let path = urlString.hasPrefix("/") ? urlString : "/\(urlString)"
        return URL(string: "\(baseURL)\(path)")
    }
}

