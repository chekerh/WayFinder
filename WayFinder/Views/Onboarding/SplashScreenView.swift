//
//  SplashScreenView.swift
//  WayFinder
//
//  Created by sarrachmek on 4/11/2025.
//

import Foundation
import SwiftUI

struct SplashScreenView: View {
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var languageManager: LanguageManager
    @State private var showWelcomeView = false // Variable d'état pour passer à l'écran de bienvenue
    @State private var navigateToHome = false // Variable d'état pour naviguer vers HomeScreen si connecté
    
    var body: some View {
        ZStack {
            ThemeColors.accentGradient(colorScheme)
            .ignoresSafeArea()
            
            VStack {
                Spacer()
                
                VStack(spacing: 24) {
                    HStack(spacing: 12) {
                        Spacer()
                            .frame(width: 60)
                        
                        HStack(spacing: 0) {
                            Text("Way")
                                .font(.system(size: 44, weight: .bold))
                                .foregroundColor(.red)
                            
                            Text("finder")
                                .font(.system(size: 44, weight: .bold))
                                .foregroundColor(Color(red: 1.0, green: 0.78, blue: 0.09))
                        }
                        
                        Image("LogoWayFinder.png")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 60, height: 60)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    
                    Text("splash_subheadline")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 32)
                
                Spacer()
            }
        }
        .onAppear {
            // Vérifier si l'utilisateur est déjà connecté
            if TokenStorage.fetch() != nil {
                // Si un token existe, naviguer directement vers HomeScreen après un court délai
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    navigateToHome = true
                }
            } else {
                // Sinon, lancer l'écran de bienvenue après 3 secondes
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                    showWelcomeView = true
                }
            }
        }
        .fullScreenCover(isPresented: $showWelcomeView) {
            WelcomeView()
        }
        .fullScreenCover(isPresented: $navigateToHome) {
            NavigationStack {
                HomeScreen()
                    .navigationBarBackButtonHidden(true)
                    .environmentObject(languageManager)
                    .environment(\.locale, languageManager.locale)
                    .environment(\.layoutDirection, languageManager.layoutDirection)
            }
        }
    }
}

struct SplashScreenView_Previews: PreviewProvider {
    static var previews: some View {
        SplashScreenView() // Affiche un aperçu de la Splash Screen
    }
}

