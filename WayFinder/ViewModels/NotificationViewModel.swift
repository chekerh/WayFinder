import Foundation

@MainActor
final class NotificationViewModel: ObservableObject {
    @Published var notifications: [Notification] = []
    @Published var unreadCount: Int = 0
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: NotificationService
    
    init(service: NotificationService = .shared) {
        self.service = service
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
    
    func loadUnreadCount() async {
        print("🔄 [NotificationViewModel] Loading unread count")
        do {
            unreadCount = try await service.getUnreadCount()
            print("✅ [NotificationViewModel] Unread count: \(unreadCount)")
        } catch {
            print("❌ [NotificationViewModel] Error loading unread count: \(error.localizedDescription)")
        }
    }
    
    func markAsRead(notificationId: String) async {
        print("🔄 [NotificationViewModel] Marking notification as read: \(notificationId)")
        do {
            let updated = try await service.markAsRead(notificationId: notificationId)
            if let index = notifications.firstIndex(where: { $0.id == notificationId }) {
                notifications[index] = updated
            }
            await loadUnreadCount()
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
            await loadUnreadCount()
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
            await loadUnreadCount()
            print("✅ [NotificationViewModel] Notification deleted")
        } catch {
            print("❌ [NotificationViewModel] Error deleting notification: \(error.localizedDescription)")
        }
    }
}

