//
//  PersonalizedResultsView.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

struct PersonalizedResultsView: View {
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                header
                
                VStack(alignment: .leading, spacing: 12) {
                    Text("Voyage à Paris – Vols + Hôtel")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    ResultRow(title: "100 TND – Vol + Hôtel (130€)", price: "100 TND")
                    ResultRow(title: "250 TND – Vol + Hôtel (2 nuits)", price: "250 TND")
                    ResultRow(title: "50 TND – Transport de l'aéroport", price: "50 TND", isHighlighted: true)
                }
                
                Spacer(minLength: 0)
                
                Button(action: {}) {
                    Text("Voir plus d'options")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .background(ThemeColors.accent())
                .clipShape(Capsule())
                .padding(.horizontal, 24)
                .padding(.bottom, 12)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
        }
        .navigationBarHidden(true)
    }
    
    private var header: some View {
        HStack {
            Text("Résultats pour toi")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
            Image(systemName: "wallet.pass.fill")
                .font(.system(size: 28, weight: .semibold))
                .foregroundColor(ThemeColors.accent())
        }
    }
}

private struct ResultRow: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let price: String
    var isHighlighted: Bool = false
    
    var body: some View {
        HStack {
            Text(title)
                .font(.subheadline)
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
            Text(price)
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

struct PersonalizedResultsView_Previews: PreviewProvider {
    static var previews: some View {
        PersonalizedResultsView()
    }
}


