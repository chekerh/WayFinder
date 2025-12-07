//
//  WayFinderApp.swift
//  WayFinder
//
//  Created by sarrachmek on 4/11/2025.
//



import SwiftUI
import FirebaseCore

@main // Cela marque le point d'entrée principal de l'application
struct WayFinderApp: App {
    @StateObject private var languageManager = LanguageManager()
    @UIApplicationDelegateAdaptor(WayFinderAppDelegate.self) var delegate
    
    init() {
        // Configure Firebase early in app initialization (before any views load)
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
            print("✅ [WayFinderApp] Firebase configured in app init")
        }
    }
    
    var body: some Scene {
        WindowGroup {
            SplashScreenView() // Affiche la splash screen en premier
                .environmentObject(languageManager)
                .environment(\.locale, languageManager.locale)
                .environment(\.layoutDirection, languageManager.layoutDirection)
                .id(languageManager.selectedLanguage.id) // Force la recréation de la vue quand la langue change
                .onAppear {
                    // Initialize FCM service when app appears
                    Task { @MainActor in
                        await FirebaseMessagingService.shared.initialize()
                    }
                    
                    // Listen for FCM notification taps
                    setupNotificationNavigationListener()
                    
                    // Check for notifications when app becomes active
                    setupAppStateObserver()
                }
        }
    }
    
    private func setupNotificationNavigationListener() {
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("FCMNotificationTapped"),
            object: nil,
            queue: .main
        ) { notification in
            guard let userInfo = notification.userInfo else { return }
            
            let type = userInfo["type"] as? String ?? ""
            let actionUrl = userInfo["actionUrl"] as? String ?? ""
            let postId = userInfo["postId"] as? String
            let bookingId = userInfo["bookingId"] as? String
            let journeyId = userInfo["journeyId"] as? String
            
            print("🧭 [WayFinderApp] Handling notification navigation")
            print("   Type: \(type)")
            print("   ActionUrl: \(actionUrl)")
            print("   PostId: \(postId ?? "nil")")
            print("   BookingId: \(bookingId ?? "nil")")
            print("   JourneyId: \(journeyId ?? "nil")")
            
            // Handle navigation based on notification type
            // The actual navigation will be handled in the views that listen to this
            // For now, we just log it - views can observe this notification to navigate
        }
    }
    
    private func setupAppStateObserver() {
        // Check for new notifications when app becomes active
        NotificationCenter.default.addObserver(
            forName: UIApplication.didBecomeActiveNotification,
            object: nil,
            queue: .main
        ) { _ in
            print("🔄 [WayFinderApp] App became active - checking for new notifications")
            FirebaseMessagingService.shared.checkForNewNotifications()
        }
    }
}
