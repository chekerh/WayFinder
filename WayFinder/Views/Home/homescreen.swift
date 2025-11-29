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
    @EnvironmentObject private var languageManager: LanguageManager
    @StateObject private var viewModel = HomeViewModel()
    @StateObject private var catalogViewModel = CatalogViewModel()
    @StateObject private var favoritesViewModel = FavoriteViewModel()
    @StateObject private var notificationsViewModel = NotificationViewModel()
    @State private var selectedTab: FloatingTab = .activity
    @State private var selectedRegion: String? = nil
    @State private var showNotifications = false
    @State private var navigateToPostId: String? = nil // Pour naviguer vers un post spécifique
    @State private var showDiscussionView = false // Pour afficher DiscussionView au lieu de ChatView
    @State private var navigateToOnboarding = false // Pour naviguer vers l'onboarding
    let initialName: String?
    
    init(initialName: String? = nil) {
        self.initialName = initialName
        print("🏠 [HomeScreen] Initialized with initialName: \(initialName ?? "nil")")
    }
    
    var body: some View {
        ZStack(alignment: .bottom) {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            Group {
                switch selectedTab {
                case .activity:
                    NavigationStack {
                        VStack(spacing: 0) {
                        TopBar(
                            name: {
                                // Utiliser initialName en priorité, sinon greetingName du viewModel
                                let name = initialName ?? viewModel.greetingName
                                if let initialName = initialName {
                                    print("🏠 [HomeScreen] Using initialName: \(initialName)")
                                } else {
                                    print("🏠 [HomeScreen] Using viewModel.greetingName: \(viewModel.greetingName)")
                                }
                                return name
                            }(),
                            profileImageUrl: viewModel.profileImageUrl,
                            onNotificationsTap: { showNotifications = true },
                            onProfileTap: { selectedTab = .profile }
                        )
                        .background(
                            ThemeColors.background(colorScheme)
                        )
                        .navigationDestination(isPresented: $navigateToOnboarding) {
                            SurveyScreen()
                        }
                        .task {
                            // Charger le profil utilisateur pour vérifier l'état d'onboarding
                            await viewModel.loadUserProfileIfNeeded()
                        }
                        
                        ScrollView(showsIndicators: false) {
                            VStack(alignment: .leading, spacing: 0) {
                                // Onboarding Reminder Card (like Android) - affichée en haut si onboarding non complété
                                if !viewModel.onboardingCompleted {
                                    OnboardingReminderCard(
                                        onStart: {
                                            navigateToOnboarding = true
                                        },
                                        onContinue: {
                                            viewModel.showOnboardingAlert = false
                                        },
                                        colorScheme: colorScheme
                                    )
                                    .padding(.horizontal, 16)
                                    .padding(.top, 16)
                                    .padding(.bottom, 8)
                                }
                                // Section "Personnalisé par Gemini"
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("home_personalized_title")
                                        .font(.system(size: 34, weight: .bold))
                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                    Text("home_personalized_subtitle")
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
                                        Text("home_comparator_title")
                                            .font(.system(size: 22, weight: .bold))
                                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                        Spacer()
                                        NavigationLink(destination: AllFlightsScreen(selectedRegion: selectedRegion)) {
                                            Text("home_view_all")
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
                                                Text("home_offline_cache")
                                                    .font(.caption)
                                                    .foregroundColor(Color(red: 0.937, green: 0.188, blue: 0.0))
                                            }
                                            .padding(.horizontal, 24)
                                            .padding(.bottom, 12)
                                        }
                                        
                                        let filteredDestinations = filterDestinations(destinations, by: selectedRegion)
                                        let displayDestinations = Array(filteredDestinations.prefix(6))
                                        
                                        if displayDestinations.isEmpty {
                                            Text("home_no_destinations")
                                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                                .frame(maxWidth: .infinity)
                                                .frame(height: 340)
                                        } else {
                                            DestinationsSection(
                                                destinations: displayDestinations,
                                                favoritesViewModel: favoritesViewModel
                                            )
                                        }
                                    case .error(let message):
                                        VStack(spacing: 16) {
                                            Text(message)
                                                .foregroundColor(.red)
                                                .multilineTextAlignment(.center)
                                                .padding(.horizontal)
                                            Button(String(localized: "home_retry")) {
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
                        FavoritesView(viewModel: favoritesViewModel)
                    }
                case .explore:
                    NavigationStack {
                        if showDiscussionView {
                            DiscussionViewWithNavigation(
                                navigateToPostId: $navigateToPostId,
                                onDismiss: { showDiscussionView = false }
                            )
                        } else {
                            ChatView(onBackToHome: {
                                selectedTab = .activity
                            })
                        }
                    }
                case .alerts:
                    NavigationStack {
                        BookingHistoryView(onBackToHome: {
                            selectedTab = .activity
                        })
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
            // Recharger les favoris quand on navigue vers l'onglet favoris
            if newValue == .favorites {
                Task {
                    await favoritesViewModel.loadFavorites()
                    await favoritesViewModel.loadFavoriteCount()
                }
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
            // Les notifications sont désactivées pour le moment
            // await notificationsViewModel.loadUnreadCount()
            
            if viewModel.regions.isEmpty {
                await viewModel.loadCountries(for: "europe")
            }
        }
        .onChange(of: selectedRegion) {
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
        .onAppear {
            // Activer le polling des notifications quand on arrive dans l'interface principale
            FirebaseMessagingService.shared.setMainInterfaceState(true)
            
            // Écouter les notifications tapées pour naviguer
            setupNotificationNavigation()
        }
        .onDisappear {
            // Désactiver le polling quand on quitte l'interface principale
            FirebaseMessagingService.shared.setMainInterfaceState(false)
        }
    }
    
    private func localizedRegionName(for key: String) -> String {
        switch key {
        case "Preferences": return String(localized: "region_preferences")
        case "Europe": return String(localized: "region_europe")
        case "Asie": return String(localized: "region_asia")
        case "Amerique": return String(localized: "region_america")
        case "Australie": return String(localized: "region_australia")
        default: return key
        }
    }
    
    private func filterDestinations(_ destinations: [FlightDestination], by region: String?) -> [FlightDestination] {
        guard let region = region, region != "Preferences" else {
            return destinations
        }
        
        let regionCountries: [String: [String]] = [
            "Europe": ["France", "United Kingdom", "Italy", "Spain", "Netherlands", "Germany", "Switzerland", "Belgium", "Portugal", "Greece", "Austria", "Sweden", "Norway", "Denmark", "Finland", "Poland", "Czech Republic", "Hungary", "Ireland"],
            "Asie": ["China", "Japan", "India", "Thailand", "Singapore", "Malaysia", "Indonesia", "South Korea", "Vietnam", "Philippines", "UAE", "United Arab Emirates", "Saudi Arabia", "Turkey", "Israel"],
            "Amerique": ["United States", "USA", "Canada", "Mexico", "Brazil", "Argentina", "Chile", "Colombia", "Peru"],
            "Australie": ["Australia", "New Zealand", "Fiji"]
        ]
        
        guard let countries = regionCountries[region] else {
            return destinations
        }
        
        return destinations.filter { destination in
            countries.contains { country in
                // Use contains (like Android) instead of localizedCaseInsensitiveContains
                // This allows partial matching (e.g., "United Arab Emirates" matches "UAE")
                destination.country.localizedCaseInsensitiveContains(country) || 
                country.localizedCaseInsensitiveContains(destination.country)
            }
        }
    }
}

// MARK: - TopBar
struct TopBar: View {
    @Environment(\.colorScheme) private var colorScheme
    let name: String
    let profileImageUrl: String?
    let onNotificationsTap: () -> Void
    let onProfileTap: () -> Void
    
    // Helper function for greeting based on time of day (like Android)
    private func getGreeting() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        let key: String
        switch hour {
        case 0..<12:
            key = "greeting_morning"
        case 12..<18:
            key = "greeting_afternoon"
        default:
            key = "greeting_evening"
        }
        // Use Bundle.main which is modified by LanguageManager
        return Bundle.main.localizedString(forKey: key, value: nil, table: nil)
    }
    
    var body: some View {
        HStack(spacing: 12) {
            // Profile image or placeholder - circular
            Button(action: onProfileTap) {
                ProfileImageView(imageUrl: profileImageUrl, size: 40)
            }
            .buttonStyle(.plain)
            
            Text("\(getGreeting()), \(name)! 👋")
                .font(.headline)
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                .lineLimit(1)
            
            Spacer()
            
            Button(action: onNotificationsTap) {
                Image(systemName: "bell")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(Color(red: 0.098, green: 0.463, blue: 0.824))
                    .frame(width: 40, height: 40)
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
    
    // Internal keys for filtering (must match regionCountries keys)
    private let regionKeys = ["Preferences", "Europe", "Asie", "Amerique", "Australie"]
    
    private var regions: [(key: String, name: String, imageName: String?)] {
        [
            ("Preferences", String(localized: "region_preferences"), nil),
            ("Europe", String(localized: "region_europe"), "europe"),
            ("Asie", String(localized: "region_asia"), "asia"),
            ("Amerique", String(localized: "region_america"), "america"),
            ("Australie", String(localized: "region_australia"), "australia")
        ]
    }
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                ForEach(regions, id: \.key) { region in
                    RegionChip(
                        name: region.name,
                        imageName: region.imageName,
                        isSelected: selectedRegion == region.key,
                        onTap: {
                            onRegionSelected(region.key)
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
                if name == String(localized: "region_preferences") {
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
                    .foregroundColor(isSelected ? .white : ThemeColors.primaryText(colorScheme))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(
                Capsule()
                    .fill(isSelected ? ThemeColors.accent() : ThemeColors.surface(colorScheme))
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - ScrollOffsetPreferenceKey
struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

// MARK: - DestinationsSection
struct DestinationsSection: View {
    @Environment(\.colorScheme) private var colorScheme
    let destinations: [FlightDestination]
    @ObservedObject var favoritesViewModel: FavoriteViewModel
    
    @State private var scrollOffset: CGFloat = 0
    
    private var cardWidth: CGFloat {
        return 290
    }
    private let cardHeight: CGFloat = 340
    private let cardSpacing: CGFloat = -60 // Reduced overlap for more padding between cards
    private let horizontalPadding: CGFloat = 80 // Padding to show adjacent cards
    
    private var totalCardWidth: CGFloat {
        return cardWidth + cardSpacing
    }
    
    var body: some View {
        if destinations.isEmpty {
            Text("home_no_destinations")
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                .frame(maxWidth: .infinity)
                .frame(height: cardHeight)
        } else {
            GeometryReader { geometry in
                let screenWidth = geometry.size.width
                let centerX = screenWidth / 2
                
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: cardSpacing) {
                        ForEach(Array(destinations.enumerated()), id: \.element.id) { index, destination in
                            GeometryReader { cardGeometry in
                                let cardFrame = cardGeometry.frame(in: .named("scroll"))
                                let cardCenter = cardFrame.midX
                                let distanceFromCenter = abs(cardCenter - centerX)
                                let pageOffset = min(1.0, distanceFromCenter / totalCardWidth)
                                
                                // Determine if card is to the left or right of center
                                let isRightOfCenter = cardCenter > centerX + 10 // Add 10pt threshold to avoid exact center edge cases
                                let isLeftOfCenter = cardCenter < centerX - 10
                                let isAtOrNearCenter = !isRightOfCenter && !isLeftOfCenter
                                
                                // Calculate z-index ensuring right cards are ALWAYS behind
                                // Priority: Center/Near Center > Left > Right
                                let zIndexValue: Double = {
                                    if isAtOrNearCenter {
                                        // Center card: highest z-index
                                        return 1000000.0 - Double(distanceFromCenter) * 100
                                    } else if isLeftOfCenter {
                                        // Left cards: medium-high z-index (always above right cards)
                                        return 500000.0 - Double(distanceFromCenter) * 100 + Double(destinations.count - index) * 1000
                                    } else {
                                        // Right cards: lowest z-index (ALWAYS behind, regardless of distance)
                                        return 1000.0 - Double(distanceFromCenter) * 10 + Double(destinations.count - index) * 100
                                    }
                                }()
                                
                                // Calculate transparency effect based on distance
                                let transparency = max(0.3, 1.0 - pageOffset * 0.7)
                                
                                NavigationLink(destination: FlightDetailScreen(destinationId: destination.id, destination: destination)) {
                                    ZStack(alignment: .topTrailing) {
                                        DestinationCard(
                                            destination: destination,
                                            favoritesViewModel: favoritesViewModel,
                                            index: index,
                                            currentIndex: 0,
                                            totalCount: destinations.count,
                                            pageOffset: pageOffset
                                        )
                                        .frame(width: cardWidth, height: cardHeight)
                                        .blur(radius: pageOffset > 0.25 ? 1.5 * pageOffset : 0)
                                        .overlay(
                                            // Dynamic glass effect overlay for cards not in center
                                            Group {
                                                if pageOffset > 0.15 {
                                                    RoundedRectangle(cornerRadius: 28)
                                                        .fill(
                                                            LinearGradient(
                                                                colors: [
                                                                    Color.white.opacity(0.15 * min(pageOffset * 1.5, 1.0)),
                                                                    Color.white.opacity(0.08 * min(pageOffset * 1.5, 1.0)),
                                                                    Color.white.opacity(0.05 * min(pageOffset * 1.5, 1.0))
                                                                ],
                                                                startPoint: .topLeading,
                                                                endPoint: .bottomTrailing
                                                            )
                                                        )
                                                        .overlay(
                                                            RoundedRectangle(cornerRadius: 28)
                                                                .stroke(
                                                                    LinearGradient(
                                                                        colors: [
                                                                            Color.white.opacity(0.3 * min(pageOffset * 1.2, 1.0)),
                                                                            Color.white.opacity(0.15 * min(pageOffset * 1.2, 1.0))
                                                                        ],
                                                                        startPoint: .topLeading,
                                                                        endPoint: .bottomTrailing
                                                                    ),
                                                                    lineWidth: 1.5
                                                                )
                                                        )
                                                        .shadow(
                                                            color: Color.white.opacity(0.1 * min(pageOffset, 0.8)),
                                                            radius: 8 * min(pageOffset, 1.0),
                                                            x: 0,
                                                            y: 2
                                                        )
                                                }
                                            }
                                        )
                                        
                                        FavoriteButtonOverlay(
                                            destination: destination,
                                            favoritesViewModel: favoritesViewModel,
                                            isFocused: pageOffset < 0.1
                                        )
                                        .padding(.trailing, 16)
                                        .padding(.top, 16)
                                    }
                                    .padding(.vertical, pageOffset > 0.15 ? 12 * pageOffset : 0)
                                    .padding(.horizontal, pageOffset > 0.15 ? 6 * pageOffset : 0)
                                }
                                .buttonStyle(.plain)
                                .scaleEffect(
                                    x: 0.88 + (1.0 - 0.88) * (1.0 - pageOffset),
                                    y: 0.88 + (1.0 - 0.88) * (1.0 - pageOffset)
                                )
                                .opacity(transparency)
                                .zIndex(zIndexValue)
                                .animation(
                                    .spring(response: 0.5, dampingFraction: 0.8, blendDuration: 0),
                                    value: pageOffset
                                )
                            }
                            .frame(width: cardWidth)
                        }
                    }
                    .padding(.horizontal, horizontalPadding)
                    .background(
                        GeometryReader { scrollGeometry in
                            Color.clear
                                .preference(key: ScrollOffsetPreferenceKey.self, value: scrollGeometry.frame(in: .named("scroll")).minX)
                        }
                    )
                }
                .coordinateSpace(name: "scroll")
                .onPreferenceChange(ScrollOffsetPreferenceKey.self) { value in
                    scrollOffset = -value
                }
            }
            .frame(height: cardHeight)
        }
    }
}

struct DestinationCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let destination: FlightDestination
    @ObservedObject var favoritesViewModel: FavoriteViewModel
    let index: Int
    let currentIndex: Int
    let totalCount: Int
    let pageOffset: CGFloat
    
    private var isCurrentCard: Bool {
        pageOffset < 0.1
    }
    
    var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let height = geometry.size.height
            
            ZStack(alignment: .bottomLeading) {
                // Background Image with enhanced styling
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
                .frame(width: width, height: height)
                .clipShape(RoundedRectangle(cornerRadius: 28))
                .overlay(
                    RoundedRectangle(cornerRadius: 28)
                        .stroke(
                            isCurrentCard ? Color.white.opacity(0.3) : Color.clear,
                            lineWidth: isCurrentCard ? 2 : 0
                        )
                )
                
                // Enhanced Gradient Overlay
                LinearGradient(
                    colors: [
                        Color.clear,
                        Color.black.opacity(0.3),
                        Color.black.opacity(0.7)
                    ],
                    startPoint: UnitPoint(x: 0.5, y: 0.4),
                    endPoint: .bottom
                )
                .frame(width: width, height: height)
                .clipShape(RoundedRectangle(cornerRadius: 28))
            
                // Content with enhanced typography
            VStack(alignment: .leading, spacing: 0) {
                Text(destination.name)
                        .font(.system(size: 26, weight: .bold))
                    .foregroundColor(.white)
                        .shadow(color: Color.black.opacity(0.3), radius: 2, x: 0, y: 1)
                
                Text(destination.country)
                        .font(.system(size: 17, weight: .medium))
                        .foregroundColor(.white.opacity(0.95))
                        .padding(.top, 6)
                        .shadow(color: Color.black.opacity(0.2), radius: 1, x: 0, y: 1)
                
                if let price = destination.price, price > 0 {
                        HStack(spacing: 4) {
                            Text("\(Int(price))")
                                .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                            Text(destination.currency)
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(.white.opacity(0.9))
                        }
                        .padding(.top, 10)
                        .shadow(color: Color.black.opacity(0.3), radius: 2, x: 0, y: 1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
                .padding(26)
            }
            .frame(width: width, height: height)
            .shadow(
                color: isCurrentCard
                    ? Color.black.opacity(0.35)
                    : Color.black.opacity(0.15),
                radius: isCurrentCard ? 20 : 10,
                x: 0,
                y: isCurrentCard ? 15 : 6
            )
        }
    }
}

// MARK: - FavoriteButtonOverlay
struct FavoriteButtonOverlay: View {
    let destination: FlightDestination
    @ObservedObject var favoritesViewModel: FavoriteViewModel
    let isFocused: Bool
    
    @State private var isFavorite: Bool = false
    @State private var isPressed: Bool = false
    
    init(destination: FlightDestination, favoritesViewModel: FavoriteViewModel, isFocused: Bool = false) {
        self.destination = destination
        self.favoritesViewModel = favoritesViewModel
        self.isFocused = isFocused
    }
    
    var body: some View {
        Button(action: {
            // Haptic feedback
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.impactOccurred()
            
            withAnimation(.spring(response: 0.2, dampingFraction: 0.6)) {
                isPressed = true
            }
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                withAnimation(.spring(response: 0.2, dampingFraction: 0.6)) {
                    isPressed = false
                }
            }
            
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
                // Enhanced background with blur effect
                Circle()
                    .fill(
                        isFocused 
                            ? (isFavorite ? Color(red: 1.0, green: 0.09, blue: 0.267).opacity(0.25) : Color.white.opacity(0.4))
                            : Color.white.opacity(0.3)
                    )
                    .frame(width: isFocused ? 48 : 44, height: isFocused ? 48 : 44)
                    .background(
                        Circle()
                            .fill(Color.white.opacity(0.2))
                            .blur(radius: 8)
                    )
                
                Image(systemName: isFavorite ? "heart.fill" : "heart")
                    .foregroundColor(isFavorite ? Color(red: 1.0, green: 0.09, blue: 0.267) : .white)
                    .font(.system(size: isFocused ? 22 : 20, weight: isFavorite ? .semibold : .medium))
                    .scaleEffect(isPressed ? 0.9 : (isFavorite ? 1.15 : 1.0))
                    .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isFavorite)
                    .animation(.spring(response: 0.2, dampingFraction: 0.6), value: isPressed)
            }
        }
        .buttonStyle(.plain)
        .frame(width: isFocused ? 48 : 44, height: isFocused ? 48 : 44)
        .contentShape(Circle())
        .scaleEffect(isFocused ? 1.0 : 0.95)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isFocused)
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
                    Text("home_community_discussions")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("home_community_subtitle")
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
                    .fill(ThemeColors.surface(colorScheme))
            )
            .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - AllFlightsScreen (Comparateur de prix)
struct AllFlightsScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var catalogViewModel = CatalogViewModel()
    @StateObject private var favoritesViewModel = FavoriteViewModel()
    @State private var sortOption: SortOption = .price
    @State private var selectedRegion: String?
    
    let initialSelectedRegion: String?
    
    enum SortOption: String, CaseIterable {
        case price = "Prix"
        case name = "Nom"
        case country = "Pays"
    }
    
    init(selectedRegion: String? = nil) {
        self.initialSelectedRegion = selectedRegion
        self._selectedRegion = State(initialValue: selectedRegion)
    }
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("home_comparator_title")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        Spacer()
                    }
                    
                    // Filtres et tri
                    HStack(spacing: 12) {
                        // Filtre par région
                        Menu {
                            Button(action: { selectedRegion = nil }) {
                                HStack {
                                    Text("home_all_regions")
                                    if selectedRegion == nil {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                            Divider()
                            ForEach(["Europe", "Asie", "Amerique", "Australie"], id: \.self) { regionKey in
                                Button(action: { selectedRegion = regionKey }) {
                                    HStack {
                                        Text(localizedRegionName(for: regionKey))
                                        if selectedRegion == regionKey {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                }
                            }
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: "location.fill")
                                    .font(.system(size: 14))
                                Text(selectedRegion != nil ? localizedRegionName(for: selectedRegion!) : String(localized: "region_all"))
                                    .font(.system(size: 14, weight: .medium))
                            }
                            .foregroundColor(selectedRegion != nil ? Color(red: 0.098, green: 0.463, blue: 0.824) : ThemeColors.secondaryText(colorScheme))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(
                                Capsule()
                                    .fill(ThemeColors.surface(colorScheme))
                            )
                        }
                        
                        // Tri
                        Menu {
                            ForEach(SortOption.allCases, id: \.self) { option in
                                Button(action: { sortOption = option }) {
                                    HStack {
                                        Text(option.rawValue)
                                        if sortOption == option {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                }
                            }
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: "arrow.up.arrow.down")
                                    .font(.system(size: 14))
                                Text(sortOption.rawValue)
                                    .font(.system(size: 14, weight: .medium))
                            }
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(
                                Capsule()
                                    .fill(ThemeColors.surface(colorScheme))
                            )
                        }
                        
                        Spacer()
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 12)
                .background(
                    ThemeColors.background(colorScheme)
                )
                
                // Liste des destinations
                switch catalogViewModel.uiState {
                case .loading:
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                case .success(let destinations, let fromCache, _, _):
                    let filteredDestinations = filterDestinations(destinations, by: selectedRegion)
                    let sortedDestinations = sortDestinations(filteredDestinations, by: sortOption)
                    
                    if fromCache {
                        HStack(spacing: 8) {
                            Image(systemName: "wifi.slash")
                                .foregroundColor(Color(red: 0.937, green: 0.188, blue: 0.0))
                            Text("home_offline_cache")
                                .font(.caption)
                                .foregroundColor(Color(red: 0.937, green: 0.188, blue: 0.0))
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 8)
                    }
                    
                    if sortedDestinations.isEmpty {
                        VStack(spacing: 16) {
                            Image(systemName: "airplane.departure")
                                .font(.system(size: 48))
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            Text("home_no_destinations")
                                .font(.headline)
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            Text("home_try_change_filters")
                                .font(.subheadline)
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 16) {
                                ForEach(sortedDestinations) { destination in
                                    ComparisonCard(
                                        destination: destination,
                                        favoritesViewModel: favoritesViewModel
                                    )
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.top, 12)
                            .padding(.bottom, 20)
                        }
                    }
                case .error(let message):
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 48))
                            .foregroundColor(.orange)
                        Text("home_error")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        Text(message)
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button(action: {
                            Task {
                                await catalogViewModel.loadRecommendedFlights(showAll: true)
                            }
                        }) {
                            Text("generic_retry")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 12)
                                .background(ThemeColors.accent())
                                .clipShape(Capsule())
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                case .idle:
                    EmptyView()
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await catalogViewModel.loadRecommendedFlights(showAll: true)
        }
        .onChange(of: selectedRegion) {
            Task {
                await catalogViewModel.loadRecommendedFlights(showAll: true)
            }
        }
    }
    
    private func localizedRegionName(for key: String) -> String {
        switch key {
        case "Preferences": return String(localized: "region_preferences")
        case "Europe": return String(localized: "region_europe")
        case "Asie": return String(localized: "region_asia")
        case "Amerique": return String(localized: "region_america")
        case "Australie": return String(localized: "region_australia")
        default: return key
        }
    }
    
    private func filterDestinations(_ destinations: [FlightDestination], by region: String?) -> [FlightDestination] {
        guard let region = region, region != "Preferences" else {
            return destinations
        }
        
        let regionCountries: [String: [String]] = [
            "Europe": ["France", "United Kingdom", "Italy", "Spain", "Netherlands", "Germany", "Switzerland", "Belgium", "Portugal", "Greece", "Austria", "Sweden", "Norway", "Denmark", "Finland", "Poland", "Czech Republic", "Hungary", "Ireland"],
            "Asie": ["China", "Japan", "India", "Thailand", "Singapore", "Malaysia", "Indonesia", "South Korea", "Vietnam", "Philippines", "UAE", "United Arab Emirates", "Saudi Arabia", "Turkey", "Israel"],
            "Amerique": ["United States", "USA", "Canada", "Mexico", "Brazil", "Argentina", "Chile", "Colombia", "Peru"],
            "Australie": ["Australia", "New Zealand", "Fiji"]
        ]
        
        guard let countries = regionCountries[region] else {
            return destinations
        }
        
        return destinations.filter { destination in
            countries.contains { country in
                // Use contains (like Android) instead of localizedCaseInsensitiveContains
                // This allows partial matching (e.g., "United Arab Emirates" matches "UAE")
                destination.country.localizedCaseInsensitiveContains(country) || 
                country.localizedCaseInsensitiveContains(destination.country)
            }
        }
    }
    
    private func sortDestinations(_ destinations: [FlightDestination], by option: SortOption) -> [FlightDestination] {
        switch option {
        case .price:
            let maxValue = Double.greatestFiniteMagnitude
            return destinations.sorted { ($0.price ?? maxValue) < ($1.price ?? maxValue) }
        case .name:
            return destinations.sorted { $0.name < $1.name }
        case .country:
            return destinations.sorted { $0.country < $1.country }
        }
    }
}

// MARK: - ComparisonCard
struct ComparisonCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let destination: FlightDestination
    @ObservedObject var favoritesViewModel: FavoriteViewModel
    
    @State private var isFavorite: Bool = false
    
    var body: some View {
        NavigationLink(destination: FlightDetailScreen(destinationId: destination.id, destination: destination)) {
            HStack(spacing: 12) {
                // Image
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
                                    .fill(LinearGradient(
                                        colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ))
                            @unknown default:
                                Rectangle()
                                    .fill(LinearGradient(
                                        colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ))
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
                .frame(width: 100, height: 100)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                
                // Informations
                VStack(alignment: .leading, spacing: 6) {
                    Text(destination.name)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        .lineLimit(1)
                    
                    HStack(spacing: 4) {
                        Image(systemName: "location.fill")
                            .font(.system(size: 12))
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        Text("\(destination.city), \(destination.country)")
                            .font(.system(size: 14))
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            .lineLimit(1)
                    }
                    
                    if let airline = destination.airline {
                        HStack(spacing: 4) {
                            Image(systemName: "airplane")
                                .font(.system(size: 12))
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            Text(airline)
                                .font(.system(size: 13))
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        }
                    }
                }
                
                Spacer()
                
                // Prix et favori
                VStack(alignment: .trailing, spacing: 8) {
                    if let price = destination.price, price > 0 {
                        VStack(alignment: .trailing, spacing: 2) {
                            Text("\(Int(price))")
                                .font(.system(size: 22, weight: .bold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            Text(destination.currency)
                                .font(.system(size: 12))
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        }
                    } else {
                        Text("generic_not_available")
                            .font(.system(size: 14))
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                    
                    // Bouton favori
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
                        Image(systemName: isFavorite ? "heart.fill" : "heart")
                            .foregroundColor(isFavorite ? Color(red: 1.0, green: 0.09, blue: 0.267) : ThemeColors.secondaryText(colorScheme))
                            .font(.system(size: 20))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(ThemeColors.surface(colorScheme))
            )
            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 8, x: 0, y: 4)
        }
        .buttonStyle(.plain)
        .task {
            isFavorite = await favoritesViewModel.checkFavorite(itemType: .flight, itemId: destination.id)
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

// MARK: - Notification Navigation Helper
extension HomeScreen {
    private func setupNotificationNavigation() {
        // Écouter les notifications tapées pour naviguer
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("FCMNotificationTapped"),
            object: nil,
            queue: .main
        ) { notification in
            guard let userInfo = notification.userInfo else { return }
            
            let type = userInfo["type"] as? String ?? ""
            let postId = userInfo["postId"] as? String
            let commentId = userInfo["commentId"] as? String
            let journeyId = userInfo["journeyId"] as? String
            
            print("🧭 [HomeScreen] Handling notification navigation")
            print("   Type: \(type)")
            print("   PostId: \(postId ?? "nil")")
            print("   CommentId: \(commentId ?? "nil")")
            
            // Naviguer vers les discussions si c'est un commentaire ou un like
            if type == "post_commented" || type == "post_liked" || type == "journey_commented" || type == "journey_liked" {
                if let postId = postId {
                    // Changer vers l'onglet explore et afficher DiscussionView
                    showDiscussionView = true
                    selectedTab = .explore
                    
                    // Naviguer vers le post spécifique après un court délai
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        navigateToPostId = postId
                    }
                }
            }
        }
    }
}

// MARK: - Onboarding Reminder Card (like Android)
struct OnboardingReminderCard: View {
    let onStart: () -> Void
    let onContinue: () -> Void
    let colorScheme: ColorScheme
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("home_onboarding_title")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            
            Text("home_onboarding_message")
                .font(.system(size: 14))
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            
            HStack(spacing: 12) {
                Button(action: onStart) {
                    Text("home_onboarding_fill_form")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(ThemeColors.accent())
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
                
                Button(action: onContinue) {
                    Text("home_onboarding_continue")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 10, x: 0, y: 6)
        )
    }
}

// Vue wrapper pour DiscussionView avec navigation automatique vers un post
struct DiscussionViewWithNavigation: View {
    @Binding var navigateToPostId: String?
    let onDismiss: () -> Void
    @StateObject private var viewModel = DiscussionViewModel()
    @State private var selectedPostId: String? = nil
    
    var body: some View {
        DiscussionView()
            .onChange(of: navigateToPostId) { oldValue, newValue in
                if let postId = newValue {
                    // Ouvrir les commentaires du post
                    selectedPostId = postId
                    navigateToPostId = nil // Réinitialiser
                }
            }
            .sheet(item: Binding(
                get: { selectedPostId.map { PostCommentItem(id: $0) } },
                set: { selectedPostId = $0?.id }
            )) { item in
                PostCommentsView(postId: item.id, viewModel: viewModel)
            }
    }
}
