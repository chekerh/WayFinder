import Foundation

enum APIConfig {
    /// Point d’entrée principal de l’API NestJS.
    /// - Important: pour un appareil physique, remplace `localhost` par l’adresse IP du Mac (ex: 192.168.0.42).
    static let baseURL = URL(string: "https://wayfinder-api-w92x.onrender.com/api")!
}

