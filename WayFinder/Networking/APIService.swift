import Foundation

protocol RequestBuilder {
    var method: String { get }
    var path: String { get }
    var queryItems: [URLQueryItem]? { get }
    var headers: [String: String]? { get }
    var body: Data? { get }
}

struct DefaultRequest: RequestBuilder {
    var method: String
    var path: String
    var queryItems: [URLQueryItem]? = nil
    var headers: [String: String]? = nil
    var body: Data? = nil
}

final class APIService {
    static let shared = APIService()
    private init() {}

    // URLSession configurée avec des timeouts plus longs pour gérer les cold starts de Render
    private lazy var session: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 90.0  // 90 secondes pour le cold start Render
        configuration.timeoutIntervalForResource = 120.0  // 120 secondes au total
        return URLSession(configuration: configuration)
    }()

    func request<T: Decodable>(_ builder: RequestBuilder, decodeTo type: T.Type) async throws -> T {
        var urlComponents = URLComponents(url: APIConfig.baseURL.appendingPathComponent(builder.path),
                                          resolvingAgainstBaseURL: false)
        urlComponents?.queryItems = builder.queryItems

        guard let url = urlComponents?.url else { throw APIError.invalidURL }

        var request = URLRequest(url: url)
        request.httpMethod = builder.method
        request.httpBody = builder.body
        builder.headers?.forEach { request.setValue($1, forHTTPHeaderField: $0) }

        if request.value(forHTTPHeaderField: "Content-Type") == nil {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        
        if request.value(forHTTPHeaderField: "Authorization") == nil,
           let token = TokenStorage.fetch(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        // Log de debug pour les tests
        print("🌐 [API] \(builder.method) \(url.absoluteString)")
        if let body = request.httpBody, let bodyString = String(data: body, encoding: .utf8) {
            print("📤 [API] Body: \(bodyString)")
        }

        let (data, response): (Data, URLResponse)
        do {
            print("⏳ [API] Starting network request (timeout: \(session.configuration.timeoutIntervalForRequest)s)...")
            (data, response) = try await session.data(for: request)
            print("✅ [API] Network request completed, received \(data.count) bytes")
        } catch {
            print("❌ [API] Network error: \(error.localizedDescription)")
            let errorTypeName = String(describing: Swift.type(of: error))
            print("❌ [API] Error type: \(errorTypeName)")
            if let urlError = error as? URLError {
                print("❌ [API] URLError code: \(urlError.code.rawValue) (\(urlError.code))")
                print("❌ [API] URLError description: \(urlError.localizedDescription)")
                switch urlError.code {
                case .timedOut:
                    print("❌ [API] Request timed out after \(session.configuration.timeoutIntervalForRequest)s")
                    throw APIError.timeout
                case .notConnectedToInternet:
                    print("❌ [API] No internet connection")
                    throw APIError.custom("Pas de connexion Internet")
                case .cannotConnectToHost:
                    print("❌ [API] Cannot connect to host: \(url.host ?? "unknown")")
                    throw APIError.custom("Impossible de se connecter au serveur. Le serveur peut être en cours de démarrage.")
                case .networkConnectionLost:
                    print("❌ [API] Network connection lost")
                    throw APIError.custom("Connexion réseau perdue. Veuillez réessayer.")
                default:
                    print("❌ [API] Other network error: \(urlError)")
                    throw APIError.networkError(error)
                }
            } else {
                throw APIError.networkError(error)
            }
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }

        // Log de la réponse
        print("📥 [API] Status: \(httpResponse.statusCode)")
        if data.isEmpty {
            print("⚠️ [API] Response data is EMPTY")
        }
        if let responseString = String(data: data, encoding: .utf8) {
            let preview = responseString.prefix(1000)
            print("📥 [API] Response (\(data.count) bytes): \(preview)")
            if responseString.count > 1000 {
                print("📥 [API] ... (truncated, total: \(responseString.count) chars)")
            }
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            // Essayer de décoder l'erreur NestJS standard
            if let nestError = try? JSONDecoder().decode(NestError.self, from: data) {
                print("❌ [API] Error: \(nestError.message.text)")
                throw APIError.custom(nestError.message.text)
            }
            
            // Essayer d'extraire le message directement depuis le JSON
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = json["message"] as? String {
                print("❌ [API] Error message: \(message)")
                throw APIError.custom(message)
            }
            
            // Si le message est un tableau (format de validation NestJS)
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let messageArray = json["message"] as? [String],
               let firstMessage = messageArray.first {
                print("❌ [API] Error message: \(firstMessage)")
                throw APIError.custom(firstMessage)
            }
            
            print("❌ [API] HTTP Error: \(httpResponse.statusCode)")
            throw APIError.httpError(httpResponse.statusCode, data)
        }

        do {
            let decoder = JSONDecoder()
            // Ne pas utiliser convertFromSnakeCase si le modèle a des CodingKeys personnalisés
            // decoder.keyDecodingStrategy = .convertFromSnakeCase
            decoder.dateDecodingStrategy = .iso8601
            let result = try decoder.decode(T.self, from: data)
            print("✅ [API] Decode success")
            return result
        } catch {
            print("❌ [API] Decode error: \(error.localizedDescription)")
            if let decodingError = error as? DecodingError {
                print("❌ [API] DecodingError details:")
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
            // Afficher le JSON brut pour debug
            if let jsonString = String(data: data, encoding: .utf8) {
                print("❌ [API] Raw JSON: \(jsonString)")
            }
            throw APIError.decodingError(error)
        }
    }

    func requestVoid(_ builder: RequestBuilder) async throws {
        struct Empty: Decodable {}
        let _: Empty = try await request(builder, decodeTo: Empty.self)
    }
    
    /// Returns raw data and response without decoding (useful for custom decoding)
    func requestRaw(_ builder: RequestBuilder) async throws -> (Data, HTTPURLResponse) {
        var urlComponents = URLComponents(url: APIConfig.baseURL.appendingPathComponent(builder.path),
                                          resolvingAgainstBaseURL: false)
        urlComponents?.queryItems = builder.queryItems

        guard let url = urlComponents?.url else { throw APIError.invalidURL }

        var request = URLRequest(url: url)
        request.httpMethod = builder.method
        request.httpBody = builder.body
        builder.headers?.forEach { request.setValue($1, forHTTPHeaderField: $0) }

        if request.value(forHTTPHeaderField: "Content-Type") == nil {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        
        if request.value(forHTTPHeaderField: "Authorization") == nil,
           let token = TokenStorage.fetch(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        // Log de debug pour les tests
        print("🌐 [API] \(builder.method) \(url.absoluteString)")
        if let body = request.httpBody, let bodyString = String(data: body, encoding: .utf8) {
            print("📤 [API] Body: \(bodyString)")
        }

        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            print("❌ [API] Network error: \(error.localizedDescription)")
            throw APIError.networkError(error)
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }

        print("📥 [API] Status: \(httpResponse.statusCode)")
        if let responseString = String(data: data, encoding: .utf8) {
            print("📥 [API] Response: \(responseString.prefix(500))")
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            // Essayer de décoder l'erreur NestJS standard
            if let nestError = try? JSONDecoder().decode(NestError.self, from: data) {
                print("❌ [API] Error: \(nestError.message.text)")
                throw APIError.custom(nestError.message.text)
            }
            
            // Essayer d'extraire le message directement depuis le JSON
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = json["message"] as? String {
                print("❌ [API] Error message: \(message)")
                throw APIError.custom(message)
            }
            
            // Si le message est un tableau (format de validation NestJS)
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let messageArray = json["message"] as? [String],
               let firstMessage = messageArray.first {
                print("❌ [API] Error message: \(firstMessage)")
                throw APIError.custom(firstMessage)
            }
            
            print("❌ [API] HTTP Error: \(httpResponse.statusCode)")
            throw APIError.httpError(httpResponse.statusCode, data)
        }

        return (data, httpResponse)
    }
}

