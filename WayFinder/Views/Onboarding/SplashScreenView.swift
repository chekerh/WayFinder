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
    @State private var showWelcomeView = false // Variable d'état pour passer à l'écran de bienvenue
    
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
                            
                            Text("findr")
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
            // Lancer l'écran de bienvenue après 3 secondes
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                showWelcomeView = true
            }
        }
        .fullScreenCover(isPresented: $showWelcomeView) {
            WelcomeView()
        }
    }
}

struct SplashScreenView_Previews: PreviewProvider {
    static var previews: some View {
        SplashScreenView() // Affiche un aperçu de la Splash Screen
    }
}

