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
    @StateObject private var catalogViewModel = CatalogViewModel()
    @StateObject private var favoritesViewModel = FavoriteViewModel()
    @StateObject private var notificationsViewModel = NotificationViewModel()
    @State private var selectedTab: FloatingTab = .activity
    @State private var selectedRegion: String? = nil
    @State private var showNotifications = false
    let initialName: String?
    
    init(initialName: String? = nil) {
        self.initialName = initialName
    }
    
    var body: some View {
        ZStack(alignment: .bottom) {
            Color(red: 0.918, green: 0.949, blue: 1.0) // #EAF2FF
                .ignoresSafeArea()
            
            Group {
                switch selectedTab {
                case .activity:
                    NavigationStack {
                        VStack(spacing: 0) {
                        TopBar(
                            name: initialName ?? viewModel.greetingName,
                            profileImageUrl: viewModel.profileImageUrl,
                            unreadCount: notificationsViewModel.unreadCount,
                            onNotificationsTap: { showNotifications = true },
                            onProfileTap: { selectedTab = .profile }
                        )
                        .background(
                            Color(red: 0.918, green: 0.949, blue: 1.0) // #EAF2FF
                        )
                        
                        ScrollView(showsIndicators: false) {
                            VStack(alignment: .leading, spacing: 0) {
                                // Section "Personnalisé par Gemini"
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Personnalisé par Gemini")
                                        .font(.system(size: 34, weight: .bold))
                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                    Text("Voyages adaptés à vos préférences")
                                        .font(.body)
                                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                }
                                .padding(.horizontal, 16)
                                .padding(.bottom, 8)
                                    
                                // Section des régions
                                RegionSection(
                                    selectedRegion: $selectedRegion,
                                    onRegionSelected: { region in
                                        selectedRegion = selectedRegion == region ? nil : region
                                        Task {
                                            await catalogViewModel.loadRecommendedFlights(showAll: false)
                                        }
                                    }
                                )
                                .padding(.horizontal, 24)
                                
                                Spacer()
                                    .frame(height: 16)
                                
                                // Section "Comparateur avec Gemini"
                                VStack(alignment: .leading, spacing: 8) {
                                    HStack {
                                        Text("Comparateur avec Gemini")
                                            .font(.system(size: 22, weight: .bold))
                                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                        Spacer()
                                        NavigationLink(destination: AllFlightsScreen(selectedRegion: selectedRegion)) {
                                            Text("Voir tous")
                                                .font(.body)
                                                .foregroundStyle(Color(red: 0.098, green: 0.463, blue: 0.824)) // #1976D2
                                        }
                                    }
                                    .padding(.horizontal, 24)
                                    
                                    // Destinations carousel
                                    switch catalogViewModel.uiState {
                                    case .loading:
                                        ProgressView()
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 340)
                                    case .success(let destinations, let fromCache, _, _):
                                        if fromCache {
                                            HStack(spacing: 8) {
                                                Image(systemName: "wifi.slash")
                                                    .foregroundColor(Color(red: 0.937, green: 0.188, blue: 0.0)) // #EF6C00
                                                Text("Affichage hors ligne (cache)")
                                                    .font(.caption)
                                                    .foregroundColor(Color(red: 0.937, green: 0.188, blue: 0.0))
                                            }
                                            .padding(.horizontal, 24)
                                            .padding(.bottom, 12)
                                        }
                                        
                                        let filteredDestinations = filterDestinations(destinations, by: selectedRegion)
                                        let displayDestinations = Array(filteredDestinations.prefix(6))
                                        
                                        if displayDestinations.isEmpty {
                                            Text("Aucune destination disponible")
                                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                                .frame(maxWidth: .infinity)
                                                .frame(height: 340)
                                        } else {
                                            DestinationsSection(
                                                destinations: displayDestinations,
                                                favoritesViewModel: favoritesViewModel
                                            )
                                            .padding(.horizontal, 24)
                                        }
                                    case .error(let message):
                                        VStack(spacing: 16) {
                                            Text(message)
                                                .foregroundColor(.red)
                                                .multilineTextAlignment(.center)
                                                .padding(.horizontal)
                                            Button("Réessayer") {
                                                Task {
                                                    await catalogViewModel.loadRecommendedFlights(showAll: false)
                                                }
                                            }
                                            .buttonStyle(.borderedProminent)
                                        }
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 340)
                                    case .idle:
                                        EmptyView()
                                    }
                                }
                                
                                Spacer()
                                    .frame(height: 16)
                                
                                // Discussion Card
                                DiscussionCard()
                                    .padding(.horizontal, 24)
                                
                                Spacer()
                                    .frame(height: 16)
                                
                                // Espace en bas pour le tab bar (comme Android paddingValues.calculateBottomPadding())
                                Spacer()
                                    .frame(height: 74) // Hauteur du FloatingTabBar (64) + safe area (10)
                            }
                            .scrollContentBackground(.hidden)
                        }
                        .contentMargins(.horizontal, 0, for: .scrollContent)
                    }
                    }
                case .favorites:
                    NavigationStack {
                        FavoritesView()
                    }
                case .explore:
                    NavigationStack {
                        DiscussionView()
                    }
                case .alerts:
                    NavigationStack {
                        BookingHistoryView()
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
        }
        .onAppear {
            // Recharger l'image à chaque fois que l'écran apparaît
            viewModel.reloadProfileImage()
        }
        .onChange(of: selectedTab) { oldValue, newValue in
            // Recharger l'image quand on change d'onglet et qu'on revient à activity
            if newValue == .activity {
                viewModel.reloadProfileImage()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("UserProfileImageDidUpdate"))) { _ in
            // Recharger l'image quand elle change dans le profil
            viewModel.reloadProfileImage()
        }
        .task {
            // Charger l'image depuis UserStorage immédiatement au démarrage
            viewModel.reloadProfileImage()
            print("🔄 [HomeScreen] Initial profile image: \(viewModel.profileImageUrl ?? "nil")")
            await viewModel.load()
            // Recharger l'image après le chargement du profil
            viewModel.reloadProfileImage()
            print("🔄 [HomeScreen] Profile image after load: \(viewModel.profileImageUrl ?? "nil")")
            // Load flights - CatalogViewModel will use cache if available
            await catalogViewModel.loadRecommendedFlights(showAll: false)
            await notificationsViewModel.loadUnreadCount()
            
            if viewModel.regions.isEmpty {
                await viewModel.loadCountries(for: "europe")
            }
        }
        .onChange(of: selectedRegion) { _ in
            // Reload flights when region changes
            Task {
                await catalogViewModel.loadRecommendedFlights(showAll: false)
            }
        }
        .sheet(isPresented: $showNotifications) {
            NavigationStack {
                NotificationView()
            }
        }
    }
    
    private func filterDestinations(_ destinations: [FlightDestination], by region: String?) -> [FlightDestination] {
        guard let region = region, region != "Préférences" else {
            return destinations
        }
        
        let regionCountries: [String: [String]] = [
            "Europe": ["France", "United Kingdom", "Italy", "Spain", "Netherlands", "Germany", "Switzerland", "Belgium", "Portugal", "Greece", "Austria", "Sweden", "Norway", "Denmark", "Finland", "Poland", "Czech Republic", "Hungary", "Ireland"],
            "Asie": ["China", "Japan", "India", "Thailand", "Singapore", "Malaysia", "Indonesia", "South Korea", "Vietnam", "Philippines", "UAE", "Saudi Arabia", "Turkey", "Israel"],
            "Amerique": ["United States", "Canada", "Mexico", "Brazil", "Argentina", "Chile", "Colombia", "Peru"],
            "Australie": ["Australia", "New Zealand", "Fiji"]
        ]
        
        guard let countries = regionCountries[region] else {
            return destinations
        }
        
        return destinations.filter { destination in
            countries.contains { country in
                destination.country.localizedCaseInsensitiveContains(country)
            }
        }
    }
}

// MARK: - TopBar
struct TopBar: View {
    @Environment(\.colorScheme) private var colorScheme
    let name: String
    let profileImageUrl: String?
    let unreadCount: Int
    let onNotificationsTap: () -> Void
    let onProfileTap: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            // Profile image or placeholder - circular
            Button(action: onProfileTap) {
                ProfileImageView(imageUrl: profileImageUrl, size: 40)
            }
            .buttonStyle(.plain)
            
            Text("Salut, \(name)")
                .font(.headline)
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                .lineLimit(1)
            
            Spacer()
            
            Button(action: onNotificationsTap) {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: "bell")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Color(red: 0.098, green: 0.463, blue: 0.824))
                        .frame(width: 40, height: 40)
                    
                    if unreadCount > 0 {
                        Text("\(unreadCount)")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                            .padding(4)
                            .background(
                                Circle()
                                    .fill(Color.red)
                            )
                            .offset(x: 8, y: -8)
                    }
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 4)
        .padding(.bottom, 12)
    }
}

// MARK: - RegionSection
struct RegionSection: View {
    @Environment(\.colorScheme) private var colorScheme
    @Binding var selectedRegion: String?
    let onRegionSelected: (String) -> Void
    
    private let regions: [(name: String, imageName: String?)] = [
        ("Préférences", nil),
        ("Europe", "europe"),
        ("Asie", "asia"),
        ("Amerique", "australia"), // Using australia as placeholder
        ("Australie", "australia")
    ]
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                ForEach(regions, id: \.name) { region in
                    RegionChip(
                        name: region.name,
                        imageName: region.imageName,
                        isSelected: selectedRegion == region.name,
                        onTap: {
                            onRegionSelected(region.name)
                        }
                    )
                }
            }
        }
    }
}

struct RegionChip: View {
    @Environment(\.colorScheme) private var colorScheme
    let name: String
    let imageName: String?
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                if name == "Préférences" {
                    Image(systemName: "star.fill")
                        .font(.system(size: 24))
                        .foregroundColor(isSelected ? .white : Color(red: 1.0, green: 0.757, blue: 0.027)) // #FFC107
                } else if let imageName = imageName {
                    Image(imageName)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 32, height: 32)
                        .clipShape(Circle())
                }
                
                Text(name)
                    .font(.system(size: 16, weight: isSelected ? .bold : .medium))
                    .foregroundColor(isSelected ? .white : .black)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(
                Capsule()
                    .fill(isSelected ? Color(red: 0.098, green: 0.463, blue: 0.824) : Color.white)
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - DestinationsSection
struct DestinationsSection: View {
    @Environment(\.colorScheme) private var colorScheme
    let destinations: [FlightDestination]
    @ObservedObject var favoritesViewModel: FavoriteViewModel
    
    @State private var currentIndex: Int = 0
    
    var body: some View {
        TabView(selection: $currentIndex) {
            ForEach(Array(destinations.enumerated()), id: \.element.id) { index, destination in
                DestinationCard(destination: destination, favoritesViewModel: favoritesViewModel)
                    .tag(index)
                    .padding(.horizontal, 80)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .frame(height: 340)
    }
}

struct DestinationCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let destination: FlightDestination
    @ObservedObject var favoritesViewModel: FavoriteViewModel
    
    @State private var isFavorite: Bool = false
    
    var body: some View {
        NavigationLink(destination: FlightDetailScreen(destinationId: destination.id, destination: destination)) {
            ZStack(alignment: .bottomLeading) {
                // Background Image
                Group {
                    if let imageUrl = destination.imageUrl, let url = URL(string: imageUrl) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                            case .failure, .empty:
                                Rectangle()
                                    .fill(Color.gray.opacity(0.3))
                            @unknown default:
                                Rectangle()
                                    .fill(Color.gray.opacity(0.3))
                            }
                        }
                    } else {
                        Rectangle()
                            .fill(LinearGradient(
                                colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ))
                    }
                }
                .frame(width: 290, height: 340)
                .clipShape(RoundedRectangle(cornerRadius: 24))
                
                // Gradient Overlay
                LinearGradient(
                    colors: [Color.clear, Color.black.opacity(0.7)],
                    startPoint: .center,
                    endPoint: .bottom
                )
                .frame(width: 290, height: 340)
                .clipShape(RoundedRectangle(cornerRadius: 24))
                
                // Content
                VStack(alignment: .leading, spacing: 2) {
                    Text(destination.name)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                    
                    Text(destination.country)
                        .font(.body)
                        .foregroundColor(.white.opacity(0.9))
                    
                    if let price = destination.price, price > 0 {
                        Text("\(Int(price)) \(destination.currency)")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.top, 4)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
                
                // Favorite Button (separate from navigation)
                VStack {
                    HStack {
                        Spacer()
                        Button(action: {
                            Task {
                                let newFavoriteState = !isFavorite
                                if newFavoriteState {
                                    await favoritesViewModel.addFavorite(
                                        itemType: .flight,
                                        itemId: destination.id,
                                        itemData: [
                                            "name": destination.name,
                                            "city": destination.city,
                                            "country": destination.country,
                                            "imageUrl": destination.imageUrl ?? "",
                                            "price": "\(destination.price ?? 0)",
                                            "currency": destination.currency,
                                            "airline": destination.airline ?? ""
                                        ]
                                    )
                                } else {
                                    await favoritesViewModel.removeFavorite(itemType: .flight, itemId: destination.id)
                                }
                                isFavorite = newFavoriteState
                            }
                        }) {
                            ZStack {
                                Circle()
                                    .fill(Color.white.opacity(0.3))
                                    .frame(width: 40, height: 40)
                                
                                Image(systemName: isFavorite ? "heart.fill" : "heart")
                                    .foregroundColor(isFavorite ? Color(red: 1.0, green: 0.09, blue: 0.267) : .white)
                                    .font(.system(size: 20))
                            }
                        }
                        .buttonStyle(.plain)
                    }
                    Spacer()
                }
                .padding(12)
            }
            .frame(width: 290, height: 340)
            .shadow(color: Color.black.opacity(0.12), radius: 12, x: 0, y: 8)
        }
        .buttonStyle(.plain)
        .task {
            // Check if favorite on load
            isFavorite = await favoritesViewModel.checkFavorite(itemType: .flight, itemId: destination.id)
        }
    }
}

// MARK: - DiscussionCard
struct DiscussionCard: View {
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        NavigationLink(destination: DiscussionView()) {
            HStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(Color(red: 0.098, green: 0.463, blue: 0.824).opacity(0.1))
                        .frame(width: 56, height: 56)
                    
                    Image(systemName: "bubble.left.and.bubble.right.fill")
                        .font(.system(size: 28))
                        .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Discussions de la communauté")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("Partagez vos expériences et découvrez les conseils des voyageurs")
                            .font(.caption)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                
                Spacer()
                
                Image(systemName: "arrow.right")
                    .font(.system(size: 24))
                    .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
            }
            .padding(20)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.white)
            )
            .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
        }
        .buttonStyle(.plain)
    }
}

// Placeholder screens (to be implemented later)
struct AllFlightsScreen: View {
    let selectedRegion: String?
    var body: some View {
        ZStack {
            ThemeColors.background(ColorScheme.light)
                .ignoresSafeArea()
            VStack {
                Text("Tous les vols")
                    .font(.title)
                if let region = selectedRegion {
                    Text("Région: \(region)")
                        .foregroundStyle(.secondary)
                }
            }
        }
    }
}

// FlightDetailScreen is now defined in FlightDetailScreen.swift

// Note: ProfileView is already defined in Profile/profil.swift

struct HomeScreen_Previews: PreviewProvider {
    static var previews: some View {
        HomeScreen()
    }
}
