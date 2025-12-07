import Foundation
import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging

@MainActor
final class FirebaseMessagingService: NSObject, ObservableObject {
    static let shared = FirebaseMessagingService()
    
    @Published var fcmToken: String?
    private var isInitialized = false
    private var isInMainInterface = false // Track if user is in main interface (not login/onboarding)
    
    private override init() {
        super.init()
    }
    
    func initialize() {
        guard !isInitialized else { return }
        
        // Firebase should already be configured in app init or AppDelegate
        // Just verify it's configured
        if FirebaseApp.app() == nil {
            print("⚠️ [FCM] Firebase not configured yet, configuring now...")
            FirebaseApp.configure()
            print("✅ [FCM] Firebase configured")
        } else {
            print("✅ [FCM] Firebase already configured")
        }
        
        // Set messaging delegate
        Messaging.messaging().delegate = self
        
        // Set UNUserNotificationCenter delegate
        UNUserNotificationCenter.current().delegate = self
        
        // Request notification permissions first
        // The FCM token will be requested after APNs token is received
        requestNotificationPermissions()
        
        isInitialized = true
        print("✅ [FCM] Firebase Messaging Service initialized")
    }
    
    private func requestNotificationPermissions() {
        // Request all notification permissions including provisional for better popup display
        let options: UNAuthorizationOptions = [.alert, .badge, .sound, .provisional]
        
        UNUserNotificationCenter.current().requestAuthorization(options: options) { [weak self] granted, error in
            if let error = error {
                print("❌ [FCM] Error requesting notification permissions: \(error.localizedDescription)")
                return
            }
            
            if granted {
                print("✅ [FCM] Notification permissions granted")
                
                // Get current notification settings to verify
                UNUserNotificationCenter.current().getNotificationSettings { settings in
                    print("📱 [FCM] Notification settings - Alert: \(settings.alertSetting.rawValue), Badge: \(settings.badgeSetting.rawValue), Sound: \(settings.soundSetting.rawValue)")
                }
                
                // Don't start polling here - it will be started when user enters main interface
                // This prevents notifications from showing during login/onboarding
                print("ℹ️ [FCM] Notification polling will start when user enters main interface")
            } else {
                print("⚠️ [FCM] Notification permissions denied")
            }
        }
    }
    
    private var pollingTimer: Timer?
    
    // Public method to enable/disable notification polling based on app state
    func setMainInterfaceState(_ isInMainInterface: Bool) {
        self.isInMainInterface = isInMainInterface
        
        if isInMainInterface {
            // Start polling when user enters main interface
            startNotificationPolling()
        } else {
            // Stop polling when user leaves main interface (login/onboarding)
            stopNotificationPolling()
        }
    }
    
    // Poll for new notifications and display them as local notifications
    private func startNotificationPolling() {
        // Stop any existing timer
        pollingTimer?.invalidate()
        
        // Only start polling if user is logged in and in main interface
        guard TokenStorage.fetch() != nil, isInMainInterface else {
            print("⚠️ [FCM] Not starting polling - user not logged in or not in main interface")
            return
        }
        
        print("✅ [FCM] Starting notification polling - user is in main interface")
        
        // Check for new notifications every 2 seconds when app is active for faster detection
        pollingTimer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { [weak self] _ in
            Task { @MainActor in
                await self?.checkAndDisplayNewNotifications()
            }
        }
        
        // Also check immediately
        Task { @MainActor in
            await checkAndDisplayNewNotifications()
        }
    }
    
    private func stopNotificationPolling() {
        pollingTimer?.invalidate()
        pollingTimer = nil
        // Reset displayed notifications when user leaves main interface (logout)
        // This allows notifications to be shown again if user logs back in
        displayedNotificationIds.removeAll()
        displayedCancellationBookingIds.removeAll()
        lastNotificationIds.removeAll()
        lastNotificationCheck = Date()
        print("⏸️ [FCM] Stopped notification polling - user left main interface")
        print("🔄 [FCM] Reset displayed notifications list")
    }
    
    private var lastNotificationCheck: Date = Date()
    private var lastNotificationIds: Set<String> = []
    // Track notifications that have already been displayed to prevent duplicates
    private var displayedNotificationIds: Set<String> = []
    // Track cancellation notifications by bookingId to prevent duplicates for the same booking
    private var displayedCancellationBookingIds: Set<String> = []
    // Track if we're currently processing notifications to prevent concurrent processing
    private var isProcessingNotifications = false
    // Minimum time between notification checks (prevent too frequent polling)
    private let minTimeBetweenChecks: TimeInterval = 1.0
    private var lastCheckTime: Date = Date.distantPast
    
    // Public method to manually trigger notification check (called when app becomes active or user loads notifications)
    func checkForNewNotifications() {
        Task { @MainActor in
            await checkAndDisplayNewNotifications()
        }
    }
    
    private func checkAndDisplayNewNotifications() async {
        // Prevent concurrent processing of notifications
        guard !isProcessingNotifications else {
            print("⚠️ [FCM] Already processing notifications, skipping concurrent check")
            return
        }
        
        // Prevent too frequent checks (minimum 1 second between checks)
        let now = Date()
        let timeSinceLastCheck = now.timeIntervalSince(lastCheckTime)
        guard timeSinceLastCheck >= minTimeBetweenChecks else {
            print("⚠️ [FCM] Too soon since last check (\(timeSinceLastCheck)s), skipping")
            return
        }
        
        // Check if user is logged in and in main interface
        guard TokenStorage.fetch() != nil else {
            print("⚠️ [FCM] User not logged in, skipping notification check")
            return
        }
        
        guard isInMainInterface else {
            print("⚠️ [FCM] User not in main interface, skipping notification check")
            return
        }
        
        isProcessingNotifications = true
        lastCheckTime = now
        defer { isProcessingNotifications = false }
        
        do {
            let notifications = try await NotificationService.shared.getNotifications()
            
            // Dédupliquer les notifications d'annulation par bookingId AVANT de filtrer les non lues
            // Garder seulement la notification la plus récente pour chaque bookingId annulé
            var cancellationNotificationsByBookingId: [String: Notification] = [:]
            var deduplicatedNotifications: [Notification] = []
            
            for notification in notifications {
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
                            print("⚠️ [FCM] Replacing older cancellation notification for booking \(bookingId) with newer one")
                        } else {
                            // Ignorer cette notification car on a déjà une notification plus récente
                            print("⚠️ [FCM] Duplicate cancellation notification for booking \(bookingId), skipping (older than existing)")
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
            
            if notifications.count != deduplicatedNotifications.count {
                print("✅ [FCM] Deduplicated \(notifications.count - deduplicatedNotifications.count) duplicate cancellation notifications")
            }
            
            // Maintenant filtrer les non lues après déduplication
            let unreadNotifications = deduplicatedNotifications.filter { !$0.isRead }
            
            print("🔍 [FCM] Checking notifications - Total: \(deduplicatedNotifications.count), Unread: \(unreadNotifications.count)")
            print("🔍 [FCM] Last check: \(lastNotificationCheck), Last known IDs: \(lastNotificationIds.count), Already displayed: \(displayedNotificationIds.count)")
            
            // Get current notification IDs
            let currentNotificationIds = Set(unreadNotifications.map { $0.id })
            
            // Find new notifications that we haven't shown yet
            // A notification is "new" if:
            // 1. It's a new ID we haven't seen before (not in lastNotificationIds)
            // 2. AND we haven't already displayed it (not in displayedNotificationIds)
            // 3. AND it was created recently (within last 10 minutes) OR it's the first check
            // 4. For cancellation notifications: check if we've already displayed one for this bookingId
            
            // First pass: filter by basic criteria AND check cancellation bookingIds BEFORE filtering
            // This prevents race conditions where multiple cancellation notifications pass through
            let candidateNotifications = unreadNotifications.filter { notification in
                let isNewId = !lastNotificationIds.contains(notification.id)
                let notAlreadyDisplayed = !displayedNotificationIds.contains(notification.id)
                let createdAt = notification.createdAt ?? Date()
                // Check if notification was created in the last 10 minutes
                let isRecent = createdAt > lastNotificationCheck.addingTimeInterval(-600)
                
                // CRITICAL: For cancellation notifications, check bookingId FIRST before other criteria
                // This prevents multiple cancellation notifications for same booking from passing through
                if notification.type == .bookingCancelled,
                   let bookingId = notification.data?.bookingId {
                    // If we've already displayed a cancellation for this booking, skip immediately
                    if displayedCancellationBookingIds.contains(bookingId) {
                        print("⚠️ [FCM] Cancellation notification for booking \(bookingId) already displayed, skipping at first pass")
                        return false
                    }
                }
                
                return isNewId && notAlreadyDisplayed && (lastNotificationIds.isEmpty || isRecent)
            }
            
            // Second pass: additional deduplication for cancellation notifications within current batch
            // Track bookingIds we've seen in this batch to prevent duplicates
            var seenBookingIdsInBatch: Set<String> = []
            let newNotifications = candidateNotifications.filter { notification in
                // Special handling for cancellation notifications: check bookingId
                if notification.type == .bookingCancelled,
                   let bookingId = notification.data?.bookingId {
                    // Double-check if we've already displayed a cancellation notification for this booking
                    // (shouldn't happen after first pass, but extra safety)
                    if displayedCancellationBookingIds.contains(bookingId) {
                        print("⚠️ [FCM] Cancellation notification for booking \(bookingId) already displayed previously, skipping duplicate")
                        return false
                    }
                    // Check if we've seen this bookingId in the current batch
                    if seenBookingIdsInBatch.contains(bookingId) {
                        print("⚠️ [FCM] Cancellation notification for booking \(bookingId) already in current batch, skipping duplicate")
                        return false
                    }
                    seenBookingIdsInBatch.insert(bookingId)
                }
                
                print("🆕 [FCM] New notification to display: \(notification.id) - \(notification.title)")
                return true
            }
            
            print("📬 [FCM] Found \(newNotifications.count) new notification(s) to display")
            
            // IMPORTANT: Final deduplication pass - ensure only one cancellation per bookingId
            // Also mark bookingIds as displayed BEFORE processing to prevent race conditions
            var finalNotifications: [Notification] = []
            var finalSeenBookingIds: Set<String> = []
            
            // CRITICAL: Mark cancellation bookingIds as displayed IMMEDIATELY before processing
            // This prevents race conditions where multiple calls to checkAndDisplayNewNotifications()
            // process the same notifications simultaneously
            for notification in newNotifications {
                if notification.type == .bookingCancelled,
                   let bookingId = notification.data?.bookingId {
                    // If we've already added a cancellation for this bookingId in this batch, skip
                    if finalSeenBookingIds.contains(bookingId) {
                        print("⚠️ [FCM] Final deduplication: Skipping duplicate cancellation for booking \(bookingId)")
                        continue
                    }
                    // Mark as displayed IMMEDIATELY to prevent other concurrent calls from processing it
                    displayedCancellationBookingIds.insert(bookingId)
                    finalSeenBookingIds.insert(bookingId)
                    print("✅ [FCM] Pre-marked cancellation notification for booking: \(bookingId) (before display)")
                }
                // Mark notification ID as displayed immediately
                displayedNotificationIds.insert(notification.id)
                finalNotifications.append(notification)
            }
            
            if newNotifications.count != finalNotifications.count {
                print("⚠️ [FCM] Final deduplication removed \(newNotifications.count - finalNotifications.count) duplicate notifications")
            }
            
            // Display each new notification as a local notification popup
            for notification in finalNotifications {
                print("📤 [FCM] Displaying notification: \(notification.id) - \(notification.title)")
                if let bookingId = notification.data?.bookingId {
                    print("   BookingId: \(bookingId)")
                }
                await displayLocalNotification(for: notification)
            }
            
            // Update last check and IDs AFTER displaying notifications
            lastNotificationCheck = Date()
            lastNotificationIds = currentNotificationIds
            
            // Note: displayed IDs are cleaned up on logout (see clearOnLogout method)
            // This keeps memory usage reasonable while allowing proper deduplication
            
            if !newNotifications.isEmpty {
                print("✅ [FCM] Successfully displayed \(newNotifications.count) new notification(s) as popup")
                print("📱 [FCM] Notification IDs: \(newNotifications.map { $0.id })")
            } else {
                print("ℹ️ [FCM] No new notifications to display")
            }
        } catch {
            print("❌ [FCM] Error checking notifications: \(error.localizedDescription)")
        }
    }
    
    private func displayLocalNotification(for notification: Notification) async {
        let content = UNMutableNotificationContent()
        
        // Extract title and body from notification
        content.title = notification.title
        content.body = notification.message
        content.sound = .default
        content.badge = NSNumber(value: UIApplication.shared.applicationIconBadgeNumber + 1)
        
        // Add notification data for navigation
        content.userInfo["notificationId"] = notification.id
        content.userInfo["type"] = notification.type.rawValue
        if let actionUrl = notification.actionUrl {
            content.userInfo["actionUrl"] = actionUrl
        }
        
        // Add data from notification.data if available
        if let data = notification.data {
            if let postId = data.postId {
                content.userInfo["postId"] = postId
            }
            if let commentId = data.commentId {
                content.userInfo["commentId"] = commentId
            }
            if let bookingId = data.bookingId {
                content.userInfo["bookingId"] = bookingId
            }
            if let journeyId = data.journeyId {
                content.userInfo["journeyId"] = journeyId
            }
        }
        
        // Create unique identifier for the notification (like Android: use notification.id.hashValue)
        // For cancellation notifications, also use bookingId to prevent duplicates
        // For other notifications, use notification.id.hashValue (like Android)
        let identifier: String
        if notification.type == .bookingCancelled,
           let bookingId = notification.data?.bookingId {
            // Use bookingId as identifier for cancellation notifications to prevent duplicates
            // This is better than Android which only uses notificationId
            identifier = "booking_cancelled_\(bookingId)"
            print("🔑 [FCM] Using bookingId-based identifier for cancellation: \(identifier)")
        } else {
            // For other notifications, use notification.id.hashValue (like Android uses notificationId.hashCode())
            identifier = "notification_\(notification.id.hashValue)"
        }
        
        // Check if a notification with this identifier already exists
        // This prevents duplicate notifications from being scheduled (like Android's behavior)
        let center = UNUserNotificationCenter.current()
        
        // Check both pending and delivered notifications
        let pendingIdentifiers = await center.pendingNotificationRequests().map { $0.identifier }
        let deliveredNotifications = await center.deliveredNotifications()
        let deliveredIdentifiers = deliveredNotifications.map { $0.request.identifier }
        let allIdentifiers = Set(pendingIdentifiers + deliveredIdentifiers)
        
        if allIdentifiers.contains(identifier) {
            print("⚠️ [FCM] Notification with identifier \(identifier) already exists (pending or delivered), skipping duplicate")
            return
        }
        
        // For cancellation notifications, ALWAYS remove any existing notifications with the same bookingId
        // This ensures only one notification is shown per booking cancellation, even if multiple notifications
        // with different IDs are created for the same bookingId
        if notification.type == .bookingCancelled,
           let bookingId = notification.data?.bookingId {
            // Remove any existing cancellation notifications for this bookingId
            let cancellationIdentifier = "booking_cancelled_\(bookingId)"
            
            // Remove delivered notifications
            let deliveredToRemove = deliveredNotifications.filter { $0.request.identifier == cancellationIdentifier }
            if !deliveredToRemove.isEmpty {
                center.removeDeliveredNotifications(withIdentifiers: [cancellationIdentifier])
                print("🗑️ [FCM] Removed \(deliveredToRemove.count) delivered cancellation notification(s) for booking \(bookingId)")
            }
            
            // Remove pending notifications
            let pendingToRemove = pendingIdentifiers.filter { $0 == cancellationIdentifier }
            if !pendingToRemove.isEmpty {
                center.removePendingNotificationRequests(withIdentifiers: [cancellationIdentifier])
                print("🗑️ [FCM] Removed \(pendingToRemove.count) pending cancellation notification(s) for booking \(bookingId)")
            }
            
            // Also check for any notifications that might have been created with notification.id.hashValue
            // and remove them if they're for the same bookingId
            let notificationIdIdentifier = "notification_\(notification.id.hashValue)"
            if allIdentifiers.contains(notificationIdIdentifier) {
                center.removeDeliveredNotifications(withIdentifiers: [notificationIdIdentifier])
                center.removePendingNotificationRequests(withIdentifiers: [notificationIdIdentifier])
                print("🗑️ [FCM] Removed notification with ID-based identifier for booking \(bookingId)")
            }
        }
        
        // Show notification immediately
        let request = UNNotificationRequest(
            identifier: identifier,
            content: content,
            trigger: nil // Show immediately
        )
        
        do {
            try await UNUserNotificationCenter.current().add(request)
            print("✅ [FCM] Local notification displayed: \(notification.title)")
            print("📱 [FCM] Notification body: \(notification.message)")
        } catch {
            print("❌ [FCM] Error displaying local notification: \(error.localizedDescription)")
        }
    }
    
    private func getFCMToken() {
        Messaging.messaging().token { [weak self] token, error in
            if let error = error {
                print("❌ [FCM] Error getting FCM token: \(error.localizedDescription)")
                return
            }
            
            if let token = token {
                print("✅ [FCM] FCM Token received: \(token)")
                Task { @MainActor in
                    self?.fcmToken = token
                    await self?.registerTokenWithBackend(token: token)
                }
            }
        }
    }
    
    func registerTokenWithBackend(token: String) async {
        // Check if user is logged in
        guard TokenStorage.fetch() != nil else {
            print("⚠️ [FCM] User not logged in, token will be registered on next login")
            return
        }
        
        do {
            try await UserService.shared.registerFcmToken(token: token)
            print("✅ [FCM] FCM token registered with backend")
        } catch {
            print("❌ [FCM] Error registering FCM token with backend: \(error.localizedDescription)")
        }
    }
    
    func handleTokenRefresh() {
        getFCMToken()
    }
}

// MARK: - MessagingDelegate
extension FirebaseMessagingService: MessagingDelegate {
    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("🔄 [FCM] FCM registration token refreshed: \(fcmToken ?? "nil")")
        
        guard let token = fcmToken else { return }
        
        Task { @MainActor in
            self.fcmToken = token
            await self.registerTokenWithBackend(token: token)
        }
    }
}

// MARK: - UNUserNotificationCenterDelegate
extension FirebaseMessagingService: UNUserNotificationCenterDelegate {
    // Handle notification when app is in foreground
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        let title = notification.request.content.title
        let body = notification.request.content.body
        
        print("📬 [FCM] Notification received in foreground")
        print("   Title: \(title)")
        print("   Body: \(body)")
        print("   UserInfo: \(userInfo)")
        
        // Extract and log notification data for debugging (on main actor)
        Task { @MainActor in
            self.extractAndLogNotificationData(from: userInfo)
        }
        
        // Show notification even when app is in foreground
        // Use .list and .banner for iOS 14+ to show as popup
        if #available(iOS 14.0, *) {
            completionHandler([.list, .banner, .badge, .sound])
        } else {
            completionHandler([.alert, .badge, .sound])
        }
    }
    
    // Helper method to extract notification data from userInfo
    private func extractAndLogNotificationData(from userInfo: [AnyHashable: Any]) {
        // Extract notification data from different possible locations
        // FCM can send data in different formats: "gcm.notification.title" or "title" or "data.title"
        
        // Convert [AnyHashable: Any] to [String: Any] for easier access
        let stringKeyedUserInfo = Dictionary(uniqueKeysWithValues: userInfo.map { (String(describing: $0.key), $0.value) })
        
        // Try to get type from different possible locations
        let dataPayload = (stringKeyedUserInfo["data"] as? [String: Any]) ?? stringKeyedUserInfo
        let type = dataPayload["type"] as? String ?? stringKeyedUserInfo["type"] as? String
        let actionUrl = dataPayload["actionUrl"] as? String ?? stringKeyedUserInfo["actionUrl"] as? String
        let postId = dataPayload["postId"] as? String ?? stringKeyedUserInfo["postId"] as? String
        let commentId = dataPayload["commentId"] as? String ?? stringKeyedUserInfo["commentId"] as? String
        let bookingId = dataPayload["bookingId"] as? String ?? stringKeyedUserInfo["bookingId"] as? String
        let journeyId = dataPayload["journeyId"] as? String ?? stringKeyedUserInfo["journeyId"] as? String
        
        print("📱 [FCM] Extracted data - Type: \(type ?? "nil"), ActionUrl: \(actionUrl ?? "nil")")
        print("📱 [FCM] IDs - Post: \(postId ?? "nil"), Comment: \(commentId ?? "nil"), Booking: \(bookingId ?? "nil"), Journey: \(journeyId ?? "nil")")
    }
    
    // Handle notification tap
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        print("👆 [FCM] Notification tapped: \(userInfo)")
        
        // Extract notification data (on main actor)
        Task { @MainActor in
            self.handleNotificationTap(userInfo: userInfo)
        }
        
        completionHandler()
    }
    
    private func handleNotificationTap(userInfo: [AnyHashable: Any]) {
        print("🔍 [FCM] Processing notification tap with userInfo: \(userInfo)")
        
        // Convert [AnyHashable: Any] to [String: Any] for easier access
        let stringKeyedUserInfo = Dictionary(uniqueKeysWithValues: userInfo.map { (String(describing: $0.key), $0.value) })
        
        // Extract notification data from different possible locations
        // FCM can send data in "data" field or directly in userInfo
        let dataPayload = (stringKeyedUserInfo["data"] as? [String: Any]) ?? stringKeyedUserInfo
        let gcmData = stringKeyedUserInfo["gcm.notification"] as? [String: Any]
        
        // Extract type from multiple possible locations
        let type = dataPayload["type"] as? String 
            ?? stringKeyedUserInfo["type"] as? String
            ?? gcmData?["type"] as? String
        
        // Extract actionUrl from multiple possible locations
        let actionUrl = dataPayload["actionUrl"] as? String
            ?? stringKeyedUserInfo["actionUrl"] as? String
            ?? gcmData?["actionUrl"] as? String
        
        // Extract additional data for navigation
        let postId = dataPayload["postId"] as? String ?? stringKeyedUserInfo["postId"] as? String
        let commentId = dataPayload["commentId"] as? String ?? stringKeyedUserInfo["commentId"] as? String
        let bookingId = dataPayload["bookingId"] as? String ?? stringKeyedUserInfo["bookingId"] as? String
        let journeyId = dataPayload["journeyId"] as? String ?? stringKeyedUserInfo["journeyId"] as? String
        
        print("📱 [FCM] Extracted - Type: \(type ?? "nil"), ActionUrl: \(actionUrl ?? "nil")")
        print("📱 [FCM] IDs - Post: \(postId ?? "nil"), Comment: \(commentId ?? "nil"), Booking: \(bookingId ?? "nil"), Journey: \(journeyId ?? "nil")")
        
        // Post notification to handle navigation even if actionUrl is missing
        // We can construct it from type and IDs if needed
        var navigationData: [String: Any] = [:]
        
        if let type = type {
            navigationData["type"] = type
        }
        
        if let actionUrl = actionUrl {
            navigationData["actionUrl"] = actionUrl
        } else if let type = type {
            // Construct actionUrl from type and IDs if not provided
            switch type {
            case "post_liked", "post_commented":
                if let postId = postId {
                    navigationData["actionUrl"] = "/post_detail/\(postId)"
                }
            case "booking_confirmed", "booking_cancelled", "booking_updated":
                if let bookingId = bookingId {
                    navigationData["actionUrl"] = "/booking_detail/\(bookingId)"
                }
            case "journey_liked", "journey_commented":
                if let journeyId = journeyId {
                    navigationData["actionUrl"] = "/journey_detail/\(journeyId)"
                }
            default:
                break
            }
        }
        
        // Add all IDs for navigation
        if let postId = postId {
            navigationData["postId"] = postId
        }
        if let commentId = commentId {
            navigationData["commentId"] = commentId
        }
        if let bookingId = bookingId {
            navigationData["bookingId"] = bookingId
        }
        if let journeyId = journeyId {
            navigationData["journeyId"] = journeyId
        }
        
        // Post notification to handle navigation
        NotificationCenter.default.post(
            name: NSNotification.Name("FCMNotificationTapped"),
            object: nil,
            userInfo: navigationData
        )
        
        print("✅ [FCM] Navigation notification posted with data: \(navigationData)")
    }
}

// MARK: - AppDelegate methods (to be called from WayFinderApp)
extension FirebaseMessagingService {
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        print("✅ [FCM] APNs device token received (optional - not required for local notifications)")
        // Note: We're using local notifications instead of push notifications
        // This allows the app to work without a paid Apple Developer account
    }
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("⚠️ [FCM] Remote notifications not available (using local notifications instead)")
        print("ℹ️ [FCM] This is normal when using a free developer account")
        // This is OK - we'll use local notifications instead
    }
}

