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
        case .httpError(let code, let data):
            // Essayer d'extraire le message depuis les données si disponibles
            if let data = data,
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = json["message"] as? String {
                return message
            }
            // Messages par défaut selon le code HTTP
            switch code {
            case 400:
                return "Requête invalide."
            case 401:
                return "Non autorisé. Veuillez vous reconnecter."
            case 403:
                return "Accès refusé."
            case 404:
                return "Ressource non trouvée."
            case 409:
                return "Conflit. Cette ressource existe déjà."
            case 500:
                return "Erreur interne du serveur."
            default:
                return "Erreur HTTP (code: \(code))."
            }
        case .decodingError(let error):
            return "Erreur de décodage JSON: \(error.localizedDescription)"
        case .custom(let message):
            return translateErrorMessage(message)
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

/// Traduit les messages d'erreur courants du backend en français
private func translateErrorMessage(_ message: String) -> String {
    let lowercased = message.lowercased()
    
    // Check for "Email or username" first (ambiguous message)
    if lowercased.contains("email or username already exists") || 
       lowercased.contains("email or username already exist") {
        // Keep the original message so the caller can handle it appropriately
        return message // Return original to allow proper detection
    }
    
    if lowercased.contains("email already exists") || lowercased.contains("email already exist") {
        return "Cet email est déjà utilisé. Veuillez utiliser un autre email."
    }
    if lowercased.contains("username already exists") || lowercased.contains("username already exist") {
        return "Ce nom d'utilisateur est déjà utilisé. Veuillez en choisir un autre."
    }
    if lowercased.contains("invalid credentials") || lowercased.contains("wrong password") {
        return "Email ou mot de passe incorrect."
    }
    if lowercased.contains("user not found") {
        return "Utilisateur non trouvé."
    }
    if lowercased.contains("internal server error") {
        return "Erreur interne du serveur. Veuillez réessayer plus tard."
    }
    if lowercased.contains("all fields are required") {
        return "Tous les champs sont requis."
    }
    if lowercased.contains("password is required") {
        return "Le mot de passe est requis."
    }
    
    // Retourner le message original s'il n'y a pas de traduction
    return message
}

