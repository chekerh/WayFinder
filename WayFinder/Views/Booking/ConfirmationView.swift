//
//  ConfirmationView.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

struct ConfirmationView: View {
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                title
                trustBadge
                confirmationCard
                actions
                Spacer()
                footerButtons
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 12)
        }
        .navigationBarHidden(true)
    }
    
    private var title: some View {
        HStack {
            Text("Confirmation")
                .font(.system(size: 24, weight: .bold))
                .underline(true, color: ThemeColors.primaryText(colorScheme))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 28, weight: .semibold))
                .foregroundColor(.yellow)
        }
    }
    
    private var trustBadge: some View {
        HStack {
            Text("Merci Pour Votre ")
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Text("Confiance").foregroundColor(.green)
            Spacer()
        }
        .font(.subheadline)
        .padding(.vertical, 10)
        .padding(.horizontal, 12)
        .background(RoundedRectangle(cornerRadius: 12).fill(ThemeColors.surface(colorScheme)))
    }
    
    private var confirmationCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image("gemini")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 22)
                Spacer(minLength: 0)
            }
            
            Text("Réservation confirmée !")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(Color.blue)
            
            HStack(spacing: 8) {
                Image(systemName: "airplane")
                Text("Paris → Rome")
            }
            .font(.subheadline)
            .foregroundStyle(ThemeColors.primaryText(colorScheme))
            
            VStack(alignment: .leading, spacing: 4) {
                Text("Compagnie: Air France")
                HStack(spacing: 12) {
                    Text("Date: ")
                        .foregroundColor(.secondary) + Text("22 Oct 2025").foregroundColor(.red)
                    Text("•")
                    Text("Siège: ")
                        .foregroundColor(.secondary) + Text("14A").foregroundColor(.red)
                }
            }
            .font(.footnote)
            .foregroundStyle(ThemeColors.primaryText(colorScheme))
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 16).fill(ThemeColors.surface(colorScheme)))
    }
    
    private var actions: some View {
        VStack(spacing: 14) {
            HStack(spacing: 14) {
                Button(action: {}) {
                    Text("Voir mes\nréservations")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.white)
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .background(ThemeColors.accent())
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                
                Button(action: {}) {
                    Text("Terminer")
                        .foregroundColor(.black)
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .background(Color.yellow)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            
            Button(action: {}) {
                Text("Donner un feedback")
                    .foregroundColor(.white)
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .background(ThemeColors.accent())
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }
    
    private var footerButtons: some View {
        VStack(spacing: 16) {
            Image("gemini")
                .resizable()
                .scaledToFit()
                .frame(height: 36)
                .opacity(0.9)
            
            HStack(spacing: 16) {
            Button(action: {}) {
                Text("Decline")
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .background(Color.pink.opacity(0.3))
            .clipShape(Capsule())
            
            Button(action: {}) {
                HStack {
                    Text("Buy $1,200.00")
                    Image(systemName: "chevron.right")
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            }
            .background(ThemeColors.accent())
            .clipShape(Capsule())
            }
        }
    }
}

struct ConfirmationView_Previews: PreviewProvider {
    static var previews: some View {
        ConfirmationView()
    }
}


