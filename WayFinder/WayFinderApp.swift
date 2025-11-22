//
//  WayFinderApp.swift
//  WayFinder
//
//  Created by sarrachmek on 4/11/2025.
//



import SwiftUI

@main // Cela marque le point d'entrée principal de l'application
struct WayFinderApp: App {
    @StateObject private var languageManager = LanguageManager()
    
    var body: some Scene {
        WindowGroup {
            SplashScreenView() // Affiche la splash screen en premier
                .environmentObject(languageManager)
                .environment(\.locale, languageManager.locale)
                .environment(\.layoutDirection, languageManager.layoutDirection)
                .id(languageManager.selectedLanguage.id) // Force la recréation de la vue quand la langue change
        }
    }
}
