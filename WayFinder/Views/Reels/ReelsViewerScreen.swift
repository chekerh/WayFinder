import SwiftUI

struct ReelsViewerScreen: View {
    @StateObject private var viewModel = ReelsViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @State private var selectedItemForComments: ReelContentItem?
    @State private var showComments = false
    
    var initialIndex: Int = 0
    
    var body: some View {
        ZStack {
            Color.black
                .ignoresSafeArea()
            
            switch viewModel.uiState {
            case .loading, .idle:
                ProgressView()
                    .tint(.white)
            case .success(let items, _):
                if items.isEmpty {
                    EmptyReelsState(
                        onRetry: { viewModel.retry() },
                        onBack: { dismiss() }
                    )
                } else {
                    GeometryReader { geometry in
                        ScrollViewReader { proxy in
                            ScrollView(.vertical, showsIndicators: false) {
                                LazyVStack(spacing: 0) {
                                    ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                                        ReelItemView(
                                            item: item,
                                            currentUserId: viewModel.currentUserId,
                                            onLike: {
                                                Task {
                                                    await viewModel.likeItem(item)
                                                }
                                            },
                                            onComment: {
                                                selectedItemForComments = item
                                                showComments = true
                                            },
                                            onShare: {
                                                // Share functionality
                                                // TODO: Implement share
                                            },
                                            onProfileClick: {
                                                // Navigate to profile
                                                // TODO: Implement navigation
                                            }
                                        )
                                        .id(index)
                                        .frame(width: geometry.size.width, height: geometry.size.height)
                                    }
                                }
                            }
                            .scrollTargetBehavior(.paging)
                            .onAppear {
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                                    withAnimation {
                                        proxy.scrollTo(min(initialIndex, items.count - 1), anchor: .top)
                                    }
                                }
                            }
                        }
                    }
                }
            case .error(let message):
                ErrorReelsState(
                    message: message,
                    onRetry: { viewModel.retry() },
                    onBack: { dismiss() }
                )
            }
            
            // Top bar with back button
            VStack {
                HStack {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(8)
                            .background(Color.black.opacity(0.3))
                            .clipShape(Circle())
                    }
                    
                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                
                Spacer()
            }
        }
        .navigationBarHidden(true)
        .sheet(isPresented: $showComments) {
            if let item = selectedItemForComments {
                NavigationStack {
                    Group {
                        switch item {
                        case .postItem(let post):
                            PostCommentsView(
                                postId: post.id,
                                viewModel: DiscussionViewModel()
                            )
                        case .journeyItem(let journey):
                            JourneyCommentsSheet(
                                journey: journey,
                                onClose: { showComments = false },
                                onJourneyUpdated: { _ in }
                            )
                        }
                    }
                }
            }
        }
        .task {
            await viewModel.loadReelsFeed(refresh: true)
        }
    }
}

struct ReelItemView: View {
    let item: ReelContentItem
    let currentUserId: String?
    let onLike: () -> Void
    let onComment: () -> Void
    let onShare: () -> Void
    let onProfileClick: () -> Void
    
    @State private var isLiked: Bool = false
    @State private var showFullCaption: Bool = false
    @State private var likeScale: CGFloat = 1.0
    @Environment(\.colorScheme) private var colorScheme
    
    @ViewBuilder
    private var profileImageView: some View {
        Group {
            let avatarUrl = item.creatorAvatar
            let _ = print("🔍 [ReelItemView] Avatar URL from item: \(avatarUrl ?? "nil")")
            if let avatarUrl = avatarUrl, !avatarUrl.isEmpty {
                let url = buildImageURL(from: avatarUrl)
                let _ = print("🔍 [ReelItemView] Built URL: \(url?.absoluteString ?? "nil")")
                if let url = url {
                    let _ = print("🖼️ [ReelItemView] Loading avatar: \(avatarUrl) -> \(url.absoluteString)")
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            Circle()
                                .fill(Color.white.opacity(0.3))
                                .overlay {
                                    ProgressView()
                                        .tint(.white)
                                }
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        case .failure:
                            Circle()
                                .fill(Color.white.opacity(0.3))
                                .overlay {
                                    Image(systemName: "person")
                                        .foregroundColor(.white)
                                }
                        @unknown default:
                            Circle()
                                .fill(Color.white.opacity(0.3))
                                .overlay {
                                    Image(systemName: "person")
                                        .foregroundColor(.white)
                                }
                        }
                    }
                    .frame(width: 48, height: 48)
                    .clipShape(Circle())
                    .overlay {
                        Circle()
                            .stroke(Color.white, lineWidth: 2)
                    }
                    .onTapGesture {
                        onProfileClick()
                    }
                } else {
                    // URL construction failed
                    let _ = print("⚠️ [ReelItemView] Failed to build URL from: \(avatarUrl)")
                    Circle()
                        .fill(Color.white.opacity(0.3))
                        .frame(width: 48, height: 48)
                        .overlay {
                            Image(systemName: "person")
                                .foregroundColor(.white)
                        }
                        .overlay {
                            Circle()
                                .stroke(Color.white, lineWidth: 2)
                        }
                        .onTapGesture {
                            onProfileClick()
                        }
                }
            } else {
                // No avatar URL
                let _ = print("⚠️ [ReelItemView] No avatar URL for item: \(item.id)")
                Circle()
                    .fill(Color.white.opacity(0.3))
                    .frame(width: 48, height: 48)
                    .overlay {
                        Image(systemName: "person")
                            .foregroundColor(.white)
                    }
                    .overlay {
                        Circle()
                            .stroke(Color.white, lineWidth: 2)
                    }
                    .onTapGesture {
                        onProfileClick()
                    }
            }
        }
    }
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Main content (image/video)
                if let thumbnailUrl = item.thumbnailUrl, !thumbnailUrl.isEmpty {
                    AsyncImage(url: buildImageURL(from: thumbnailUrl)) { phase in
                        switch phase {
                        case .empty:
                            gradientPlaceholder
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
                    .frame(width: geometry.size.width, height: geometry.size.height)
                    .clipped()
                } else {
                    gradientPlaceholder
                        .frame(width: geometry.size.width, height: geometry.size.height)
                }
                
                // Video play overlay
                if item.isVideo {
                    ZStack {
                        Color.black.opacity(0.3)
                        
                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 64))
                            .foregroundColor(.white)
                    }
                }
                
                // Gradient overlay for text readability
                LinearGradient(
                    colors: [
                        Color.clear,
                        Color.black.opacity(0.4)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(width: geometry.size.width, height: geometry.size.height)
                
                // Right side engagement buttons
                VStack(spacing: 24) {
                    // Profile picture
                    profileImageView
                    
                    // Like button
                    VStack(spacing: 4) {
                        Button(action: {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                                likeScale = 1.2
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                                    likeScale = 1.0
                                }
                            }
                            // Update optimistically
                            isLiked.toggle()
                            onLike()
                        }) {
                            Image(systemName: isLiked ? "heart.fill" : "heart")
                                .font(.system(size: 24))
                                .foregroundColor(isLiked ? Color(red: 1.0, green: 0.09, blue: 0.267) : .white)
                                .frame(width: 48, height: 48)
                                .background(Color.black.opacity(0.3))
                                .clipShape(Circle())
                                .scaleEffect(likeScale)
                        }
                        
                        Text(formatCount(item.likesCount))
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                    }
                    
                    // Comment button
                    VStack(spacing: 4) {
                        Button(action: onComment) {
                            Image(systemName: "bubble.right")
                                .font(.system(size: 24))
                                .foregroundColor(.white)
                                .frame(width: 48, height: 48)
                                .background(Color.black.opacity(0.3))
                                .clipShape(Circle())
                        }
                        
                        Text(formatCount(item.commentsCount))
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                    }
                    
                    // Views button
                    VStack(spacing: 4) {
                        Image(systemName: "eye.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.white)
                            .frame(width: 48, height: 48)
                            .background(Color.black.opacity(0.3))
                            .clipShape(Circle())
                        
                        Text(formatCount(item.viewsCount))
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                    }
                    
                    // Share button
                    Button(action: onShare) {
                        Image(systemName: "arrowshape.turn.up.right")
                            .font(.system(size: 24))
                            .foregroundColor(.white)
                            .frame(width: 48, height: 48)
                            .background(Color.black.opacity(0.3))
                            .clipShape(Circle())
                    }
                }
                .padding(.trailing, 16)
                .padding(.bottom, 80)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .trailing)
                
                // Bottom content overlay
                VStack(alignment: .leading, spacing: 8) {
                    // Creator info
                    HStack(alignment: .center, spacing: 8) {
                        Text(item.creatorName)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                        
                        if let destination = item.destination {
                            Text(destination)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.white.opacity(0.2))
                                .clipShape(Capsule())
                        }
                    }
                    
                    // Caption
                    if !item.caption.isEmpty {
                        let displayCaption = showFullCaption || item.caption.count <= 100
                            ? item.caption
                            : String(item.caption.prefix(100)) + "..."
                        
                        Text(displayCaption)
                            .font(.system(size: 14))
                            .foregroundColor(.white)
                            .lineLimit(showFullCaption ? nil : 3)
                            .onTapGesture {
                                if item.caption.count > 100 {
                                    showFullCaption.toggle()
                                }
                            }
                        
                        if item.caption.count > 100 && !showFullCaption {
                            Text("reels_see_more")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(.white.opacity(0.8))
                                .onTapGesture {
                                    showFullCaption = true
                                }
                        }
                    }
                    
                    // Tags
                    if !item.tags.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 6) {
                                ForEach(Array(item.tags.prefix(3)), id: \.self) { tag in
                                    Text("#\(tag)")
                                        .font(.system(size: 12))
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.white.opacity(0.2))
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .frame(maxHeight: .infinity, alignment: .bottom)
            }
        }
        .simultaneousGesture(
            TapGesture(count: 2)
                .onEnded {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                        likeScale = 1.2
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                            likeScale = 1.0
                        }
                    }
                    isLiked = true
                    onLike()
                }
        )
        .onAppear {
            updateLikedState()
        }
        .onChange(of: item.id) { _, _ in
            updateLikedState()
        }
    }
    
    private func updateLikedState() {
        // Determine if liked
        switch item {
        case .postItem(let post):
            // Check if current user is in likedBy
            if let userId = currentUserId {
                isLiked = post.likedBy.contains(userId)
            } else {
                isLiked = false
            }
        case .journeyItem(let journey):
            isLiked = journey.isLiked
        }
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

struct EmptyReelsState: View {
    let onRetry: () -> Void
    let onBack: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "video.slash")
                .font(.system(size: 64))
                .foregroundColor(.white.opacity(0.6))
            
            Text("reels_no_content_available")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(.white)
            
            Text("reels_share_travels_message")
                .font(.system(size: 14))
                .foregroundColor(.white.opacity(0.7))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            Button(action: onRetry) {
                Text("reels_retry")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color(red: 0.098, green: 0.463, blue: 0.824))
                    .clipShape(Capsule())
            }
        }
    }
}

struct ErrorReelsState: View {
    let message: String
    let onRetry: () -> Void
    let onBack: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 64))
                .foregroundColor(.white.opacity(0.6))
            
            Text(message)
                .font(.system(size: 16))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            
            Button(action: onRetry) {
                Text("reels_retry")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color(red: 0.098, green: 0.463, blue: 0.824))
                    .clipShape(Capsule())
            }
        }
    }
}

