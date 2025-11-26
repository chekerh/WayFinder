import SwiftUI

struct DiscussionView: View {
    @StateObject private var viewModel = DiscussionViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @State private var showCreatePost = false
    @State private var selectedPostId: String? = nil
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.errorMessage {
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
                            await viewModel.loadPosts()
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
            } else if viewModel.posts.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "bubble.left.and.bubble.right")
                        .font(.system(size: 48))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    Text("Aucun post")
                        .font(.headline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("Les discussions apparaîtront ici")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ZStack(alignment: .bottomTrailing) {
                    VStack(spacing: 0) {
                        // Header
                        HStack {
                            Text("Discussions")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            Spacer()
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 20)
                        .padding(.bottom, 24)
                        
                        // Liste des posts
                        ScrollView {
                            VStack(spacing: 16) {
                                ForEach(viewModel.posts) { post in
                                    PostCard(post: post, viewModel: viewModel, onCommentTap: {
                                        selectedPostId = post.id
                                    })
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.bottom, 100) // Espace pour le FAB
                        }
                    }
                    
                    // Floating Action Button pour créer un nouveau post
                    Button(action: {
                        showCreatePost = true
                    }) {
                        Image(systemName: "plus")
                            .font(.system(size: 24, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(
                                Circle()
                                    .fill(Color(red: 0.098, green: 0.463, blue: 0.824))
                            )
                            .shadow(color: Color.black.opacity(0.2), radius: 8, x: 0, y: 4)
                    }
                    .padding(.trailing, 20)
                    .padding(.bottom, 20)
                }
                .overlay {
                    if showCreatePost {
                        CreatePostView(viewModel: viewModel, isPresented: $showCreatePost)
                            .zIndex(1000)
                    }
                }
                .sheet(item: Binding(
                    get: { selectedPostId.map { PostCommentItem(id: $0) } },
                    set: { selectedPostId = $0?.id }
                )) { item in
                    PostCommentsView(postId: item.id, viewModel: viewModel)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadPosts()
        }
        .refreshable {
            await viewModel.loadPosts()
        }
    }
}

struct PostCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let post: DiscussionPost
    @ObservedObject var viewModel: DiscussionViewModel
    let onCommentTap: () -> Void
    @State private var isLiked: Bool = false
    @State private var likesCount: Int
    
    init(post: DiscussionPost, viewModel: DiscussionViewModel, onCommentTap: @escaping () -> Void = {}) {
        self.post = post
        self.viewModel = viewModel
        self.onCommentTap = onCommentTap
        _likesCount = State(initialValue: post.likesCount)
    }
    
    private var formattedDate: String {
        guard let createdAt = post.createdAt else {
            // Si pas de date, retourner une chaîne vide
            return ""
        }
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM yyyy, HH:mm"
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: createdAt)
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // En-tête avec photo de profil, nom et date
            HStack(spacing: 10) {
                userAvatar
                    .frame(width: 40, height: 40)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(post.user.displayName)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    if !formattedDate.isEmpty {
                        Text(formattedDate)
                            .font(.caption)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                }
                
                Spacer()
            }
            
            // Titre du post
            Text(post.title)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            
            // Contenu du post
            Text(post.content)
                .font(.system(size: 14))
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                .lineLimit(nil)
            
            // Destination avec pin rouge
            if let destination = post.destination {
                HStack(spacing: 4) {
                    Image(systemName: "mappin.fill")
                        .foregroundColor(.red)
                        .font(.system(size: 12))
                    Text(destination)
                        .font(.caption)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
            }
            
            // Tags (optionnel)
            if !post.tags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(post.tags, id: \.self) { tag in
                            Text("#\(tag)")
                                .font(.caption)
                                .foregroundColor(ThemeColors.accent())
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(
                                    Capsule()
                                        .fill(ThemeColors.accent().opacity(0.1))
                                )
                        }
                    }
                }
            }
            
            // Likes et commentaires en bas
            HStack(spacing: 16) {
                Button(action: {
                    Task {
                        await toggleLike()
                    }
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: isLiked ? "heart.fill" : "heart")
                            .foregroundColor(isLiked ? .red : ThemeColors.secondaryText(colorScheme))
                            .font(.system(size: 14))
                        Text("\(likesCount)")
                            .font(.caption)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    }
                }
                .buttonStyle(.plain)
                
                Button(action: onCommentTap) {
                    HStack(spacing: 4) {
                        Image(systemName: "bubble.right")
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .font(.system(size: 14))
                        Text("\(post.commentsCount) commentaires")
                            .font(.caption)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                }
                .buttonStyle(.plain)
                
                Spacer()
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.white)
        )
        .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
    
    @ViewBuilder
    private var userAvatar: some View {
        if let imageUrl = post.user.profileImageUrl, let url = buildImageURL(from: imageUrl) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                        .clipShape(Circle())
                case .failure, .empty:
                    placeholder
                @unknown default:
                    placeholder
                }
            }
        } else {
            placeholder
        }
    }
    
    private var placeholder: some View {
        Circle()
            .fill(Color.gray.opacity(0.3))
            .overlay(
                Image(systemName: "person.fill")
                    .foregroundColor(.white)
            )
    }
    
    private func buildImageURL(from urlString: String) -> URL? {
        if urlString.hasPrefix("http://") || urlString.hasPrefix("https://") {
            return URL(string: urlString)
        }
        let baseURL = "https://wayfinder-api-w92x.onrender.com"
        return URL(string: "\(baseURL)\(urlString)")
    }
    
    private func toggleLike() async {
        await viewModel.likePost(id: post.id)
        // Mettre à jour l'état local
        if let updatedPost = viewModel.posts.first(where: { $0.id == post.id }) {
            // Vérifier si l'utilisateur actuel a liké (simplifié - devrait vérifier avec l'ID utilisateur réel)
            isLiked = !isLiked // Toggle simple pour l'instant
            likesCount = updatedPost.likesCount
        }
    }
}

// MARK: - PostCommentItem (pour le sheet)
struct PostCommentItem: Identifiable {
    let id: String
}

// MARK: - PostCommentsView (style Instagram)
struct PostCommentsView: View {
    @StateObject private var commentsViewModel = DiscussionViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    let postId: String
    @ObservedObject var viewModel: DiscussionViewModel
    
    @State private var commentText: String = ""
    @State private var isPostingComment = false
    @State private var currentPost: DiscussionPost?
    @State private var currentUserId: String?
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Commentaires")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.primary)
                
                Spacer()
                
                Button(action: {
                    dismiss()
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(.primary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 12)
            .background(Color.white)
            
            // Liste des commentaires (scrollable)
            if commentsViewModel.comments.isEmpty {
                VStack(spacing: 16) {
                    Spacer()
                    Text("Aucun commentaire pour le moment")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .frame(maxWidth: .infinity)
                .background(Color.white)
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 16) {
                        ForEach(commentsViewModel.comments) { comment in
                            SwipeableCommentRow(
                                comment: comment,
                                currentUserId: currentUserId,
                                onDelete: {
                                    Task {
                                        await commentsViewModel.deleteComment(id: comment.id)
                                        await commentsViewModel.loadComments(postId: postId)
                                        await viewModel.loadPosts()
                                    }
                                },
                                onLike: {
                                    Task {
                                        await commentsViewModel.likeComment(id: comment.id)
                                        // Recharger pour obtenir le compteur à jour
                                        await commentsViewModel.loadComments(postId: postId)
                                    }
                                }
                            )
                            .id(comment.id) // Forcer la mise à jour quand le commentaire change
                            .padding(.horizontal, 16)
                        }
                    }
                    .padding(.vertical, 12)
                }
                .background(Color.white)
            }
            
            // Champ de saisie style Instagram (toujours en bas, blanc jusqu'à la fin)
            VStack(spacing: 0) {
                Divider()
                HStack(spacing: 12) {
                    // Avatar de l'utilisateur
                    UserAvatarView(size: 32)
                    
                    // Champ de texte
                    HStack {
                        TextField("Ajoutez un commentaire...", text: $commentText)
                            .font(.system(size: 14))
                        
                        if !commentText.isEmpty {
                            Button(action: {
                                Task {
                                    await postComment()
                                }
                            }) {
                                Text("Publier")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                            }
                        } else {
                            Button(action: {
                                // Afficher le clavier emoji (optionnel)
                            }) {
                                Image(systemName: "face.smiling")
                                    .foregroundColor(.gray)
                                    .font(.system(size: 18))
                            }
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: 20)
                            .fill(Color.gray.opacity(0.1))
                    )
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 12)
                .background(Color.white)
                .safeAreaPadding(.bottom)
            }
        }
        .background(Color.white)
        .ignoresSafeArea(edges: .bottom)
        .task {
            await loadData()
        }
    }
    
    private func loadData() async {
        await commentsViewModel.loadPost(id: postId)
        await commentsViewModel.loadComments(postId: postId)
        currentPost = commentsViewModel.currentPost
        
        // Récupérer l'ID de l'utilisateur connecté
        do {
            let profile = try await UserService.shared.fetchProfile()
            currentUserId = profile.id
        } catch {
            print("⚠️ [PostCommentsView] Erreur lors de la récupération du profil utilisateur: \(error.localizedDescription)")
            currentUserId = nil
        }
    }
    
    private func postComment() async {
        guard !commentText.isEmpty else { return }
        
        isPostingComment = true
        await commentsViewModel.createComment(postId: postId, content: commentText)
        isPostingComment = false
        
        if commentsViewModel.errorMessage == nil {
            commentText = ""
            await commentsViewModel.loadComments(postId: postId)
            // Mettre à jour le compteur dans la vue principale
            await viewModel.loadPosts()
        }
    }
}

// MARK: - SwipeableCommentRow (avec swipe pour supprimer)
struct SwipeableCommentRow: View {
    let comment: DiscussionComment
    let currentUserId: String?
    let onDelete: () -> Void
    let onLike: () -> Void
    
    @State private var offset: CGFloat = 0
    @State private var isLiked: Bool = false
    @State private var likesCount: Int
    
    // Vérifier si l'utilisateur connecté est le propriétaire du commentaire
    private var canDelete: Bool {
        guard let currentUserId = currentUserId,
              let commentUserId = comment.user?.id else {
            return false
        }
        return currentUserId == commentUserId
    }
    
    init(comment: DiscussionComment, currentUserId: String?, onDelete: @escaping () -> Void, onLike: @escaping () -> Void) {
        self.comment = comment
        self.currentUserId = currentUserId
        self.onDelete = onDelete
        self.onLike = onLike
        _likesCount = State(initialValue: comment.likesCount)
    }
    
    // Mettre à jour le compteur quand le commentaire change
    private var currentLikesCount: Int {
        comment.likesCount
    }
    
    private var formattedDate: String {
        guard let createdAt = comment.createdAt else {
            return ""
        }
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .abbreviated
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.localizedString(for: createdAt, relativeTo: Date())
    }
    
    var body: some View {
        ZStack(alignment: .trailing) {
            // Bouton de suppression (visible uniquement quand on swipe ET si l'utilisateur peut supprimer)
            if canDelete {
                HStack {
                    Spacer()
                    Button(action: {
                        withAnimation(.spring()) {
                            offset = 0
                        }
                        // Attendre un peu avant de supprimer pour voir l'animation
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                            onDelete()
                        }
                    }) {
                        Image(systemName: "trash")
                            .foregroundColor(.white)
                            .font(.system(size: 16, weight: .medium))
                            .frame(width: 60)
                            .frame(maxHeight: .infinity)
                            .background(
                                Color.red
                                    .clipShape(Rectangle())
                            )
                    }
                    .buttonStyle(.plain)
                }
                .opacity(offset < -10 ? 1 : 0) // Visible seulement quand on swipe
                .allowsHitTesting(offset < -10) // Désactiver les interactions quand invisible
            }
            
            // Contenu du commentaire
            HStack(alignment: .top, spacing: 12) {
                // Avatar
                if let user = comment.user, let imageUrl = user.profileImageUrl, let url = buildImageURL(from: imageUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                                .frame(width: 32, height: 32)
                                .clipShape(Circle())
                        default:
                            Circle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(width: 32, height: 32)
                                .overlay(
                                    Image(systemName: "person.fill")
                                        .foregroundColor(.gray)
                                        .font(.system(size: 14))
                                )
                        }
                    }
                } else {
                    Circle()
                        .fill(Color.gray.opacity(0.3))
                        .frame(width: 32, height: 32)
                        .overlay(
                            Image(systemName: "person.fill")
                                .foregroundColor(.gray)
                                .font(.system(size: 14))
                        )
                }
                
                // Contenu du commentaire
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        if let user = comment.user {
                            Text(user.displayName)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.primary)
                        }
                        
                        Text(comment.content)
                            .font(.system(size: 14))
                            .foregroundColor(.primary)
                    }
                    
                    HStack(spacing: 16) {
                        if !formattedDate.isEmpty {
                            Text(formattedDate)
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                        
                        Button("Répondre") {
                            // TODO: Implémenter la réponse
                        }
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                        
                        if currentLikesCount > 0 {
                            HStack(spacing: 4) {
                                Image(systemName: "heart.fill")
                                    .foregroundColor(.red)
                                    .font(.system(size: 10))
                                Text("\(currentLikesCount)")
                                    .font(.system(size: 12))
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
                
                Spacer()
                
                // Bouton like du commentaire
                Button(action: {
                    withAnimation {
                        isLiked.toggle()
                    }
                    onLike()
                }) {
                    Image(systemName: isLiked ? "heart.fill" : "heart")
                        .foregroundColor(isLiked ? .red : .secondary)
                        .font(.system(size: 14))
                }
                .onChange(of: comment.likesCount) { oldValue, newValue in
                    // Mettre à jour le compteur quand le commentaire change
                    likesCount = newValue
                }
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 12)
            .background(Color(.systemBackground))
            .contentShape(Rectangle())
            .offset(x: offset)
            .gesture(
                // Permettre le swipe seulement si l'utilisateur peut supprimer
                canDelete ? DragGesture()
                    .onChanged { value in
                        if value.translation.width < 0 {
                            // Swipe vers la gauche
                            offset = max(value.translation.width, -60)
                        } else if offset < 0 {
                            // Permettre de revenir en arrière
                            offset = min(0, offset + value.translation.width)
                        }
                    }
                    .onEnded { value in
                        if value.translation.width < -30 || offset < -30 {
                            // Ouvrir le bouton de suppression
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                offset = -60
                            }
                        } else {
                            // Fermer le bouton de suppression
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                offset = 0
                            }
                        }
                    } : nil
            )
        }
        .clipped() // Empêcher le bouton de dépasser les bords
        .contentShape(Rectangle())
    }
    
    private func buildImageURL(from urlString: String) -> URL? {
        if urlString.hasPrefix("http://") || urlString.hasPrefix("https://") {
            return URL(string: urlString)
        }
        let baseURL = "https://wayfinder-api-w92x.onrender.com"
        return URL(string: "\(baseURL)\(urlString)")
    }
}

struct PostDetailView: View {
    @StateObject private var viewModel = DiscussionViewModel()
    @Environment(\.colorScheme) private var colorScheme
    let postId: String
    
    @State private var commentText: String = ""
    @State private var isPostingComment = false
    @State private var isLiked: Bool = false
    @State private var likesCount: Int = 0
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            if let post = viewModel.currentPost {
                VStack(spacing: 0) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            // Post content
                            VStack(alignment: .leading, spacing: 12) {
                                Text(post.title)
                                    .font(.system(size: 24, weight: .bold))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                
                                Text(post.content)
                                    .font(.system(size: 16))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                
                                if let destination = post.destination {
                                    HStack(spacing: 4) {
                                        Image(systemName: "mappin.fill")
                                            .foregroundColor(.red)
                                            .font(.system(size: 12))
                                        Text(destination)
                                            .font(.caption)
                                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                    }
                                }
                                
                                // Like button
                                Button(action: {
                                    Task {
                                        await toggleLike()
                                    }
                                }) {
                                    HStack(spacing: 4) {
                                        Image(systemName: isLiked ? "heart.fill" : "heart")
                                            .foregroundColor(isLiked ? .red : ThemeColors.secondaryText(colorScheme))
                                        Text("\(likesCount)")
                                            .font(.caption)
                                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                            .padding(20)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.white)
                            )
                            .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
                            
                            Divider()
                                .padding(.horizontal, 20)
                            
                            // Commentaires section
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Commentaires (\(viewModel.comments.count))")
                                    .font(.headline)
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                    .padding(.horizontal, 20)
                                
                                if viewModel.comments.isEmpty {
                                    Text("Aucun commentaire pour le moment")
                                        .font(.subheadline)
                                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                        .padding(.horizontal, 20)
                                        .padding(.vertical, 20)
                                } else {
                                    ForEach(viewModel.comments) { comment in
                                        CommentCard(comment: comment)
                                            .padding(.horizontal, 20)
                                    }
                                }
                            }
                            .padding(.top, 8)
                            .padding(.bottom, 100) // Espace pour le champ de commentaire
                        }
                    }
                    
                    // Champ de saisie de commentaire en bas
                    VStack(spacing: 0) {
                        Divider()
                        HStack(spacing: 12) {
                            TextField("Écrire un commentaire...", text: $commentText, axis: .vertical)
                                .textFieldStyle(.roundedBorder)
                                .lineLimit(1...4)
                            
                            Button(action: {
                                Task {
                                    await postComment()
                                }
                            }) {
                                Image(systemName: "paperplane.fill")
                                    .font(.system(size: 18))
                                    .foregroundColor(commentText.isEmpty ? .gray : Color(red: 0.098, green: 0.463, blue: 0.824))
                            }
                            .disabled(commentText.isEmpty || isPostingComment)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(Color.white)
                    }
                }
            } else {
                ProgressView()
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadPost(id: postId)
            await viewModel.loadComments(postId: postId)
            if let post = viewModel.currentPost {
                likesCount = post.likesCount
                // Vérifier si l'utilisateur a liké (simplifié)
                isLiked = false
            }
        }
    }
    
    private func toggleLike() async {
        await viewModel.likePost(id: postId)
        if let updatedPost = viewModel.currentPost {
            isLiked = !isLiked
            likesCount = updatedPost.likesCount
        }
    }
    
    private func postComment() async {
        guard !commentText.isEmpty else { return }
        
        isPostingComment = true
        await viewModel.createComment(postId: postId, content: commentText)
        isPostingComment = false
        
        if viewModel.errorMessage == nil {
            commentText = ""
            await viewModel.loadComments(postId: postId)
        }
    }
}

struct CommentCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let comment: DiscussionComment
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let user = comment.user {
                HStack(spacing: 8) {
                    if let imageUrl = user.profileImageUrl, let url = buildImageURL(from: imageUrl) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 28, height: 28)
                                    .clipShape(Circle())
                            default:
                                Circle()
                                    .fill(Color.gray.opacity(0.3))
                                    .frame(width: 28, height: 28)
                            }
                        }
                    } else {
                        Circle()
                            .fill(Color.gray.opacity(0.3))
                            .frame(width: 28, height: 28)
                    }
                    
                    Text(user.displayName)
                        .font(.caption)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    
                    Spacer()
                }
            }
            
            Text(comment.content)
                .font(.system(size: 14))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            
            HStack {
                HStack(spacing: 4) {
                    Image(systemName: "heart.fill")
                        .foregroundColor(.red)
                    Text("\(comment.likesCount)")
                }
                .font(.caption)
                
                Spacer()
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
        )
    }
    
    private func buildImageURL(from urlString: String) -> URL? {
        if urlString.hasPrefix("http://") || urlString.hasPrefix("https://") {
            return URL(string: urlString)
        }
        let baseURL = "https://wayfinder-api-w92x.onrender.com"
        return URL(string: "\(baseURL)\(urlString)")
    }
}

// MARK: - CreatePostView (Popup Style)
struct CreatePostView: View {
    @ObservedObject var viewModel: DiscussionViewModel
    @Binding var isPresented: Bool
    @Environment(\.colorScheme) private var colorScheme
    
    @State private var title: String = ""
    @State private var content: String = ""
    @State private var destination: String = ""
    @State private var isCreating = false
    @State private var errorMessage: String?
    @FocusState private var focusedField: Field?
    
    enum Field {
        case title, content, destination
    }
    
    var body: some View {
        ZStack {
            // Overlay semi-transparent avec blur
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .background(.ultraThinMaterial)
                .onTapGesture {
                    if !isCreating {
                        isPresented = false
                    }
                }
            
            // Popup content avec coins arrondis
            VStack(spacing: 0) {
                // Header amélioré avec coins arrondis en haut
                HStack(spacing: 12) {
                    Button(action: {
                        if !isCreating {
                            isPresented = false
                        }
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .frame(width: 32, height: 32)
                            .background(
                                Circle()
                                    .fill(ThemeColors.surface(colorScheme).opacity(0.6))
                            )
                    }
                    .disabled(isCreating)
                    
                    Spacer()
                    
                    Text("Nouveau post")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                    
                    Spacer()
                    
                    Button(action: {
                        Task {
                            await createPost()
                        }
                    }) {
                        if isCreating {
                            ProgressView()
                                .tint(.white)
                                .frame(width: 90, height: 38)
                        } else {
                            HStack(spacing: 6) {
                                Image(systemName: "paperplane.fill")
                                    .font(.system(size: 14, weight: .semibold))
                                Text("Publier")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .frame(width: 110, height: 38)
                            .background(
                                Capsule()
                                    .fill(title.isEmpty || content.isEmpty ? Color.gray.opacity(0.3) : Color(red: 0.098, green: 0.463, blue: 0.824))
                            )
                        }
                    }
                    .disabled(isCreating || title.isEmpty || content.isEmpty)
                }
                .padding(.horizontal, 24)
                .padding(.top, 28)
                .padding(.bottom, 22)
                .background(
                    UnevenRoundedRectangle(cornerRadii: .init(
                        topLeading: 32,
                        bottomLeading: 0,
                        bottomTrailing: 0,
                        topTrailing: 32
                    ))
                    .fill(ThemeColors.surface(colorScheme))
                )
                
                // Content amélioré
                ScrollView {
                    VStack(spacing: 20) {
                        // Titre avec icône
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 8) {
                                Image(systemName: "text.bubble.fill")
                                    .font(.system(size: 15))
                                    .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                                Text("Titre")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                            
                            TextField("Entrez le titre de votre post", text: $title)
                                .focused($focusedField, equals: .title)
                                .textFieldStyle(.plain)
                                .font(.system(size: 16))
                                .padding(18)
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(ThemeColors.surface(colorScheme))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 16)
                                                .stroke(focusedField == .title ? Color(red: 0.098, green: 0.463, blue: 0.824) : Color.clear, lineWidth: 2.5)
                                        )
                                )
                        }
                        .padding(.horizontal, 24)
                        .padding(.top, 20)
                        
                        // Contenu avec icône
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 8) {
                                Image(systemName: "doc.text.fill")
                                    .font(.system(size: 15))
                                    .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                                Text("Contenu")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                            
                            ZStack(alignment: .topLeading) {
                                if content.isEmpty {
                                    Text("Partagez vos expériences et conseils...")
                                        .font(.system(size: 16))
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme).opacity(0.5))
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 14)
                                }
                                
                                TextEditor(text: $content)
                                    .focused($focusedField, equals: .content)
                                    .font(.system(size: 16))
                                    .frame(minHeight: 150)
                                    .scrollContentBackground(.hidden)
                                    .padding(10)
                                    .background(
                                        RoundedRectangle(cornerRadius: 16)
                                            .fill(ThemeColors.surface(colorScheme))
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 16)
                                                    .stroke(focusedField == .content ? Color(red: 0.098, green: 0.463, blue: 0.824) : Color.clear, lineWidth: 2.5)
                                            )
                                    )
                            }
                        }
                        .padding(.horizontal, 24)
                        
                        // Destination (optionnel) avec icône
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 8) {
                                Image(systemName: "mappin.circle.fill")
                                    .font(.system(size: 15))
                                    .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824).opacity(0.7))
                                Text("Destination (optionnel)")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                            
                            TextField("Ex: Paris, Tokyo...", text: $destination)
                                .focused($focusedField, equals: .destination)
                                .textFieldStyle(.plain)
                                .font(.system(size: 16))
                                .padding(18)
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(ThemeColors.surface(colorScheme))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 16)
                                                .stroke(focusedField == .destination ? Color(red: 0.098, green: 0.463, blue: 0.824) : Color.clear, lineWidth: 2.5)
                                        )
                                )
                        }
                        .padding(.horizontal, 24)
                        
                        // Message d'erreur amélioré
                        if let errorMessage = errorMessage {
                            HStack(spacing: 8) {
                                Image(systemName: "exclamationmark.circle.fill")
                                    .font(.system(size: 14))
                                    .foregroundColor(.red)
                                Text(errorMessage)
                                    .font(.system(size: 14))
                                    .foregroundColor(.red)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.red.opacity(0.1))
                            )
                            .padding(.horizontal, 24)
                        }
                    }
                    .padding(.bottom, 32)
                }
                .background(ThemeColors.background(colorScheme))
            }
            .frame(maxWidth: 500)
            .frame(height: min(650, UIScreen.main.bounds.height * 0.8))
            .background(
                RoundedRectangle(cornerRadius: 32)
                    .fill(ThemeColors.background(colorScheme))
                    .shadow(color: Color.black.opacity(0.2), radius: 40, x: 0, y: 20)
            )
            .clipShape(RoundedRectangle(cornerRadius: 32))
            .padding(.horizontal, 16)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .transition(.opacity.combined(with: .scale(scale: 0.95)))
        .animation(.spring(response: 0.3, dampingFraction: 0.8), value: isPresented)
    }
    
    private func createPost() async {
        guard !title.isEmpty && !content.isEmpty else { return }
        
        isCreating = true
        errorMessage = nil
        
        await viewModel.createPost(
            title: title,
            content: content,
            tags: nil,
            destination: destination.isEmpty ? nil : destination,
            imageUrl: nil
        )
        
        isCreating = false
        
        if viewModel.errorMessage == nil {
            // Recharger les posts et fermer l'écran
            await viewModel.loadPosts()
            isPresented = false
        } else {
            errorMessage = viewModel.errorMessage
        }
    }
}

// MARK: - UserAvatarView
struct UserAvatarView: View {
    let size: CGFloat
    @StateObject private var profileImageService = ProfileImageService.shared
    
    var body: some View {
        Group {
            if let imageUrl = profileImageService.profileImageUrl, let url = buildImageURL(from: imageUrl) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(width: size, height: size)
                            .clipShape(Circle())
                    default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
    }
    
    private var placeholder: some View {
        Circle()
            .fill(Color.gray.opacity(0.3))
            .frame(width: size, height: size)
            .overlay(
                Image(systemName: "person.fill")
                    .foregroundColor(.gray)
                    .font(.system(size: size * 0.5))
            )
    }
    
    private func buildImageURL(from urlString: String) -> URL? {
        if urlString.hasPrefix("http://") || urlString.hasPrefix("https://") {
            return URL(string: urlString)
        }
        let baseURL = "https://wayfinder-api-w92x.onrender.com"
        return URL(string: "\(baseURL)\(urlString)")
    }
}

struct DiscussionView_Previews: PreviewProvider {
    static var previews: some View {
        DiscussionView()
    }
}

