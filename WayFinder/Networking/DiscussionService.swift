import Foundation

@MainActor
final class DiscussionService {
    static let shared = DiscussionService()
    private init() {}
    
    /// Crée un post
    func createPost(
        title: String,
        content: String,
        tags: [String]? = nil,
        destination: String? = nil,
        imageUrl: String? = nil
    ) async throws -> DiscussionPost {
        let request = CreatePostRequest(
            title: title,
            content: content,
            tags: tags,
            destination: destination,
            imageUrl: imageUrl
        )
        
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "discussion/posts",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        
        // Utiliser un décodage personnalisé pour éviter les conflits avec convertFromSnakeCase
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        let decoder = JSONDecoder()
        // Ne pas utiliser convertFromSnakeCase car nos CodingKeys gèrent déjà le mapping
        decoder.keyDecodingStrategy = .useDefaultKeys
        decoder.dateDecodingStrategy = .iso8601
        
        return try decoder.decode(DiscussionPost.self, from: data)
    }
    
    /// Récupère les posts
    func getPosts(
        limit: Int = 20,
        skip: Int = 0,
        destination: String? = nil
    ) async throws -> [DiscussionPost] {
        // Si c'est la première page et pas de filtre destination, charger depuis le cache d'abord
        if skip == 0 && destination == nil {
            if let cachedPosts = AppDataCache.shared.postsCache.load() {
                print("✅ [DiscussionService] Loaded \(cachedPosts.count) posts from cache")
                
                // Limiter aux premiers éléments si nécessaire
                let limitedPosts = Array(cachedPosts.prefix(limit))
                
                // Mettre à jour en arrière-plan sans bloquer
                Task {
                    do {
                        let freshPosts = try await fetchPostsFromAPI(limit: limit, skip: skip, destination: destination)
                        AppDataCache.shared.postsCache.store(freshPosts)
                        print("✅ [DiscussionService] Updated cache with \(freshPosts.count) posts")
                    } catch {
                        print("⚠️ [DiscussionService] Failed to update cache: \(error.localizedDescription)")
                    }
                }
                
                return limitedPosts
            }
        }
        
        // Pas de cache ou filtre destination : charger depuis l'API
        let posts = try await fetchPostsFromAPI(limit: limit, skip: skip, destination: destination)
        
        // Sauvegarder dans le cache seulement pour la première page sans filtre
        if skip == 0 && destination == nil {
            AppDataCache.shared.postsCache.store(posts)
        }
        
        return posts
    }
    
    private func fetchPostsFromAPI(
        limit: Int = 20,
        skip: Int = 0,
        destination: String? = nil
    ) async throws -> [DiscussionPost] {
        // Convertir skip en page (l'API utilise page, pas skip)
        // page = (skip / limit) + 1
        let page = (skip / limit) + 1
        
        var queryItems: [URLQueryItem] = [
            URLQueryItem(name: "limit", value: String(limit)),
            URLQueryItem(name: "page", value: String(page))
        ]
        if let destination = destination {
            queryItems.append(URLQueryItem(name: "destination", value: destination))
        }
        
        let builder = DefaultRequest(
            method: "GET",
            path: "discussion/posts",
            queryItems: queryItems
        )
        
        // L'API retourne { data: [...], pagination: {...} } (format paginé standard)
        // Mais on supporte aussi l'ancien format { posts: [...], total, limit, skip } pour compatibilité
        
        // Utiliser un décodage personnalisé pour éviter les conflits avec convertFromSnakeCase
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        // Log raw JSON for debugging
        if let jsonString = String(data: data, encoding: .utf8) {
            print("🔍 [DiscussionService] Raw JSON response: \(jsonString.prefix(500))")
        }
        
        let decoder = JSONDecoder()
        // Ne pas utiliser convertFromSnakeCase car nos CodingKeys gèrent déjà le mapping
        decoder.keyDecodingStrategy = .useDefaultKeys
        decoder.dateDecodingStrategy = .iso8601
        
        // Essayer d'abord le format paginé standard { data: [...], pagination: {...} }
        if let paginatedResponse = try? decoder.decode(PaginatedResponse<DiscussionPost>.self, from: data) {
            print("✅ [DiscussionService] Decoded paginated response with \(paginatedResponse.data.count) posts")
            return paginatedResponse.data
        }
        
        // Essayer l'ancien format { posts: [...], total, limit, skip }
        struct PostsResponse: Decodable {
            let posts: [DiscussionPost]
            let total: Int?
            let limit: Int?
            let skip: Int?
        }
        
        if let oldResponse = try? decoder.decode(PostsResponse.self, from: data) {
            print("✅ [DiscussionService] Decoded old format response with \(oldResponse.posts.count) posts")
            return oldResponse.posts
        }
        
        // Si aucun format ne fonctionne, essayer directement un tableau
        if let directArray = try? decoder.decode([DiscussionPost].self, from: data) {
            print("✅ [DiscussionService] Decoded direct array with \(directArray.count) posts")
            return directArray
        }
        
        // Si tout échoue, lancer une erreur descriptive
        throw APIError.decodingError(NSError(
            domain: "DiscussionService",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Impossible de décoder la réponse de l'API. Format inattendu."]
        ))
    }
    
    /// Récupère un post par ID
    func getPost(id: String) async throws -> DiscussionPost {
        let builder = DefaultRequest(
            method: "GET",
            path: "discussion/posts/\(id)"
        )
        
        // Utiliser un décodage personnalisé pour éviter les conflits avec convertFromSnakeCase
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        let decoder = JSONDecoder()
        // Ne pas utiliser convertFromSnakeCase car nos CodingKeys gèrent déjà le mapping
        decoder.keyDecodingStrategy = .useDefaultKeys
        decoder.dateDecodingStrategy = .iso8601
        
        return try decoder.decode(DiscussionPost.self, from: data)
    }
    
    /// Met à jour un post
    func updatePost(
        id: String,
        title: String? = nil,
        content: String? = nil,
        tags: [String]? = nil
    ) async throws -> DiscussionPost {
        let request = UpdatePostRequest(
            title: title,
            content: content,
            tags: tags
        )
        
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "PUT",
            path: "discussion/posts/\(id)",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: DiscussionPost.self)
    }
    
    /// Supprime un post
    func deletePost(id: String) async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "discussion/posts/\(id)"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
    
    /// Like un post
    func likePost(id: String) async throws -> DiscussionPost {
        let builder = DefaultRequest(
            method: "POST",
            path: "discussion/posts/\(id)/like"
        )
        return try await APIService.shared.request(builder, decodeTo: DiscussionPost.self)
    }
    
    /// Crée un commentaire
    func createComment(
        postId: String,
        content: String
    ) async throws -> DiscussionComment {
        let request = CreateCommentRequest(content: content)
        
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "discussion/posts/\(postId)/comments",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        
        // Utiliser un décodage personnalisé pour éviter les conflits avec convertFromSnakeCase
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .useDefaultKeys
        decoder.dateDecodingStrategy = .iso8601
        
        return try decoder.decode(DiscussionComment.self, from: data)
    }
    
    /// Récupère les commentaires d'un post
    func getComments(
        postId: String,
        limit: Int = 50,
        skip: Int = 0
    ) async throws -> [DiscussionComment] {
        let queryItems = [
            URLQueryItem(name: "limit", value: String(limit)),
            URLQueryItem(name: "skip", value: String(skip))
        ]
        
        let builder = DefaultRequest(
            method: "GET",
            path: "discussion/posts/\(postId)/comments",
            queryItems: queryItems
        )
        
        // L'API retourne { comments: [...], total, limit, skip }
        struct CommentsResponse: Decodable {
            let comments: [DiscussionComment]
            let total: Int?
            let limit: Int?
            let skip: Int?
        }
        
        // Utiliser un décodage personnalisé pour éviter les conflits avec convertFromSnakeCase
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        let decoder = JSONDecoder()
        // Ne pas utiliser convertFromSnakeCase car nos CodingKeys gèrent déjà le mapping
        decoder.keyDecodingStrategy = .useDefaultKeys
        decoder.dateDecodingStrategy = .iso8601
        
        let response = try decoder.decode(CommentsResponse.self, from: data)
        return response.comments
    }
    
    /// Like un commentaire
    func likeComment(id: String) async throws -> DiscussionComment {
        let builder = DefaultRequest(
            method: "POST",
            path: "discussion/comments/\(id)/like"
        )
        
        // Utiliser un décodage personnalisé pour éviter les conflits avec convertFromSnakeCase
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .useDefaultKeys
        decoder.dateDecodingStrategy = .iso8601
        
        return try decoder.decode(DiscussionComment.self, from: data)
    }
    
    /// Supprime un commentaire
    func deleteComment(id: String) async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "discussion/comments/\(id)"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
}

