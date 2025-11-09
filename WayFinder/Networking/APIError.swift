import Foundation

enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(Int, Data?)
    case decodingError(Error)
    case custom(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "URL invalide."
        case .invalidResponse:
            return "Réponse du serveur invalide."
        case .httpError(let code, _):
            return "Erreur HTTP (code: \(code))."
        case .decodingError(let error):
            return "Erreur de décodage JSON: \(error.localizedDescription)"
        case .custom(let message):
            return message
        }
    }
}

