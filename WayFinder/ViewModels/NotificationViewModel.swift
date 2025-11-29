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
            let previousIds = Set(notifications.map { $0.id })
            var loadedNotifications = try await service.getNotifications()
            
            // Dédupliquer les notifications d'annulation par bookingId
            // Garder seulement la notification la plus récente pour chaque bookingId annulé
            var cancellationNotificationsByBookingId: [String: Notification] = [:]
            var deduplicatedNotifications: [Notification] = []
            
            for notification in loadedNotifications {
                // Pour les notifications d'annulation, garder seulement la plus récente par bookingId
                if notification.type == .bookingCancelled,
                   let bookingId = notification.data?.bookingId {
                    if let existing = cancellationNotificationsByBookingId[bookingId] {
                        // Comparer les dates pour garder la plus récente
                        let existingDate = existing.createdAt ?? Date.distantPast
                        let currentDate = notification.createdAt ?? Date.distantPast
                        if currentDate > existingDate {
                            // Remplacer par la notification plus récente
                            cancellationNotificationsByBookingId[bookingId] = notification
                            print("⚠️ [NotificationViewModel] Replacing older cancellation notification for booking \(bookingId) with newer one")
                        } else {
                            // Ignorer cette notification car on a déjà une notification plus récente
                            print("⚠️ [NotificationViewModel] Duplicate cancellation notification for booking \(bookingId), skipping (older than existing)")
                        }
                    } else {
                        // Première notification d'annulation pour ce bookingId
                        cancellationNotificationsByBookingId[bookingId] = notification
                    }
                } else {
                    // Notification normale (pas d'annulation), l'ajouter directement
                    deduplicatedNotifications.append(notification)
                }
            }
            
            // Ajouter les notifications d'annulation dédupliquées (une seule par bookingId)
            for (_, notification) in cancellationNotificationsByBookingId {
                deduplicatedNotifications.append(notification)
            }
            
            if loadedNotifications.count != deduplicatedNotifications.count {
                print("✅ [NotificationViewModel] Deduplicated \(loadedNotifications.count - deduplicatedNotifications.count) duplicate cancellation notifications")
            }
            
            notifications = deduplicatedNotifications
            print("✅ [NotificationViewModel] Loaded \(notifications.count) notifications (after deduplication)")
            
            // Check for new notifications and trigger popup display immediately
            let currentIds = Set(notifications.map { $0.id })
            let newIds = currentIds.subtracting(previousIds)
            
            if !newIds.isEmpty {
                print("🆕 [NotificationViewModel] Found \(newIds.count) new notification(s), triggering popup display")
                // Notify FirebaseMessagingService to check for new notifications immediately
                FirebaseMessagingService.shared.checkForNewNotifications()
            }
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
    
    func deleteAllNotifications() async {
        print("🔄 [NotificationViewModel] Deleting all notifications")
        do {
            try await service.deleteAllNotifications()
            notifications.removeAll()
            print("✅ [NotificationViewModel] All notifications deleted")
        } catch {
            print("❌ [NotificationViewModel] Error deleting all notifications: \(error.localizedDescription)")
            errorMessage = "Erreur lors de la suppression des notifications"
        }
    }
}

