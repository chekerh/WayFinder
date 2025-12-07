//
//  Welcome.swift
//  WayFinder
//
//  Created by sarrachmek on 6/11/2025.
//

import SwiftUI

struct WelcomeView: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var navigateToLogin = false
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Spacer(minLength: 16)
                    
                    illustrationCard
                        .padding(.horizontal, 24)
                    
                    VStack(spacing: 12) {
                        Text("welcome_back_title")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 16)
                        
                        Text("welcome_back_body")
                            .font(.system(size: 15))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .multilineTextAlignment(.center)
                            .lineSpacing(4)
                            .padding(.horizontal, 32)
                    }
                    
                    NavigationLink(value: "login") {
                        EmptyView()
                    }
                    .hidden()
                    .navigationDestination(item: Binding(
                        get: { navigateToLogin ? "login" : nil },
                        set: { navigateToLogin = $0 != nil }
                    )) { _ in
                        LoginView()
                    }
                    
                    Button {
                        navigateToLogin = true
                    } label: {
                        Text("welcome_button")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color(red: 1.0, green: 0.75, blue: 0.0))
                            .clipShape(Capsule())
                            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.08), radius: 12, x: 0, y: 6)
                    }
                    .padding(.horizontal, 48)
                    .padding(.top, 8)
                    
                    Spacer()
                }
                .padding(.top, 32)
                .padding(.bottom, 40)
            }
            .navigationBarTitleDisplayMode(.inline)
        }
    }
    
    private var illustrationCard: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 40, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.4 : 0.06), radius: 14, x: 0, y: 8)
                .frame(height: 420)
            
            VStack {
                HStack {
                    Spacer()
                    
                    Image("LogoWayFinder")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 44, height: 44)
                }
                .padding(.top, 32)
                .padding(.horizontal, 28)
                
                Image("ic_travel")
                    .resizable()
                    .scaledToFit()
                    .padding(.horizontal, 32)
                    .padding(.top, 12)
                    .padding(.bottom, 20)
                
                Spacer()
            }
        }
    }
}

struct WelcomeView_Previews: PreviewProvider {
    static var previews: some View {
        WelcomeView()
    }
}
