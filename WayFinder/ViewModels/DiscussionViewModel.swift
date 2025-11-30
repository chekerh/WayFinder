import Foundation

@MainActor
final class DiscussionViewModel: ObservableObject {
    @Published var posts: [DiscussionPost] = []
    @Published var currentPost: DiscussionPost?
    @Published var comments: [DiscussionComment] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: DiscussionService
    
    nonisolated init(service: DiscussionService? = nil) {
        // Accéder à .shared depuis un contexte non isolé
        if let service = service {
            self.service = service
        } else {
            // Utiliser MainActor.assumeIsolated pour accéder à .shared (DiscussionService est Sendable)
            self.service = MainActor.assumeIsolated {
                DiscussionService.shared
            }
        }
    }
    
    func loadPosts(limit: Int = 20, skip: Int = 0, destination: String? = nil) async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [DiscussionViewModel] Loading posts")
        do {
            posts = try await service.getPosts(limit: limit, skip: skip, destination: destination)
            print("✅ [DiscussionViewModel] Loaded \(posts.count) posts")
        } catch {
            let errorDesc = error.localizedDescription
            print("❌ [DiscussionViewModel] Error loading posts: \(errorDesc)")
            
            // Afficher plus de détails pour les erreurs de décodage
            if let decodingError = error as? DecodingError {
                var detailedError = "Erreur de décodage JSON: "
                switch decodingError {
                case .typeMismatch(let type, let context):
                    detailedError += "Type mismatch (\(type)) à \(context.codingPath.map { $0.stringValue }.joined(separator: "."))"
                case .valueNotFound(let type, let context):
                    detailedError += "Valeur manquante (\(type)) à \(context.codingPath.map { $0.stringValue }.joined(separator: "."))"
                case .keyNotFound(let key, let context):
                    detailedError += "Clé manquante (\(key.stringValue)) à \(context.codingPath.map { $0.stringValue }.joined(separator: "."))"
                case .dataCorrupted(let context):
                    detailedError += "Données corrompues à \(context.codingPath.map { $0.stringValue }.joined(separator: ".")): \(context.debugDescription)"
                @unknown default:
                    detailedError += errorDesc
                }
                errorMessage = detailedError
            } else {
                errorMessage = errorDesc
            }
        }
    }
    
    func loadPost(id: String) async {
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [DiscussionViewModel] Loading post: \(id)")
        do {
            currentPost = try await service.getPost(id: id)
            print("✅ [DiscussionViewModel] Post loaded")
        } catch {
            print("❌ [DiscussionViewModel] Error loading post: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func loadComments(postId: String, limit: Int = 50, skip: Int = 0) async {
        print("🔄 [DiscussionViewModel] Loading comments")
        do {
            comments = try await service.getComments(postId: postId, limit: limit, skip: skip)
            print("✅ [DiscussionViewModel] Loaded \(comments.count) comments")
        } catch {
            print("❌ [DiscussionViewModel] Error loading comments: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func createPost(
        title: String,
        content: String,
        tags: [String]? = nil,
        destination: String? = nil,
        imageUrl: String? = nil
    ) async {
        print("🔄 [DiscussionViewModel] Creating post")
        do {
            let post = try await service.createPost(
                title: title,
                content: content,
                tags: tags,
                destination: destination,
                imageUrl: imageUrl
            )
            posts.insert(post, at: 0)
            print("✅ [DiscussionViewModel] Post created")
        } catch {
            print("❌ [DiscussionViewModel] Error creating post: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func likePost(id: String) async {
        print("🔄 [DiscussionViewModel] Liking post")
        do {
            let updated = try await service.likePost(id: id)
            if let index = posts.firstIndex(where: { $0.id == id }) {
                let existing = posts[index]
                // Préserver les données du user existant si le nouveau post n'a pas toutes les infos
                let preservedPost = updated.preservingUser(from: existing)
                posts[index] = preservedPost
            }
            if currentPost?.id == id {
                let existing = currentPost!
                let preservedPost = updated.preservingUser(from: existing)
                currentPost = preservedPost
            }
            print("✅ [DiscussionViewModel] Post liked")
        } catch {
            print("❌ [DiscussionViewModel] Error liking post: \(error.localizedDescription)")
        }
    }
    
    func createComment(postId: String, content: String) async {
        print("🔄 [DiscussionViewModel] Creating comment")
        do {
            let comment = try await service.createComment(postId: postId, content: content)
            comments.append(comment)
            if let index = posts.firstIndex(where: { $0.id == postId }) {
                posts[index].commentsCount += 1
            }
            print("✅ [DiscussionViewModel] Comment created")
        } catch {
            print("❌ [DiscussionViewModel] Error creating comment: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func likeComment(id: String) async {
        print("🔄 [DiscussionViewModel] Liking comment")
        do {
            let updated = try await service.likeComment(id: id)
            if let index = comments.firstIndex(where: { $0.id == id }) {
                let existing = comments[index]
                let merged = updated.preservingOwner(from: existing)
                comments[index] = merged
                print("✅ [DiscussionViewModel] Comment liked - new count: \(updated.likesCount)")
            }
        } catch {
            print("❌ [DiscussionViewModel] Error liking comment: \(error.localizedDescription)")
        }
    }
    
    func deleteComment(id: String) async {
        print("🔄 [DiscussionViewModel] Deleting comment")
        do {
            // Get postId before deleting
            let postId = comments.first(where: { $0.id == id })?.postId
            try await service.deleteComment(id: id)
            comments.removeAll { $0.id == id }
            // Update comment count in posts
            if let postId = postId {
                if let index = posts.firstIndex(where: { $0.id == postId }) {
                    posts[index].commentsCount = max(0, posts[index].commentsCount - 1)
                }
            }
            print("✅ [DiscussionViewModel] Comment deleted")
        } catch {
            print("❌ [DiscussionViewModel] Error deleting comment: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func deletePost(id: String) async {
        print("🔄 [DiscussionViewModel] Deleting post")
        do {
            try await service.deletePost(id: id)
            posts.removeAll { $0.id == id }
            print("✅ [DiscussionViewModel] Post deleted")
        } catch {
            print("❌ [DiscussionViewModel] Error deleting post: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
}

