import Foundation

struct DiscussionPost: Codable, Identifiable {
    let id: String
    let user: DiscussionUser
    let title: String
    let content: String
    let tags: [String]
    let destination: String?
    var likesCount: Int
    var likedBy: [String]
    var commentsCount: Int
    let imageUrl: String?
    let createdAt: Date?
    let updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case user = "user_id"
        case title
        case content
        case tags
        case destination
        case likesCount = "likes_count"
        case likedBy = "liked_by"
        case commentsCount = "comments_count"
        case imageUrl = "image_url"
        case createdAt = "createdAt"
        case updatedAt = "updatedAt"
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .id)
        
        // Décoder user_id - peut être un objet (populated) ou juste un ID
        do {
            // Essayer de décoder comme un objet DiscussionUser (populated)
            user = try container.decode(DiscussionUser.self, forKey: .user)
        } catch is DecodingError {
            // Si le décodage échoue, essayer de décoder comme un ID simple
            do {
                let userId = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .user)
                user = DiscussionUser(id: userId, username: nil, firstName: nil, lastName: nil, profileImageUrl: nil)
            } catch {
                // Si même l'ID ne peut pas être décodé, propager l'erreur originale
                print("❌ [DiscussionPost] Error decoding user_id: \(error)")
                throw error
            }
        } catch {
            // Pour toute autre erreur, essayer de décoder comme ID
            do {
                let userId = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .user)
                user = DiscussionUser(id: userId, username: nil, firstName: nil, lastName: nil, profileImageUrl: nil)
            } catch {
                print("❌ [DiscussionPost] Error decoding user_id: \(error)")
                throw error
            }
        }
        
        title = try container.decode(String.self, forKey: .title)
        content = try container.decode(String.self, forKey: .content)
        tags = try container.decodeIfPresent([String].self, forKey: .tags) ?? []
        destination = try container.decodeIfPresent(String.self, forKey: .destination)
        likesCount = try container.decodeIfPresent(Int.self, forKey: .likesCount) ?? 0
        likedBy = DiscussionDecodingHelper.decodeObjectIdArray(from: container, forKey: .likedBy)
        commentsCount = try container.decodeIfPresent(Int.self, forKey: .commentsCount) ?? 0
        imageUrl = try container.decodeIfPresent(String.self, forKey: .imageUrl)
        
        // Décoder createdAt - le JSONDecoder avec .iso8601 devrait le gérer automatiquement
        // Mais si ce n'est pas le cas, essayer manuellement
        var decodedCreatedAt: Date? = nil
        
        if let createdAtDate = try? container.decodeIfPresent(Date.self, forKey: .createdAt) {
            decodedCreatedAt = createdAtDate
        } else if let createdAtString = try? container.decodeIfPresent(String.self, forKey: .createdAt) {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            decodedCreatedAt = formatter.date(from: createdAtString)
            if decodedCreatedAt == nil {
                // Essayer sans fractional seconds
                let formatter2 = ISO8601DateFormatter()
                formatter2.formatOptions = [.withInternetDateTime]
                decodedCreatedAt = formatter2.date(from: createdAtString)
            }
        }
        
        createdAt = decodedCreatedAt
        
        updatedAt = nil
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(user, forKey: .user)
        try container.encode(title, forKey: .title)
        try container.encode(content, forKey: .content)
        try container.encode(tags, forKey: .tags)
        try container.encodeIfPresent(destination, forKey: .destination)
        try container.encode(likesCount, forKey: .likesCount)
        try container.encode(likedBy, forKey: .likedBy)
        try container.encode(commentsCount, forKey: .commentsCount)
        try container.encodeIfPresent(imageUrl, forKey: .imageUrl)
        try container.encodeIfPresent(createdAt, forKey: .createdAt)
        try container.encodeIfPresent(updatedAt, forKey: .updatedAt)
    }
}

struct DiscussionComment: Decodable, Identifiable {
    let id: String
    let user: DiscussionUser?
    let postId: String
    let content: String
    let likesCount: Int
    let likedBy: [String]
    let createdAt: Date?
    let updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case user = "user_id"
        case postId = "post_id"
        case content
        case likesCount = "likes_count"
        case likedBy = "liked_by"
        case createdAt
        case updatedAt
    }
    
    init(
        id: String,
        user: DiscussionUser?,
        postId: String,
        content: String,
        likesCount: Int,
        likedBy: [String],
        createdAt: Date?,
        updatedAt: Date?
    ) {
        self.id = id
        self.user = user
        self.postId = postId
        self.content = content
        self.likesCount = likesCount
        self.likedBy = likedBy
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let id = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .id)
        
        let resolvedUser: DiscussionUser?
        // Décoder user_id - peut être un objet (populated) ou juste un ID
        do {
            // Essayer de décoder comme un objet DiscussionUser (populated)
            resolvedUser = try container.decode(DiscussionUser.self, forKey: .user)
        } catch is DecodingError {
            // Si le décodage échoue, essayer de décoder comme un ID simple
            do {
                let userId = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .user)
                resolvedUser = DiscussionUser(id: userId, username: nil, firstName: nil, lastName: nil, profileImageUrl: nil)
                print("⚠️ [DiscussionComment] user_id is just an ID, not populated: \(userId)")
            } catch {
                // Si même l'ID ne peut pas être décodé, laisser user à nil
                print("❌ [DiscussionComment] Error decoding user_id: \(error)")
                resolvedUser = nil
            }
        } catch {
            // Pour toute autre erreur, essayer de décoder comme ID
            do {
                let userId = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .user)
                resolvedUser = DiscussionUser(id: userId, username: nil, firstName: nil, lastName: nil, profileImageUrl: nil)
                print("⚠️ [DiscussionComment] user_id is just an ID, not populated: \(userId)")
            } catch {
                print("❌ [DiscussionComment] Error decoding user_id: \(error)")
                resolvedUser = nil
            }
        }
        
        let postId = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .postId)
        let content = try container.decode(String.self, forKey: .content)
        let likesCount = try container.decodeIfPresent(Int.self, forKey: .likesCount) ?? 0
        let likedBy = DiscussionDecodingHelper.decodeObjectIdArray(from: container, forKey: .likedBy)
        
        self.init(
            id: id,
            user: resolvedUser,
            postId: postId,
            content: content,
            likesCount: likesCount,
            likedBy: likedBy,
            createdAt: nil,
            updatedAt: nil
        )
    }
}

struct DiscussionUser: Codable, Identifiable {
    let id: String
    let username: String?
    let firstName: String?
    let lastName: String?
    let profileImageUrl: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case username
        case firstName = "first_name"
        case lastName = "last_name"
        case profileImageUrl = "profile_image_url"
    }
    
    init(id: String, username: String?, firstName: String?, lastName: String?, profileImageUrl: String?) {
        self.id = id
        self.username = username
        self.firstName = firstName
        self.lastName = lastName
        self.profileImageUrl = profileImageUrl
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Décoder _id - peut être une string ou un objet avec $oid
        id = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .id)
        
        // Décoder les autres champs (optionnels)
        username = try container.decodeIfPresent(String.self, forKey: .username)
        firstName = try container.decodeIfPresent(String.self, forKey: .firstName)
        lastName = try container.decodeIfPresent(String.self, forKey: .lastName)
        profileImageUrl = try container.decodeIfPresent(String.self, forKey: .profileImageUrl)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encodeIfPresent(username, forKey: .username)
        try container.encodeIfPresent(firstName, forKey: .firstName)
        try container.encodeIfPresent(lastName, forKey: .lastName)
        try container.encodeIfPresent(profileImageUrl, forKey: .profileImageUrl)
    }
    
    var displayName: String {
        if let firstName = firstName, !firstName.isEmpty,
           let lastName = lastName, !lastName.isEmpty {
            return "\(firstName) \(lastName)"
        }
        if let firstName = firstName, !firstName.isEmpty {
            return firstName
        }
        return username ?? "Voyageur"
    }
}

// MARK: - Helpers
extension DiscussionComment {
    /// Retourne une copie du commentaire en préservant les informations du propriétaire existant.
    func preservingOwner(from existing: DiscussionComment) -> DiscussionComment {
        let bestUser: DiscussionUser?
        if let user = user {
            if user.hasDisplayInfo || existing.user == nil {
                bestUser = user
            } else {
                bestUser = existing.user
            }
        } else {
            bestUser = existing.user
        }
        
        return DiscussionComment(
            id: id,
            user: bestUser,
            postId: postId,
            content: content,
            likesCount: likesCount,
            likedBy: likedBy,
            createdAt: createdAt ?? existing.createdAt,
            updatedAt: updatedAt ?? existing.updatedAt
        )
    }
}

extension DiscussionUser {
    /// Indique si le profil contient suffisamment d'informations pour être affiché.
    var hasDisplayInfo: Bool {
        if let firstName = firstName, !firstName.isEmpty {
            return true
        }
        if let lastName = lastName, !lastName.isEmpty {
            return true
        }
        if let username = username, !username.isEmpty {
            return true
        }
        return false
    }
    
    /// Retourne le meilleur user entre deux, en préservant les informations complètes.
    static func preserving(from new: DiscussionUser, existing: DiscussionUser) -> DiscussionUser {
        // Si le nouveau user a toutes les infos, l'utiliser
        if new.profileImageUrl != nil && new.hasDisplayInfo {
            return new
        }
        // Sinon, préserver l'existant s'il a plus d'infos
        if existing.profileImageUrl != nil || existing.hasDisplayInfo {
            return DiscussionUser(
                id: new.id, // Toujours utiliser le nouvel ID
                username: new.username ?? existing.username,
                firstName: new.firstName ?? existing.firstName,
                lastName: new.lastName ?? existing.lastName,
                profileImageUrl: new.profileImageUrl ?? existing.profileImageUrl
            )
        }
        // Sinon, utiliser le nouveau
        return new
    }
}

extension DiscussionPost {
    /// Retourne une copie du post en préservant les informations du user existant.
    func preservingUser(from existing: DiscussionPost) -> DiscussionPost {
        let preservedUser = DiscussionUser.preserving(from: self.user, existing: existing.user)
        
        // Créer un nouveau post en encodant/décodant pour préserver toutes les propriétés
        do {
            let encoder = JSONEncoder()
            encoder.dateEncodingStrategy = .iso8601
            var jsonData = try encoder.encode(self)
            
            // Modifier le user dans le JSON avec le user préservé
            if var json = try JSONSerialization.jsonObject(with: jsonData) as? [String: Any] {
                // Encoder le user préservé
                let userData = try encoder.encode(preservedUser)
                if let userDict = try JSONSerialization.jsonObject(with: userData) as? [String: Any] {
                    json["user_id"] = userDict
                }
                jsonData = try JSONSerialization.data(withJSONObject: json)
            }
            
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            return try decoder.decode(DiscussionPost.self, from: jsonData)
        } catch {
            print("⚠️ [DiscussionPost] Error preserving user: \(error)")
            // Fallback: retourner le post mis à jour tel quel (mieux que de perdre les likes)
            return self
        }
    }
}

enum DiscussionDecodingHelper {
    static func decodeObjectId<Key: CodingKey>(from container: KeyedDecodingContainer<Key>, forKey key: Key) throws -> String {
        if let idString = try? container.decode(String.self, forKey: key) {
            return idString
        } else if let idDict = try? container.decode([String: String].self, forKey: key),
                  let idValue = idDict["$oid"] {
            return idValue
        }
        throw DecodingError.dataCorruptedError(forKey: key, in: container, debugDescription: "Invalid id format")
    }
    
    static func decodeObjectIdArray<Key: CodingKey>(from container: KeyedDecodingContainer<Key>, forKey key: Key) -> [String] {
        if let array = try? container.decode([String].self, forKey: key) {
            return array
        } else if let dictArray = try? container.decode([[String: String]].self, forKey: key) {
            return dictArray.compactMap { $0["$oid"] }
        }
        return []
    }
}

struct CreatePostRequest: Encodable {
    let title: String
    let content: String
    let tags: [String]?
    let destination: String?
    let imageUrl: String?
    
    enum CodingKeys: String, CodingKey {
        case title, content, tags, destination
        case imageUrl = "image_url"
    }
}

struct UpdatePostRequest: Encodable {
    let title: String?
    let content: String?
    let tags: [String]?
}

struct CreateCommentRequest: Encodable {
    let content: String
}

