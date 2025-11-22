import Foundation

@MainActor
final class NotificationViewModel: ObservableObject {
    @Published var notifications: [Notification] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: NotificationService
    
    nonisolated init(service: NotificationService? = nil) {
        // Accéder à .shared depuis un contexte non isolé
        if let service = service {
            self.service = service
        } else {
            // Utiliser MainActor.assumeIsolated pour accéder à .shared (NotificationService est Sendable)
            self.service = MainActor.assumeIsolated {
                NotificationService.shared
            }
        }
    }
    
    func loadNotifications() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [NotificationViewModel] Loading notifications")
        do {
            notifications = try await service.getNotifications()
            print("✅ [NotificationViewModel] Loaded \(notifications.count) notifications")
        } catch {
            print("❌ [NotificationViewModel] Error loading notifications: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func markAsRead(notificationId: String) async {
        print("🔄 [NotificationViewModel] Marking notification as read: \(notificationId)")
        do {
            let updated = try await service.markAsRead(notificationId: notificationId)
            if let index = notifications.firstIndex(where: { $0.id == notificationId }) {
                notifications[index] = updated
            }
            print("✅ [NotificationViewModel] Notification marked as read")
        } catch {
            print("❌ [NotificationViewModel] Error marking as read: \(error.localizedDescription)")
        }
    }
    
    func markAllAsRead() async {
        print("🔄 [NotificationViewModel] Marking all as read")
        do {
            try await service.markAllAsRead()
            await loadNotifications()
            print("✅ [NotificationViewModel] All notifications marked as read")
        } catch {
            print("❌ [NotificationViewModel] Error marking all as read: \(error.localizedDescription)")
        }
    }
    
    func deleteNotification(notificationId: String) async {
        print("🔄 [NotificationViewModel] Deleting notification: \(notificationId)")
        do {
            try await service.deleteNotification(notificationId: notificationId)
            notifications.removeAll { $0.id == notificationId }
            print("✅ [NotificationViewModel] Notification deleted")
        } catch {
            print("❌ [NotificationViewModel] Error deleting notification: \(error.localizedDescription)")
        }
    }
}

