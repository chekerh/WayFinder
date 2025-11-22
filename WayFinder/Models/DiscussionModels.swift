import Foundation

struct DiscussionPost: Decodable, Identifiable {
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
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .id)
        user = try? container.decode(DiscussionUser.self, forKey: .user)
        postId = try DiscussionDecodingHelper.decodeObjectId(from: container, forKey: .postId)
        content = try container.decode(String.self, forKey: .content)
        likesCount = try container.decodeIfPresent(Int.self, forKey: .likesCount) ?? 0
        likedBy = DiscussionDecodingHelper.decodeObjectIdArray(from: container, forKey: .likedBy)
        createdAt = nil
        updatedAt = nil
    }
}

struct DiscussionUser: Decodable, Identifiable {
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

