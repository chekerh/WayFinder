import Foundation

final class JourneyService {
    static let shared = JourneyService()
    private init() {}
    
    func createJourney(
        images: [Data],
        bookingId: String?,
        destination: String?,
        description: String?,
        tags: [String]?,
        isPublic: Bool?
    ) async throws -> Journey {
        guard let token = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }
        
        let url = APIConfig.baseURL.appendingPathComponent("journey")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        
        print("🔵 [JourneyService] Creating journey with \(images.count) images")
        print("🔵 [JourneyService] URL: \(url.absoluteString)")
        
        // Create multipart form data
        let boundary = UUID().uuidString
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        
        var body = Data()
        
        // Add images
        for (index, imageData) in images.enumerated() {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"images\"; filename=\"image\(index).jpg\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
            body.append(imageData)
            body.append("\r\n".data(using: .utf8)!)
        }
        
        // Add booking_id (as text/plain like Android)
        if let bookingId = bookingId, !bookingId.isEmpty {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"booking_id\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: text/plain\r\n\r\n".data(using: .utf8)!)
            body.append(bookingId.data(using: .utf8)!)
            body.append("\r\n".data(using: .utf8)!)
        }
        
        // Add destination (as text/plain like Android)
        if let destination = destination, !destination.isEmpty {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"destination\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: text/plain\r\n\r\n".data(using: .utf8)!)
            body.append(destination.data(using: .utf8)!)
            body.append("\r\n".data(using: .utf8)!)
        }
        
        // Add description (as text/plain like Android)
        if let description = description, !description.isEmpty {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"description\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: text/plain\r\n\r\n".data(using: .utf8)!)
            body.append(description.data(using: .utf8)!)
            body.append("\r\n".data(using: .utf8)!)
        }
        
        // Add tags (as JSON array like Android)
        if let tags = tags, !tags.isEmpty {
            let tagsJSON = try JSONEncoder().encode(tags)
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"tags\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: application/json\r\n\r\n".data(using: .utf8)!)
            body.append(tagsJSON)
            body.append("\r\n".data(using: .utf8)!)
        }
        
        // Add is_public (as text/plain like Android)
        if let isPublic = isPublic {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"is_public\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: text/plain\r\n\r\n".data(using: .utf8)!)
            body.append((isPublic ? "true" : "false").data(using: .utf8)!)
            body.append("\r\n".data(using: .utf8)!)
        }
        
        // Close boundary
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        
        request.httpBody = body
        request.setValue("\(body.count)", forHTTPHeaderField: "Content-Length")
        
        print("🔵 [JourneyService] Request body size: \(body.count) bytes")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidURL
        }
        
        guard (200..<300).contains(httpResponse.statusCode) else {
            let errorMessage = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            print("❌ [JourneyService] HTTP Error \(httpResponse.statusCode): \(errorMessage)")
            if let errorString = String(data: data, encoding: .utf8) {
                print("❌ [JourneyService] Response body: \(errorString)")
            }
            throw APIError.httpError(httpResponse.statusCode, data)
        }
        
        print("✅ [JourneyService] Journey created successfully")
        
        // Log response for debugging
        if let responseString = String(data: data, encoding: .utf8) {
            print("🔵 [JourneyService] Response: \(responseString.prefix(500))")
        }
        
        let decoder = JSONDecoder()
        // Ne pas utiliser convertFromSnakeCase car nous avons des CodingKeys personnalisés
        do {
            return try decoder.decode(Journey.self, from: data)
        } catch {
            print("❌ [JourneyService] Decoding error: \(error)")
            if let responseString = String(data: data, encoding: .utf8) {
                print("❌ [JourneyService] Full response: \(responseString)")
            }
            throw error
        }
    }
    
    func getJourneys(limit: Int? = nil, skip: Int? = nil) async throws -> [Journey] {
        guard let token = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }
        
        var queryItems: [URLQueryItem] = []
        if let limit = limit {
            queryItems.append(URLQueryItem(name: "limit", value: "\(limit)"))
        }
        if let skip = skip {
            queryItems.append(URLQueryItem(name: "skip", value: "\(skip)"))
        }
        
        var urlComponents = URLComponents(url: APIConfig.baseURL.appendingPathComponent("journey"), resolvingAgainstBaseURL: false)
        urlComponents?.queryItems = queryItems.isEmpty ? nil : queryItems
        
        guard let url = urlComponents?.url else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        
        print("🔵 [JourneyService] Fetching journeys from: \(url.absoluteString)")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        guard (200..<300).contains(httpResponse.statusCode) else {
            let errorMessage = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            print("❌ [JourneyService] HTTP Error \(httpResponse.statusCode): \(errorMessage)")
            throw APIError.httpError(httpResponse.statusCode, data)
        }
        
        // Log response for debugging
        if let responseString = String(data: data, encoding: .utf8) {
            print("🔵 [JourneyService] Response: \(responseString.prefix(1000))")
        }
        
        let decoder = JSONDecoder()
        // Ne pas utiliser convertFromSnakeCase car nous avons des CodingKeys personnalisés
        do {
            return try decoder.decode([Journey].self, from: data)
        } catch {
            print("❌ [JourneyService] Decoding error: \(error)")
            if let decodingError = error as? DecodingError {
                switch decodingError {
                case .typeMismatch(let type, let context):
                    print("   Type mismatch: expected \(type), path: \(context.codingPath)")
                case .valueNotFound(let type, let context):
                    print("   Value not found: \(type), path: \(context.codingPath)")
                case .keyNotFound(let key, let context):
                    print("   Key not found: \(key.stringValue), path: \(context.codingPath)")
                case .dataCorrupted(let context):
                    print("   Data corrupted: \(context.debugDescription)")
                @unknown default:
                    print("   Unknown decoding error")
                }
            }
            if let responseString = String(data: data, encoding: .utf8) {
                print("❌ [JourneyService] Full response: \(responseString)")
            }
            throw error
        }
    }
    
    func canShareJourney() async throws -> CanShareJourneyResponse {
        let builder = DefaultRequest(
            method: "GET",
            path: "journey/can-share"
        )
        
        return try await APIService.shared.request(builder, decodeTo: CanShareJourneyResponse.self)
    }
    
    func regenerateVideo(journeyId: String) async throws -> [String: String] {
        let builder = DefaultRequest(
            method: "POST",
            path: "journey/\(journeyId)/regenerate-video"
        )
        
        return try await APIService.shared.request(builder, decodeTo: [String: String].self)
    }
    
    func getMyJourneys(limit: Int? = nil, skip: Int? = nil) async throws -> [Journey] {
        var queryItems: [URLQueryItem] = []
        if let limit = limit {
            queryItems.append(URLQueryItem(name: "limit", value: "\(limit)"))
        }
        if let skip = skip {
            queryItems.append(URLQueryItem(name: "skip", value: "\(skip)"))
        }
        
        let builder = DefaultRequest(
            method: "GET",
            path: "journey/my-journeys",
            queryItems: queryItems.isEmpty ? nil : queryItems
        )
        
        return try await APIService.shared.request(builder, decodeTo: [Journey].self)
    }
    
    func likeJourney(journeyId: String) async throws -> JourneyLikeResponse {
        let builder = DefaultRequest(
            method: "POST",
            path: "journey/\(journeyId)/like"
        )
        
        return try await APIService.shared.request(builder, decodeTo: JourneyLikeResponse.self)
    }
    
    func deleteJourney(journeyId: String) async throws {
        guard let token = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }
        
        let url = APIConfig.baseURL.appendingPathComponent("journey/\(journeyId)")
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        
        print("🔵 [JourneyService] Deleting journey: \(journeyId)")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        guard (200..<300).contains(httpResponse.statusCode) else {
            let errorMessage = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            print("❌ [JourneyService] HTTP Error \(httpResponse.statusCode): \(errorMessage)")
            throw APIError.httpError(httpResponse.statusCode, data)
        }
        
        print("✅ [JourneyService] Journey deleted successfully")
    }
}

