import Foundation

struct PaginatedResponse<T: Decodable>: Decodable {
    let data: [T]
    let pagination: PaginationInfo
    
    enum CodingKeys: String, CodingKey {
        case data
        case pagination
    }
}

struct PaginationInfo: Decodable {
    let page: Int
    let limit: Int
    let total: Int
    let totalPages: Int
    let hasNext: Bool
    let hasPrev: Bool
    
    // No CodingKeys needed - API returns camelCase which matches property names
}

