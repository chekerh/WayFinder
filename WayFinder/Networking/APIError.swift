import Foundation

enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(Int, Data?)
    case decodingError(Error)
    case custom(String)
    case timeout
    case networkError(Error)

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
        case .timeout:
            return "La requête a expiré. Le serveur met du temps à démarrer, veuillez réessayer."
        case .networkError(let error):
            let nsError = error as NSError
            if nsError.domain == NSURLErrorDomain {
                switch nsError.code {
                case NSURLErrorTimedOut:
                    return "La requête a expiré. Le serveur met du temps à démarrer, veuillez réessayer."
                case NSURLErrorNotConnectedToInternet:
                    return "Pas de connexion Internet."
                case NSURLErrorCannotConnectToHost:
                    return "Impossible de se connecter au serveur."
                default:
                    return "Erreur réseau: \(error.localizedDescription)"
                }
            }
            return "Erreur réseau: \(error.localizedDescription)"
        }
    }
}

