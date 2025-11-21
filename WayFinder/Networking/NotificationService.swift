import Foundation

@MainActor
final class NotificationService {
    static let shared = NotificationService()
    private init() {}
    
    /// Récupère toutes les notifications de l'utilisateur
    func getNotifications(unreadOnly: Bool = false) async throws -> [Notification] {
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

