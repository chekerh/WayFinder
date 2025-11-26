import UIKit
import FirebaseCore
import FirebaseMessaging

class WayFinderAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        // Initialize Firebase
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        
        // Initialize FCM service
        Task { @MainActor in
            FirebaseMessagingService.shared.initialize()
        }
        
        return true
    }
    
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        FirebaseMessagingService.shared.application(application, didRegisterForRemoteNotificationsWithDeviceToken: deviceToken)
    }
    
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        FirebaseMessagingService.shared.application(application, didFailToRegisterForRemoteNotificationsWithError: error)
    }
    
    // Handle notification when app is in background or closed
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        print("📬 [AppDelegate] Received remote notification in background: \(userInfo)")
        
        // Let Firebase handle the notification
        Messaging.messaging().appDidReceiveMessage(userInfo)
        
        // Ensure notification is displayed even in background
        // iOS will automatically show the notification if it has the "notification" field in FCM payload
        
        completionHandler(.newData)
    }
    
    // Handle notification when app is opened from a notification
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any]
    ) {
        print("📬 [AppDelegate] Received remote notification (iOS 9): \(userInfo)")
        Messaging.messaging().appDidReceiveMessage(userInfo)
    }
}

