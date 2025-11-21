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
        var queryItems: [URLQueryItem] = [
            URLQueryItem(name: "limit", value: String(limit)),
            URLQueryItem(name: "skip", value: String(skip))
        ]
        if let destination = destination {
            queryItems.append(URLQueryItem(name: "destination", value: destination))
        }
        
        let builder = DefaultRequest(
            method: "GET",
            path: "discussion/posts",
            queryItems: queryItems
        )
        
        // L'API retourne { posts: [...], total, limit, skip }
        struct PostsResponse: Decodable {
            let posts: [DiscussionPost]
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
        
        let response = try decoder.decode(PostsResponse.self, from: data)
        return response.posts
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

