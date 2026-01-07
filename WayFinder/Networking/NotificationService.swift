import Foundation

@MainActor
final class NotificationService {
    static let shared = NotificationService()
    private init() {}
    
    /// Récupère toutes les notifications de l'utilisateur
    func getNotifications(unreadOnly: Bool = false) async throws -> [Notification] {
        // Si on veut toutes les notifications (pas seulement les non lues), charger depuis le cache d'abord
        if !unreadOnly {
            if let cachedNotifications = AppDataCache.shared.notificationsCache.load() {
                print("✅ [NotificationService] Loaded \(cachedNotifications.count) notifications from cache")
                
                // Mettre à jour en arrière-plan sans bloquer
                Task {
                    do {
                        let freshNotifications = try await fetchNotificationsFromAPI(unreadOnly: unreadOnly)
                        AppDataCache.shared.notificationsCache.store(freshNotifications)
                        print("✅ [NotificationService] Updated cache with \(freshNotifications.count) notifications")
                    } catch {
                        print("⚠️ [NotificationService] Failed to update cache: \(error.localizedDescription)")
                    }
                }
                
                return cachedNotifications
            }
        }
        
        // Pas de cache ou unreadOnly : charger depuis l'API
        let notifications = try await fetchNotificationsFromAPI(unreadOnly: unreadOnly)
        
        // Sauvegarder dans le cache seulement si toutes les notifications
        if !unreadOnly {
            AppDataCache.shared.notificationsCache.store(notifications)
        }
        
        return notifications
    }
    
    private func fetchNotificationsFromAPI(unreadOnly: Bool = false) async throws -> [Notification] {
        var queryItems: [URLQueryItem]? = nil
        if unreadOnly {
            queryItems = [URLQueryItem(name: "unreadOnly", value: "true")]
        }
        
        let builder = DefaultRequest(
            method: "GET",
            path: "notifications",
            queryItems: queryItems
        )
        return try await APIService.shared.request(builder, decodeTo: [Notification].self)
    }
    
    /// Récupère le nombre de notifications non lues
    func getUnreadCount() async throws -> Int {
        let builder = DefaultRequest(
            method: "GET",
            path: "notifications/unread-count"
        )
        let response = try await APIService.shared.request(builder, decodeTo: UnreadCountResponse.self)
        return response.count
    }
    
    /// Marque une notification comme lue
    func markAsRead(notificationId: String) async throws -> Notification {
        let builder = DefaultRequest(
            method: "PUT",
            path: "notifications/\(notificationId)/read"
        )
        return try await APIService.shared.request(builder, decodeTo: Notification.self)
    }
    
    /// Marque toutes les notifications comme lues
    func markAllAsRead() async throws {
        let builder = DefaultRequest(
            method: "PUT",
            path: "notifications/read-all"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
    
    /// Supprime une notification
    func deleteNotification(notificationId: String) async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "notifications/\(notificationId)"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
    
    /// Supprime toutes les notifications
    func deleteAllNotifications() async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "notifications"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
    
    /// Crée une notification (principalement pour les tests/admin)
    func createNotification(
        type: NotificationType,
        title: String,
        message: String,
        data: [String: String]? = nil,
        actionUrl: String? = nil
    ) async throws -> Notification {
        let request = CreateNotificationRequest(
            type: type.rawValue,
            title: title,
            message: message,
            data: data,
            actionUrl: actionUrl
        )
        
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "notifications",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Notification.self)
    }
}

