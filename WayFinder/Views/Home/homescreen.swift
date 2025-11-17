//
//  homescreen.swift
//  WayFinder
//
//  Created by sarrachmek on 7/11/2025.
//

import Foundation
import SwiftUI
import UIKit

struct HomeScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = HomeViewModel()
    @State private var selectedTab: FloatingTab = .activity
    let initialName: String?
    
    init(initialName: String? = nil) {
        self.initialName = initialName
    }
    
    var body: some View {
        ZStack(alignment: .bottom) {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            Group {
                switch selectedTab {
                case .activity:
            VStack(spacing: 0) {
                TopBar(name: initialName ?? viewModel.greetingName)
                    .padding(.top, 12)
                
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 24) {
                        regionSection
                        highlightSection
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 120)
                        }
                        .safeAreaPadding(.top, 8)
                    }
                case .favorites:
                    VStack {
                        Text("Favoris")
                            .font(.largeTitle.bold())
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            .padding()
                        Spacer()
                    }
                case .explore:
                    VStack {
                        Text("Explorer")
                            .font(.largeTitle.bold())
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            .padding()
                        Spacer()
                    }
                case .alerts:
                    VStack {
                        Text("Notifications")
                            .font(.largeTitle.bold())
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            .padding()
                        Spacer()
                    }
                case .profile:
                    NavigationStack {
                        ProfileView()
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            FloatingTabBar(selection: $selectedTab)
                .padding(.bottom, 0)
        }
        .task {
            await viewModel.load()
            // Si les régions sont vides, charger les pays de la première région par défaut (Europe)
            if viewModel.regions.isEmpty {
                await viewModel.loadCountries(for: "europe")
            }
        }
    }
    
    private var headlineSection: some View {
        Text("home_headline")
            .font(.system(size: 30, weight: .bold))
            .foregroundStyle(ThemeColors.primaryText(colorScheme))
            .frame(maxWidth: .infinity, alignment: .leading)
    }
    
    private var regionSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 14) {
                ForEach(displayRegions) { region in
                    NavigationLink(destination: CountryListView(region: region)) {
                        RegionChip(region: region,
                                   isSelected: viewModel.selectedRegionId == region.id) {
                            viewModel.selectRegion(region)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 6)
        }
    }
    
    private var highlightSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Comparateur avec Gemini")
                    .font(.title3.bold())
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                Spacer()
                NavigationLink(destination: {
                    if let selectedRegionId = viewModel.selectedRegionId,
                       let region = viewModel.regions.first(where: { $0.id == selectedRegionId }) ?? displayRegions.first(where: { $0.id == selectedRegionId }) {
                        CountryListView(region: region)
                    }
                }) {
                    Text("Voir tous")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
            }
            
            if viewModel.isLoadingCountries {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 60)
            } else if viewModel.countries.isEmpty {
                // Aucun pays chargé - afficher un message ou les highlights par défaut
                TabView {
                    ForEach(displayHighlights) { destination in
                        DestinationCard(
                            destination: destination,
                            localAssetName: resolveLocalAssetName(for: destination)
                        )
                            .padding(.vertical, 4)
                            .padding(.horizontal, 16)
                    }
                }
                .frame(height: 300)
                .tabViewStyle(.page(indexDisplayMode: .never))
            } else {
                // Afficher les pays chargés depuis l'API
                TabView {
                    ForEach(viewModel.countries) { country in
                        NavigationLink(destination: CountryDetailView(countryId: country.id)) {
                            CountryCard(country: country)
                                .padding(.vertical, 4)
                                .padding(.horizontal, 16)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .frame(height: 300)
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
        }
    }
    
    private func resolveLocalAssetName(for highlight: RecommendationHighlight) -> String? {
        // Prioritize specific highlight assets first
        var candidates: [String] = [
            highlight.id.lowercased(),
            highlight.title.lowercased()
        ]
        if let regionId = highlight.regionId {
            candidates.append(regionId.lowercased())
        }
        
        // Common fallbacks
        if let regionId = highlight.regionId?.lowercased() {
            if regionId.contains("europe") { candidates.append("rome") }
            if regionId.contains("asia") { candidates.append("asia") }
            if regionId.contains("australia") { candidates.append("australia") }
        }
        
        for name in candidates {
            if imageAssetExists(name) { return name }
        }
        return "rome"
    }
    
    private func imageAssetExists(_ name: String) -> Bool {
        UIImage(named: name) != nil
    }
    
    private var filteredHighlights: [RecommendationHighlight] {
        guard let selectedId = viewModel.selectedRegionId else { return viewModel.highlights }
        let filtered = viewModel.highlights.filter { $0.regionId == selectedId || $0.regionId == nil }
        return filtered.isEmpty ? viewModel.highlights : filtered
    }
    
    /// Utilisé pour l'affichage : tombe sur des données de démonstration si l'API ne répond pas.
    private var displayHighlights: [RecommendationHighlight] {
        // Ne montrer que les 3 cartes souhaitées avec assets locaux
        return defaultHighlights
    }
    
    /// Utilisé pour l'affichage des régions : remplace par Europe/Asie/Australie si vide.
    private var displayRegions: [RecommendationRegion] {
        viewModel.regions.isEmpty ? defaultRegions : viewModel.regions
    }
    
    private var defaultRegions: [RecommendationRegion] {
        [
            RecommendationRegion(id: "europe", title: "Europe", imageUrl: nil),
            RecommendationRegion(id: "asia", title: "Asie", imageUrl: nil),
            RecommendationRegion(id: "australia", title: "Australie", imageUrl: nil)
        ]
    }
    
    private var defaultHighlights: [RecommendationHighlight] {
        [
            RecommendationHighlight(
                id: "central-asia",
                title: "Asie centrale",
                subtitle: "Parcourez l'Asie centrale époustouflante",
                rating: 4.9,
                imageUrl: nil,
                regionId: "asia"
            )
        ]
    }
}

struct TopBar: View {
    @Environment(\.colorScheme) private var colorScheme
    let name: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                // Placeholder gris
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 40, height: 40)
                
                Text("Salut, \(name)")
                    .font(.headline)
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    .lineLimit(1)
                
                Spacer()
                
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
            
            // Titre principal
            Text("home_headline")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                .frame(maxWidth: .infinity, alignment: .leading)
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
    
    private var regionImage: String? {
        let title = region.title.lowercased()
        if title.contains("europe") {
            return "europe"
        } else if title.contains("asie") || title.contains("asia") {
            return "east-asia-context-dot"
        } else if title.contains("australie") || title.contains("australia") {
            return "australia"
        }
        return nil
    }
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                if let imageName = regionImage {
                    Image(imageName)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 28, height: 28)
                        .clipShape(Circle())
                } else {
                    Circle()
                        .fill(Color.clear)
                        .frame(width: 28, height: 28)
                }
                
                Text(region.title)
                    .font(.subheadline)
                    .fontWeight(isSelected ? .semibold : .regular)
                    .foregroundStyle(isSelected ? Color.white : ThemeColors.primaryText(colorScheme))
            }
            .padding(.vertical, 10)
            .padding(.horizontal, 18)
            .background {
                if isSelected {
                    Capsule()
                        .fill(ThemeColors.accentGradient(colorScheme))
                } else {
                    Capsule()
                        .fill(Color.clear)
                }
            }
            .overlay(
                Capsule()
                    .stroke(isSelected ? Color.clear : ThemeColors.border(colorScheme), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

struct DestinationCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let destination: RecommendationHighlight
    let localAssetName: String?
    
    var body: some View {
        ZStack(alignment: .topLeading) {
            backgroundImage
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            LinearGradient(colors: [Color.black.opacity(0.0), Color.black.opacity(0.7)], startPoint: .top, endPoint: .bottom)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            contentOverlay
        }
        .frame(width: 280, height: 260)
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.45 : 0.12), radius: 12, x: 0, y: 8)
    }
    
    private var backgroundImage: some View {
        Group {
            if let imageUrl = destination.imageUrl, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFill()
                        .frame(width: 280, height: 260)
                } placeholder: {
                    Rectangle()
                        .fill(ThemeColors.surface(colorScheme))
                        .frame(width: 280, height: 260)
                }
            } else {
                // Fallback vers un asset local résolu
                Image(localAssetName ?? "rome")
                    .resizable()
                    .scaledToFill()
                    .frame(width: 280, height: 260)
            }
        }
    }
    
    private var contentOverlay: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(destination.title)
                    .font(.headline)
                    .foregroundColor(.white)
                
                Spacer()
                
                if let rating = destination.rating {
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .foregroundColor(.yellow)
                            .font(.caption)
                        Text(String(format: "%.1f", rating).replacingOccurrences(of: ".", with: ","))
                            .font(.caption.bold())
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.white.opacity(0.2))
                    .clipShape(Capsule())
                }
            }
            
            Spacer()
            
            Text(destination.subtitle)
                .font(.title3.bold())
                .foregroundColor(.white)
                .lineLimit(2)
                .minimumScaleFactor(0.9)
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

// Nouvelle card pour afficher les pays depuis l'API
struct CountryCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let country: Country
    
    var body: some View {
        ZStack(alignment: .topLeading) {
            backgroundImage
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            LinearGradient(colors: [Color.black.opacity(0.0), Color.black.opacity(0.7)], startPoint: .top, endPoint: .bottom)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            contentOverlay
        }
        .frame(width: 280, height: 260)
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.45 : 0.12), radius: 12, x: 0, y: 8)
    }
    
    private var backgroundImage: some View {
        Group {
            if let imageUrl = country.thumbnailImageUrl, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(width: 280, height: 260)
                    case .failure(_), .empty:
                        Rectangle()
                            .fill(ThemeColors.surface(colorScheme))
                            .overlay {
                                Image(systemName: "photo")
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            }
                            .frame(width: 280, height: 260)
                    @unknown default:
                        Rectangle()
                            .fill(ThemeColors.surface(colorScheme))
                            .frame(width: 280, height: 260)
                    }
                }
            } else {
                // Fallback vers un asset local
                Rectangle()
                    .fill(ThemeColors.surface(colorScheme))
                    .overlay {
                        Image(systemName: "photo")
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    }
                    .frame(width: 280, height: 260)
            }
        }
    }
    
    private var contentOverlay: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(country.name)
                    .font(.headline)
                    .foregroundColor(.white)
                    .lineLimit(1)
                
                Spacer()
            }
            
            Spacer()
            
            Text(country.summary)
                .font(.title3.bold())
                .foregroundColor(.white)
                .lineLimit(2)
                .minimumScaleFactor(0.9)
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

// MARK: - City carousel views
struct CityItem: Identifiable {
    let id = UUID().uuidString
    let title: String
    let subtitle: String
    let imageName: String
    let rating: Double
}

struct CityCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let city: CityItem
    
    var body: some View {
        ZStack(alignment: .topLeading) {
            Image(city.imageName)
                .resizable()
                .scaledToFill()
                .frame(width: 320, height: 340)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            LinearGradient(colors: [Color.black.opacity(0.0), Color.black.opacity(0.8)],
                           startPoint: .center, endPoint: .bottom)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text(city.title)
                        .font(.headline)
                        .foregroundColor(.white)
                    Spacer()
                    HStack(spacing: 6) {
                        Image(systemName: "star.fill")
                            .foregroundColor(.yellow)
                            .font(.caption)
                        Text(String(format: "%.1f", city.rating).replacingOccurrences(of: ".", with: ","))
                            .font(.caption.bold())
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.2))
                    .clipShape(Capsule())
                }
                Spacer()
                Text(city.subtitle)
                    .font(.title2.bold())
                    .foregroundColor(.white)
                    .lineLimit(3)
            }
            .padding(20)
        }
        .frame(width: 320, height: 340)
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.45 : 0.12), radius: 12, x: 0, y: 8)
    }
}

struct HomeScreen_Previews: PreviewProvider {
    static var previews: some View {
        HomeScreen()
    }
}
