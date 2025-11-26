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
        
        // Configure Firebase if not already configured
        if FirebaseApp.app() == nil {
            // Firebase will be configured via GoogleService-Info.plist
            // Make sure the file is added to the project
            FirebaseApp.configure()
            print("✅ [FCM] Firebase configured")
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
        lastNotificationIds.removeAll()
        lastNotificationCheck = Date()
        print("⏸️ [FCM] Stopped notification polling - user left main interface")
        print("🔄 [FCM] Reset displayed notifications list")
    }
    
    private var lastNotificationCheck: Date = Date()
    private var lastNotificationIds: Set<String> = []
    // Track notifications that have already been displayed to prevent duplicates
    private var displayedNotificationIds: Set<String> = []
    
    // Public method to manually trigger notification check (called when app becomes active or user loads notifications)
    func checkForNewNotifications() {
        Task { @MainActor in
            await checkAndDisplayNewNotifications()
        }
    }
    
    private func checkAndDisplayNewNotifications() async {
        // Check if user is logged in and in main interface
        guard TokenStorage.fetch() != nil else {
            print("⚠️ [FCM] User not logged in, skipping notification check")
            return
        }
        
        guard isInMainInterface else {
            print("⚠️ [FCM] User not in main interface, skipping notification check")
            return
        }
        
        do {
            let notifications = try await NotificationService.shared.getNotifications()
            let unreadNotifications = notifications.filter { !$0.isRead }
            
            print("🔍 [FCM] Checking notifications - Total: \(notifications.count), Unread: \(unreadNotifications.count)")
            print("🔍 [FCM] Last check: \(lastNotificationCheck), Last known IDs: \(lastNotificationIds.count), Already displayed: \(displayedNotificationIds.count)")
            
            // Get current notification IDs
            let currentNotificationIds = Set(unreadNotifications.map { $0.id })
            
            // Find new notifications that we haven't shown yet
            // A notification is "new" if:
            // 1. It's a new ID we haven't seen before (not in lastNotificationIds)
            // 2. AND we haven't already displayed it (not in displayedNotificationIds)
            // 3. AND it was created recently (within last 10 minutes) OR it's the first check
            let newNotifications = unreadNotifications.filter { notification in
                let isNewId = !lastNotificationIds.contains(notification.id)
                let notAlreadyDisplayed = !displayedNotificationIds.contains(notification.id)
                let createdAt = notification.createdAt ?? Date()
                // Check if notification was created in the last 10 minutes
                let isRecent = createdAt > lastNotificationCheck.addingTimeInterval(-600)
                
                // Only show if it's a new ID, not already displayed, and (recent or first check)
                let shouldDisplay = isNewId && notAlreadyDisplayed && (lastNotificationIds.isEmpty || isRecent)
                
                if shouldDisplay {
                    print("🆕 [FCM] New notification to display: \(notification.id) - \(notification.title)")
                    print("   Created: \(createdAt), Last check: \(lastNotificationCheck)")
                    print("   Is new ID: \(isNewId), Not displayed: \(notAlreadyDisplayed), Is recent: \(isRecent)")
                }
                
                return shouldDisplay
            }
            
            print("📬 [FCM] Found \(newNotifications.count) new notification(s) to display")
            
            // Display each new notification as a local notification popup
            for notification in newNotifications {
                print("📤 [FCM] Displaying notification: \(notification.title)")
                await displayLocalNotification(for: notification)
                // Mark as displayed immediately to prevent duplicate displays
                displayedNotificationIds.insert(notification.id)
            }
            
            // Update last check and IDs AFTER displaying notifications
            lastNotificationCheck = Date()
            lastNotificationIds = currentNotificationIds
            
            // Clean up old displayed IDs (keep only those from last 24 hours to prevent memory growth)
            // This allows re-displaying if notification becomes unread again after being read
            let cutoffDate = Date().addingTimeInterval(-86400) // 24 hours ago
            // We'll clean this up periodically, but for now just keep all displayed IDs
            
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
        
        // Show notification immediately
        let request = UNNotificationRequest(
            identifier: "\(notification.id)-\(UUID().uuidString)", // Unique identifier to allow multiple notifications
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

