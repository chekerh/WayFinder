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

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            if let nestError = try? JSONDecoder().decode(NestError.self, from: data) {
                throw APIError.custom(nestError.message.text)
            }
            throw APIError.httpError(httpResponse.statusCode, data)
        }

        do {
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            decoder.dateDecodingStrategy = .iso8601
            return try decoder.decode(T.self, from: data)
        } catch {
            throw APIError.decodingError(error)
        }
    }

    func requestVoid(_ builder: RequestBuilder) async throws {
        struct Empty: Decodable {}
        let _: Empty = try await request(builder, decodeTo: Empty.self)
    }
}

