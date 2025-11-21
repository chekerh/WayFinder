import Foundation

struct ImageUploadResult: Decodable {
    struct Payload: Decodable {
        let url: String
    }
    
    let data: Payload
    let success: Bool
    let status: Int
}

enum ImageUploadError: Error, LocalizedError {
    case missingAPIKey
    case invalidResponse
    case uploadFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .missingAPIKey:
            return "Clé API imgbb manquante. Ajoutez IMGBB_API_KEY dans Info.plist."
        case .invalidResponse:
            return "Réponse imgbb invalide."
        case .uploadFailed(let message):
            return "Échec de l’upload imgbb: \(message)"
        }
    }
}

final class ImageUploadService {
    static let shared = ImageUploadService()
    private init() {}
    
    private let session: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 60
        configuration.timeoutIntervalForResource = 90
        return URLSession(configuration: configuration)
    }()
    
    func uploadImage(_ data: Data) async throws -> String {
        guard let apiKey = Self.fetchAPIKey(), !apiKey.isEmpty else {
            throw ImageUploadError.missingAPIKey
        }
        
        guard let url = URL(string: "https://api.imgbb.com/1/upload?key=\(apiKey)") else {
            throw ImageUploadError.invalidResponse
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        let boundary = UUID().uuidString
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        
        let base64String = data.base64EncodedString()
        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"image\"\r\n\r\n".data(using: .utf8)!)
        body.append(base64String.data(using: .utf8)!)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        request.httpBody = body
        
        let (responseData, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ImageUploadError.invalidResponse
        }
        
        guard (200..<300).contains(httpResponse.statusCode) else {
            let message = String(data: responseData, encoding: .utf8) ?? "Statut \(httpResponse.statusCode)"
            throw ImageUploadError.uploadFailed(message)
        }
        
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let uploadResult = try decoder.decode(ImageUploadResult.self, from: responseData)
        
        guard uploadResult.success else {
            throw ImageUploadError.uploadFailed("Réponse success=false")
        }
        
        return uploadResult.data.url
    }
    
    private static func fetchAPIKey() -> String? {
        if let envKey = ProcessInfo.processInfo.environment["IMGBB_API_KEY"], !envKey.isEmpty {
            return envKey
        }
        return Bundle.main.object(forInfoDictionaryKey: "IMGBB_API_KEY") as? String
    }
}

