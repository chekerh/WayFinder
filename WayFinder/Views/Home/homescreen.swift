//
//  homescreen.swift
//  WayFinder
//
//  Created by sarrachmek on 7/11/2025.
//

import Foundation
import SwiftUI

struct HomeScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = HomeViewModel()
    
    var body: some View {
        VStack(spacing: 0) {
            TopBar(name: viewModel.greetingName)
                .padding(.top, 12)
            
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    headlineSection
                    regionSection
                    highlightSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
        }
        .background(ThemeColors.background(colorScheme).ignoresSafeArea())
        .task {
            await viewModel.load()
        }
    }
    
    private var headlineSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("home_headline")
                .font(.system(size: 30, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                .lineLimit(2)
            
            Text("home_subheadline")
                .font(.subheadline)
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    
    private var regionSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 14) {
                ForEach(viewModel.regions) { region in
                    RegionChip(region: region,
                               isSelected: viewModel.selectedRegionId == region.id) {
                        viewModel.selectRegion(region)
                    }
                }
            }
            .padding(.vertical, 6)
        }
    }
    
    private var highlightSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("home_comparator_title")
                    .font(.title3.bold())
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                Spacer()
                Button(action: {
                    // TODO: navigation vers la liste complète
                }) {
                    Text("home_view_all")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
            }
            
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 60)
            } else if let error = viewModel.errorMessage {
                VStack(spacing: 12) {
                    Text("home_error_title")
                        .font(.headline)
                        .foregroundColor(.red)
                    Text(error)
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        .multilineTextAlignment(.center)
                    Button(String(localized: "home_retry")) {
                        Task { await viewModel.load() }
                    }
                    .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
            } else {
                TabView {
                    ForEach(filteredHighlights) { destination in
                        DestinationCard(destination: destination)
                            .padding(.trailing, 12)
                    }
                }
                .frame(height: 360)
                .tabViewStyle(.page(indexDisplayMode: .automatic))
            }
        }
    }
    
    private var filteredHighlights: [RecommendationHighlight] {
        guard let selectedId = viewModel.selectedRegionId else { return viewModel.highlights }
        let filtered = viewModel.highlights.filter { $0.regionId == selectedId || $0.regionId == nil }
        return filtered.isEmpty ? viewModel.highlights : filtered
    }
}

struct TopBar: View {
    @Environment(\.colorScheme) private var colorScheme
    let name: String
    
    var body: some View {
        let greetingFormat = NSLocalizedString("home_salutation_format", comment: "Greeting with user name")
        let greetingText = String(format: greetingFormat, name)
        
        HStack(spacing: 16) {
            HStack(spacing: 12) {
                Circle()
                    .fill(ThemeColors.surface(colorScheme))
                    .frame(width: 44, height: 44)
                    .overlay(
                        Text(name.prefix(1).uppercased())
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    )
                
                Text(greetingText)
                    .font(.headline)
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.9)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            Button(action: {
                // TODO: notifications
            }) {
                Image(systemName: "bell")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    .padding(12)
                    .background(
                        Circle()
                            .fill(ThemeColors.surface(colorScheme).opacity(0.7))
                    )
            }
        }
        .padding(.horizontal, 20)
    }
}

struct RegionChip: View {
    @Environment(\.colorScheme) private var colorScheme
    let region: RecommendationRegion
    let isSelected: Bool
    let onTap: () -> Void
    
    private var backgroundStyle: AnyShapeStyle {
        if isSelected {
            return AnyShapeStyle(ThemeColors.accentGradient(colorScheme))
        } else {
            return AnyShapeStyle(ThemeColors.surface(colorScheme))
        }
    }
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                if let imageUrl = region.imageUrl, let url = URL(string: imageUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable()
                    } placeholder: {
                        Circle()
                            .fill(ThemeColors.surface(colorScheme).opacity(0.4))
                    }
                    .frame(width: 28, height: 28)
                    .clipShape(Circle())
                }
                
                Text(region.title)
                    .font(.subheadline)
                    .fontWeight(isSelected ? .semibold : .regular)
                    .foregroundStyle(isSelected ? Color.white : ThemeColors.primaryText(colorScheme))
            }
            .padding(.vertical, 10)
            .padding(.horizontal, 18)
            .background(
                Capsule()
                    .fill(backgroundStyle)
                    .shadow(color: Color.black.opacity(isSelected ? 0.2 : 0.05), radius: 8, x: 0, y: 4)
            )
        }
        .buttonStyle(.plain)
    }
}

struct DestinationCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let destination: RecommendationHighlight
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            backgroundImage
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            LinearGradient(colors: [Color.black.opacity(0.0), Color.black.opacity(0.85)], startPoint: .top, endPoint: .bottom)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            contentOverlay
        }
        .frame(width: 300, height: 340)
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.45 : 0.12), radius: 12, x: 0, y: 8)
    }
    
    private var backgroundImage: some View {
        Group {
            if let imageUrl = destination.imageUrl, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { image in
                    image
                .resizable()
                .scaledToFill()
                } placeholder: {
                    Rectangle()
                        .fill(ThemeColors.surface(colorScheme))
                }
            } else {
                Rectangle()
                    .fill(ThemeColors.surface(colorScheme))
            }
        }
    }
    
    private var contentOverlay: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(destination.title)
                .font(.headline)
                .foregroundColor(.white)
            Text(destination.subtitle)
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.9))
                .lineLimit(2)
                .minimumScaleFactor(0.9)
            
            Spacer(minLength: 0)
            
            if let rating = destination.rating {
                HStack(spacing: 6) {
                    Image(systemName: "star.fill")
                        .foregroundColor(.yellow)
                        .font(.footnote)
                    Text(String(format: "%.1f", rating))
                        .font(.footnote.bold())
                    .foregroundColor(.white)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.white.opacity(0.2))
                .clipShape(Capsule())
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
    }
}

struct CustomBottomNavigationBar: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var selectedIndex = 0
    
    var body: some View {
        HStack {
            BottomNavItem(icon: "house", isSelected: selectedIndex == 0) { selectedIndex = 0 }
            BottomNavItem(icon: "heart", isSelected: selectedIndex == 1) { selectedIndex = 1 }
            BottomNavItem(icon: "message", isSelected: selectedIndex == 2) { selectedIndex = 2 }
            BottomNavItem(icon: "person", isSelected: selectedIndex == 3) { selectedIndex = 3 }
        }
        .padding(.horizontal)
        .padding(.vertical, 12)
        .background(ThemeColors.accentGradient(colorScheme))
        .clipShape(Capsule())
        .padding(.bottom, 16)
    }
}

struct BottomNavItem: View {
    @Environment(\.colorScheme) private var colorScheme
    let icon: String
    let isSelected: Bool
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            Image(systemName: icon)
                .foregroundColor(isSelected ? Color.white : ThemeColors.secondaryText(colorScheme))
                .padding()
                .background(Circle().fill(isSelected ? ThemeColors.accent() : ThemeColors.surface(colorScheme).opacity(0.4)))
        }
        .frame(maxWidth: .infinity)
    }
}

// Models
struct Region {
    let name: String
    let imageRes: String
}

struct Destination {
    let name: String
    let description: String
    let imageRes: String
    let rating: String
}

struct HomeScreen_Previews: PreviewProvider {
    static var previews: some View {
        HomeScreen()
    }
}
