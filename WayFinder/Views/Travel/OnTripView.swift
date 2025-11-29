//
//  OnTripView.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

struct OnTripView: View {
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Lors du Voyage")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    gridShortcuts
                    
                    Text("Choisissez votre activité")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    
                    activitiesGrid
                    
                    Button(action: {}) {
                        HStack {
                            Text("Donner un")
                                .foregroundColor(.white)
                            Text("feedback")
                                .foregroundColor(.green)
                                .fontWeight(.bold)
                        }
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                    }
                    .background(ThemeColors.accent())
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 24)
            }
        }
        .navigationBarHidden(true)
    }
    
    private var gridShortcuts: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)], spacing: 16) {
            ShortcutCard(title: "Météo", systemImage: "sun.max")
            ShortcutCard(title: "Alertes", systemImage: "bell.fill")
            ShortcutCard(title: "Tips", systemImage: "lightbulb")
            ShortcutCard(title: "Plans", systemImage: "map.fill")
        }
    }
    
    private var activitiesGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 16)],
                  spacing: 18) {
            ActivityCard(imageName: "Restaurant", title: String(localized: "activity_restaurant"), iconName: "fork.knife.circle")
            ActivityCard(imageName: "cafe", title: String(localized: "activity_cafe"), iconName: "cup.and.saucer.fill")
            ActivityCard(imageName: "cinema", title: String(localized: "activity_cinema"), iconName: "film.fill")
            ActivityCard(imageName: "Outdoors", title: "Outdoors", iconName: "leaf.fill")
            ActivityCard(imageName: "date", title: "Date", iconName: "heart.circle.fill")
            ActivityCard(imageName: "Paddle", title: "Paddle", iconName: "figure.rower")
        }
    }
}

private struct ShortcutCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let systemImage: String
    
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.yellow)
            Text(title)
                .font(.headline)
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
        }
        .padding(18)
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(ThemeColors.surface(colorScheme)))
    }
}

private struct ActivityCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let imageName: String
    let title: String
    let iconName: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ZStack(alignment: .topTrailing) {
                Image(imageName)
                    .resizable()
                    .scaledToFill()
                    .frame(height: 110)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                Image(systemName: "heart.fill")
                    .foregroundColor(.yellow)
                    .padding(8)
            }
            HStack(spacing: 8) {
                Image(systemName: iconName)
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                Text(title)
                    .font(.subheadline)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
            }
            .padding(.horizontal, 6)
            .padding(.bottom, 6)
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(ThemeColors.surface(colorScheme)))
    }
}

struct OnTripView_Previews: PreviewProvider {
    static var previews: some View {
        OnTripView()
    }
}


