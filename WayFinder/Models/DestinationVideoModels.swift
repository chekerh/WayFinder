import Foundation

struct DestinationVideoStatus: Decodable {
    let status: String // "not_started" | "processing" | "ready" | "failed"
    let videoUrl: String?
    let imageCount: Int
    let generatedAt: String?
    let errorMessage: String?
    
    enum CodingKeys: String, CodingKey {
        case status
        case videoUrl
        case imageCount
        case generatedAt
        case errorMessage
    }
}

struct GenerateVideoResponse: Decodable {
    let status: String
    let userId: String
    let destination: String
    let imageCount: Int
    let message: String
    
    enum CodingKeys: String, CodingKey {
        case status
        case userId
        case destination
        case imageCount
        case message
    }
}

struct DestinationWithVideoStatus: Decodable, Identifiable {
    let id: String
    let destination: String
    let videoStatus: String
    let videoUrl: String?
    let imageCount: Int
    let errorMessage: String?
    
    enum CodingKeys: String, CodingKey {
        case destination
        case videoStatus
        case videoUrl
        case imageCount
        case errorMessage
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        destination = try container.decode(String.self, forKey: .destination)
        videoStatus = try container.decode(String.self, forKey: .videoStatus)
        videoUrl = try? container.decode(String.self, forKey: .videoUrl)
        imageCount = try container.decode(Int.self, forKey: .imageCount)
        errorMessage = try? container.decode(String.self, forKey: .errorMessage)
        // Use destination as id for Identifiable
        id = destination
    }
    
    init(destination: String, videoStatus: String, videoUrl: String?, imageCount: Int, errorMessage: String?) {
        self.destination = destination
        self.videoStatus = videoStatus
        self.videoUrl = videoUrl
        self.imageCount = imageCount
        self.errorMessage = errorMessage
        self.id = destination
    }
}

struct UserDestinationsResponse: Decodable {
    let destinations: [DestinationWithVideoStatus]
    
    enum CodingKeys: String, CodingKey {
        case destinations
    }
}

