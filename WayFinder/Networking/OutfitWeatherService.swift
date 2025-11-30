import Foundation
import UIKit

final class OutfitWeatherService {
    static let shared = OutfitWeatherService()
    private init() {}
    
    private let apiService = APIService.shared
    
    // URLSession avec timeout très long pour l'upload et l'analyse d'outfit
    // (upload image + upload ImgBB + analyse AI + météo peut prendre 3-5 minutes sur Render avec cold start)
    private lazy var longTimeoutSession: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 300.0  // 5 minutes pour la requête
        configuration.timeoutIntervalForResource = 360.0  // 6 minutes au total
        return URLSession(configuration: configuration)
    }()
    
    /// Upload outfit image and analyze it
    func uploadOutfitImage(imageData: Data, bookingId: String) async throws -> UploadOutfitResponse {
        guard let token = TokenStorage.fetch() else {
            throw APIError.custom("Token manquant")
        }
        
        // Prepare multipart/form-data request
        let boundary = UUID().uuidString
        var body = Data()
        
        // Add image file
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"image\"; filename=\"outfit.jpg\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n".data(using: .utf8)!)
        
        // Add booking_id (same format as Android)
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"booking_id\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: text/plain\r\n\r\n".data(using: .utf8)!)
        body.append(bookingId.data(using: .utf8)!)
        body.append("\r\n".data(using: .utf8)!)
        
        // Close boundary
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        
        let url = APIConfig.baseURL.appendingPathComponent("outfit-weather/upload")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.httpBody = body
        
        print("🔄 [OutfitWeatherService] Uploading outfit image for booking: \(bookingId)...")
        print("📊 [OutfitWeatherService] Image size: \(imageData.count) bytes (\(String(format: "%.2f", Double(imageData.count) / 1024.0 / 1024.0)) MB)")
        print("⏱️ [OutfitWeatherService] Using extended timeout: 300s request, 360s total")
        print("🔍 [OutfitWeatherService] Booking ID being sent: '\(bookingId)' (length: \(bookingId.count))")
        
        // Utiliser la session avec timeout long pour cette opération qui peut prendre du temps
        // Note: Render peut avoir un timeout de 30s, mais on essaie quand même avec un timeout plus long
        let (responseData, response) = try await longTimeoutSession.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        guard (200..<300).contains(httpResponse.statusCode) else {
            let errorMessage = String(data: responseData, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            print("❌ [OutfitWeatherService] Upload failed: \(errorMessage)")
            throw APIError.httpError(httpResponse.statusCode, responseData)
        }
        
        let decoder = JSONDecoder()
        // Don't use convertFromSnakeCase since we have custom CodingKeys
        decoder.dateDecodingStrategy = .iso8601
        
        do {
            let uploadResponse = try decoder.decode(UploadOutfitResponse.self, from: responseData)
            print("✅ [OutfitWeatherService] Outfit uploaded and analyzed successfully")
            return uploadResponse
        } catch {
            print("❌ [OutfitWeatherService] Decode error: \(error)")
            if let jsonString = String(data: responseData, encoding: .utf8) {
                print("❌ [OutfitWeatherService] Raw JSON: \(jsonString)")
            }
            throw APIError.decodingError(error)
        }
    }
    
    /// Get outfit by ID
    func getOutfit(outfitId: String) async throws -> Outfit {
        let request = DefaultRequest(
            method: "GET",
            path: "outfit-weather/\(outfitId)"
        )
        
        return try await apiService.request(request, decodeTo: Outfit.self)
    }
    
    /// Analyze outfit with image URL (alternative method)
    func analyzeOutfit(bookingId: String, imageUrl: String) async throws -> Outfit {
        let analyzeRequest = AnalyzeOutfitRequest(bookingId: bookingId, imageUrl: imageUrl)
        
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(analyzeRequest)
        
        let request = DefaultRequest(
            method: "POST",
            path: "outfit-weather/analyze",
            body: body
        )
        
        return try await apiService.request(request, decodeTo: Outfit.self)
    }
    
    /// Get all outfits for a booking
    func getOutfitsForBooking(bookingId: String) async throws -> [Outfit] {
        let request = DefaultRequest(
            method: "GET",
            path: "outfit-weather/booking/\(bookingId)"
        )
        
        return try await apiService.request(request, decodeTo: [Outfit].self)
    }
}

