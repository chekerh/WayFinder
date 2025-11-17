//
//  ChatPacksView.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

struct ChatPacksView: View {
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                header
                geminiBox
                packsList
                Spacer(minLength: 0)
                actionButton
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 12)
        }
        .navigationBarHidden(true)
    }
    
    private var header: some View {
        HStack {
            Text("Chat – Packs")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
            Image(systemName: "cpu")
                .font(.system(size: 24, weight: .semibold))
                .foregroundColor(ThemeColors.accent())
        }
    }
    
    private var geminiBox: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Image("gemini")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 20)
            }
            
            VStack(alignment: .leading, spacing: 12) {
                Text("Bonjour ! Souhaitez‑vous\nun pack personnalisé ?")
                    .font(.subheadline)
                    .foregroundColor(.white)
                    .padding(.vertical, 10)
                    .padding(.horizontal, 12)
                    .background(Color.red)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                
                Button(action: {}) {
                    Text("Oui, proposez moi un pack")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color.red)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
        )
    }
    
    private var packsList: some View {
        VStack(spacing: 16) {
            PackRow(title: "Pack 1: Madrid → Berlin → Maison", price: 99, isHighlighted: false)
            PackRow(title: "Pack 2: Tunis → Lyon → Auberge", price: 159, isHighlighted: true)
            PackRow(title: "Pack 2: Paris → Rome → Hôtel", price: 159, isHighlighted: false)
        }
    }
    
    private var actionButton: some View {
        Button(action: {}) {
            Text("Consulter Gemini")
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
        }
        .background(ThemeColors.accent())
        .clipShape(Capsule())
    }
}

private struct PackRow: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let price: Int
    let isHighlighted: Bool
    
    var body: some View {
        HStack {
            Text(title)
                .font(.subheadline)
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
            Text("\(price)TND")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(ThemeColors.accent())
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(isHighlighted ? ThemeColors.accent() : Color.clear, lineWidth: 2)
                )
        )
    }
}

struct ChatPacksView_Previews: PreviewProvider {
    static var previews: some View {
        ChatPacksView()
    }
}


