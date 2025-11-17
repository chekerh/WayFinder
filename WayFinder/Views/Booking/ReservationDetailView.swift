//
//  ReservationDetailView.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

struct ReservationDetailView: View {
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(alignment: .leading, spacing: 20) {
                header
                
                infoCard
                
                VStack(alignment: .leading, spacing: 12) {
                    Text("Options:")
                        .font(.headline)
                        .foregroundColor(ThemeColors.accent())
                    Text("Bagages en soute: +30 TND")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("Stage prioritaire: +15 TND")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                }
                
                recommendationCard
                
                Spacer(minLength: 0)
                
                Button(action: {}) {
                    Text("Réserver maintenant")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .background(ThemeColors.accent())
                .clipShape(Capsule())
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 12)
        }
        .navigationBarHidden(true)
    }
    
    private var header: some View {
        HStack {
            Text("Détails de ta réservation")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
            Image(systemName: "creditcard.fill")
                .font(.system(size: 28, weight: .semibold))
                .foregroundColor(ThemeColors.accent())
        }
    }
    
    private var infoCard: some View {
        HStack {
            Text("100 TND – Vol + AR + Bagage à main (10kg)")
                .font(.subheadline)
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(ThemeColors.surface(colorScheme)))
    }
    
    private var recommendationCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Recommandations basées sur vos préférences")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(ThemeColors.accent())
            HStack(spacing: 4) {
                Text("Meilleur").foregroundColor(.red)
                Text("rapport qualité‑prix")
                Text("85%").foregroundColor(.red)
                Text("des voyageurs recommandent Vol généralement à l'heure")
            }
            .font(.footnote)
            .foregroundStyle(ThemeColors.primaryText(colorScheme))
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
        )
    }
}

struct ReservationDetailView_Previews: PreviewProvider {
    static var previews: some View {
        ReservationDetailView()
    }
}


