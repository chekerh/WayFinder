//
//  profil.swift
//  WayFinder
//
//  Created by sarrachmek on 7/11/2025.
//

import SwiftUI
import PhotosUI
import UIKit

@MainActor
struct ProfileView: View {
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = ProfileViewModel()
    @AppStorage(UserStorage.profileImageUrlKey) private var cachedProfileImageUrlRaw: String = ""
    @State private var showLogoutConfirmation = false
    @State private var showAuthFlow = false
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var selectedImageData: Data?
    @State private var showSettings = false
    @State private var showSurveyScreen = false
    
    private var actions: [ProfileAction] {
        var actionsList: [ProfileAction] = [
            .init(icon: "slider.horizontal.3", titleKey: "profile_preferences"),
        ]
        
        // Add "Retake Onboarding" option if user has completed onboarding
        if viewModel.profile?.onboardingCompleted == true {
            actionsList.append(.init(icon: "arrow.triangle.2.circlepath", titleKey: "profile_retake_onboarding", destination: .retakeOnboarding))
        }
        
        actionsList.append(contentsOf: [
            .init(icon: "pencil", titleKey: "profile_edit_profile", destination: .editProfile),
            .init(icon: "square.and.arrow.up", titleKey: "profile_share_trip", destination: .shareTrip),
            .init(icon: "photo.on.rectangle.angled", titleKey: "profile_shared_journeys", destination: .sharedJourneys),
            .init(icon: "gearshape", titleKey: "profile_settings", destination: .settings),
            .init(icon: "arrow.right.square", titleKey: "profile_logout", isDestructive: true)
        ])
        
        return actionsList
    }
    
    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 24) {
                header
                    .padding(.horizontal, 24)
                    .padding(.top, 24)
                    .safeAreaPadding(.top, 8)
                
                VStack(spacing: 12) {
                    ForEach(actions) { action in
                        if let destination = action.destination {
                            if destination == .settings {
                                Button(action: {
                                    showSettings = true
                                }) {
                                    ProfileRowContent(action: action)
                                }
                                .buttonStyle(.plain)
                                .padding(.horizontal, 24)
                            } else if destination == .retakeOnboarding {
                                Button(action: {
                                    showSurveyScreen = true
                                }) {
                                    ProfileRowContent(action: action)
                                }
                                .buttonStyle(.plain)
                                .padding(.horizontal, 24)
                            } else {
                                NavigationLink(destination: destinationView(for: destination)) {
                                    ProfileRowContent(action: action)
                                }
                                .buttonStyle(.plain)
                                .padding(.horizontal, 24)
                            }
                        } else {
                            Button(action: {
                                handleAction(action)
                            }) {
                                ProfileRowContent(action: action)
                            }
                            .buttonStyle(.plain)
                            .padding(.horizontal, 24)
                        }
                    }
                }
                .padding(.bottom, 32)
            }
        }
        .safeAreaPadding(.bottom, 64)
        .background(ThemeColors.background(colorScheme).ignoresSafeArea())
        .alert(String(localized: "profile_logout_confirm_title"), isPresented: $showLogoutConfirmation) {
            Button(role: .destructive) {
                performLogout()
            } label: {
                Text("profile_logout")
            }
            
            Button(role: .cancel) { } label: {
                Text("profile_logout_cancel")
            }
        } message: {
            Text("profile_logout_confirm_message")
        }
        .fullScreenCover(isPresented: $showAuthFlow) {
            WelcomeView()
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
                .presentationDetents([.medium, .large])
        }
        .fullScreenCover(isPresented: $showSurveyScreen) {
            NavigationStack {
                SurveyScreenWithCompletionWrapper(onComplete: {
                    showSurveyScreen = false
                    // Reload profile to update onboarding status
                    Task {
                        await viewModel.loadProfile()
                    }
                })
            }
        }
        .task {
            // Charger l'image persistée avant de charger le profil
            await MainActor.run {
                ProfileImageService.shared.loadPersistedImage()
            }
            await viewModel.loadProfile()
            // S'assurer que @AppStorage est synchronisé après le chargement
            await MainActor.run {
                if let imageUrl = viewModel.profileImageUrl {
                    cachedProfileImageUrlRaw = imageUrl
                }
            }
        }
        .onAppear {
            // Recharger l'image depuis le service centralisé à l'apparition
            Task { @MainActor in
                ProfileImageService.shared.loadPersistedImage()
                if let serviceUrl = ProfileImageService.shared.profileImageUrl, !serviceUrl.contains("pravatar.cc") {
                    cachedProfileImageUrlRaw = serviceUrl
                } else if let storedUrl = UserStorage.fetchProfileImageUrl(), !storedUrl.contains("pravatar.cc") {
                    cachedProfileImageUrlRaw = storedUrl
                } else {
                    // Si c'est une image de test, la supprimer
                    cachedProfileImageUrlRaw = ""
                }
                let profileUrl = viewModel.profileImageUrl
                let cachedUrl = currentCachedImageUrl
                if (profileUrl ?? cachedUrl) != nil {
                    selectedImageData = nil
                }
            }
        }
    }
    
    private var header: some View {
        // Capturer les valeurs MainActor avant la closure
        let profileImageUrl = viewModel.profileImageUrl
        let cachedImageUrl = currentCachedImageUrl
        let isUploading = viewModel.isUploadingImage
        let scheme = colorScheme
        let imageData = selectedImageData
        
        // Construire l'URL avant la closure si nécessaire
        let imageUrl: URL? = {
            if let url = profileImageUrl ?? cachedImageUrl {
                // Construire l'URL directement sans appeler buildImageURL depuis un contexte non isolé
                if url.hasPrefix("http://") || url.hasPrefix("https://") {
                    return URL(string: url)
                }
                let baseURL = "https://wayfinder-api-w92x.onrender.com"
                return URL(string: "\(baseURL)\(url)")
            }
            return nil
        }()
        
        return VStack(spacing: 18) {
            // Top row: Points (left) - Photo (center) - Lifetime (right)
            HStack(spacing: 16) {
                // Points (left)
                CompactPointsView(
                    totalPoints: 0, // TODO: Replace with viewModel.profile?.totalPoints ?? 0
                    scheme: scheme
                )
                
                // Photo (center)
                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                    ZStack {
                        Circle()
                            .fill(ThemeColors.surface(scheme))
                            .frame(width: 132, height: 132)
                            .shadow(color: Color.black.opacity(scheme == .dark ? 0.25 : 0.08), radius: 18, x: 0, y: 8)
                            .overlay(
                                Circle()
                                    .strokeBorder(
                                        LinearGradient(
                                            colors: [
                                                Color.white.opacity(scheme == .dark ? 0.4 : 0.9),
                                                ThemeColors.accent()
                                            ],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        ),
                                        lineWidth: 3
                                    )
                            )
                        
                        if let imageData = imageData, let uiImage = UIImage(data: imageData) {
                            Image(uiImage: uiImage)
                                .resizable()
                                .scaledToFill()
                                .frame(width: 120, height: 120)
                                .clipShape(Circle())
                        } else if let url = imageUrl {
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .success(let image):
                                    image
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: 120, height: 120)
                                        .clipShape(Circle())
                                case .failure, .empty:
                                    profilePlaceholder(for: scheme)
                                @unknown default:
                                    profilePlaceholder(for: scheme)
                                }
                            }
                        } else {
                            profilePlaceholder(for: scheme)
                        }
                        
                        if isUploading {
                            Circle()
                                .fill(Color.black.opacity(0.35))
                                .frame(width: 120, height: 120)
                            ProgressView()
                                .tint(.white)
                        }
                    }
                }
                .buttonStyle(.plain)
                .disabled(viewModel.isUploadingImage)
                
                // Lifetime (right)
                CompactLifetimeView(
                    lifetimePoints: 0, // TODO: Replace with viewModel.profile?.lifetimePoints ?? 0
                    scheme: scheme
                )
            }
            .frame(maxWidth: .infinity)
            
            // Day Streak (below photo)
            CompactDayStreakView(
                currentStreak: 0, // TODO: Replace with viewModel.profile?.currentStreak ?? 0
                longestStreak: 0, // TODO: Replace with viewModel.profile?.longestStreak ?? 0
                scheme: scheme
            )
            
            VStack(spacing: 4) {
                Text(viewModel.displayName)
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundColor(ThemeColors.primaryText(scheme))
                
                if let email = viewModel.profile?.email, !email.isEmpty {
                    Text(email)
                        .font(.subheadline)
                        .foregroundColor(ThemeColors.secondaryText(scheme))
                }
            }
            
        }
        .padding(.vertical, 28)
        .padding(.horizontal, 24)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(ThemeColors.surface(scheme))
                .shadow(color: Color.black.opacity(scheme == .dark ? 0.25 : 0.08), radius: 24, x: 0, y: 12)
        )
        .onChange(of: selectedPhoto) { _, newValue in
            Task {
                guard let newValue = newValue else { return }
                if let data = try? await newValue.loadTransferable(type: Data.self),
                   let preparedData = prepareImageData(data) {
                    await MainActor.run {
                        selectedImageData = preparedData
                    }
                    // Upload immédiatement l'image
                    await viewModel.uploadProfileImage(imageData: preparedData)
                    try? await Task.sleep(nanoseconds: 300_000_000)
                    await MainActor.run {
                        if (viewModel.profileImageUrl ?? currentCachedImageUrl) != nil {
                            selectedImageData = nil
                        }
                    }
                }
            }
        }
        .onChange(of: viewModel.profileImageUrl) { oldValue, newValue in
            // Quand l'URL de l'image change (après upload), réinitialiser selectedImageData
            if newValue != nil && newValue != oldValue {
                selectedImageData = nil
                // Mettre à jour @AppStorage pour synchronisation
                if let newUrl = newValue {
                    cachedProfileImageUrlRaw = newUrl
                    UserDefaults.standard.set(newUrl, forKey: UserStorage.profileImageUrlKey)
                    UserDefaults.standard.synchronize()
                }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("UserProfileImageDidUpdate"))) { notification in
            // Mettre à jour @AppStorage quand l'image change
            if let newUrl = notification.userInfo?["profileImageUrl"] as? String {
                cachedProfileImageUrlRaw = newUrl
            } else if let newUrl = UserStorage.fetchProfileImageUrl() {
                cachedProfileImageUrlRaw = newUrl
            }
        }
    }
    
    private var currentCachedImageUrl: String? {
        cachedProfileImageUrlRaw.isEmpty ? nil : cachedProfileImageUrlRaw
    }
    
    @ViewBuilder
    private func profilePlaceholder(for scheme: ColorScheme) -> some View {
        Circle()
            .fill(ThemeColors.surface(scheme))
            .overlay(
                Image(systemName: "person.fill")
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(ThemeColors.accent())
                    .padding(26)
            )
            .frame(width: 120, height: 120)
    }
    
    private func buildImageURL(from urlString: String) -> URL? {
        // Si l'URL commence par http, utiliser tel quel
        if urlString.hasPrefix("http://") || urlString.hasPrefix("https://") {
            return URL(string: urlString)
        }
        // Sinon, construire l'URL complète avec le backend
        let baseURL = "https://wayfinder-api-w92x.onrender.com"
        return URL(string: "\(baseURL)\(urlString)")
    }
}

private enum ProfileDestination {
    case bookingHistory
    case editProfile
    case editName
    case changePassword
    case changeEmail
    case settings
    case shareTrip
    case sharedJourneys
    case retakeOnboarding
}

private struct ProfileAction: Identifiable {
    let id = UUID()
    let icon: String
    let titleKey: String
    var destination: ProfileDestination? = nil
    var isDestructive: Bool = false
}

private struct ProfileRowContent: View {
    @Environment(\.colorScheme) private var colorScheme
    let action: ProfileAction
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: action.icon)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(action.isDestructive ? Color(red: 0.82, green: 0.18, blue: 0.12) : ThemeColors.accent())
                .frame(width: 32, height: 32)
                .background(
                    Group {
                        if action.isDestructive {
                            // No background for destructive actions (logout)
                            Color.clear
                        } else {
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(ThemeColors.surface(colorScheme).opacity(0.5))
                        }
                    }
                )
                .shadow(color: action.isDestructive ? Color.clear : Color.black.opacity(colorScheme == .dark ? 0.2 : 0.08), radius: 4, x: 0, y: 3)
            
            Text(LocalizedStringKey(action.titleKey))
                .font(.system(size: 16, weight: action.isDestructive ? .semibold : .regular))
                .foregroundColor(action.isDestructive ? Color(red: 0.82, green: 0.18, blue: 0.12) : ThemeColors.primaryText(colorScheme))
                .frame(maxWidth: .infinity, alignment: .leading)
            
            Image(systemName: "chevron.right")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(ThemeColors.secondaryText(colorScheme))
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 10, x: 0, y: 6)
        )
        .contentShape(Rectangle())
    }
}

private extension ProfileView {
    @ViewBuilder
    func destinationView(for destination: ProfileDestination) -> some View {
        switch destination {
        case .bookingHistory:
            BookingHistoryView()
        case .editProfile:
            EditProfileView()
        case .editName:
            EditNameView()
        case .changePassword:
            ChangePasswordView()
        case .changeEmail:
            ChangeEmailView()
        case .settings:
            SettingsView()
        case .shareTrip:
            ShareTripView()
        case .sharedJourneys:
            JourneyFeedView()
        case .retakeOnboarding:
            // This will be handled by fullScreenCover, but we need this for the enum
            EmptyView()
        }
    }
    
    func handleAction(_ action: ProfileAction) {
        if action.isDestructive {
            showLogoutConfirmation = true
        }
        // TODO: add routing for other actions when backend endpoints are ready
    }
    
    func performLogout() {
        // Arrêter le polling des notifications lors de la déconnexion
        FirebaseMessagingService.shared.setMainInterfaceState(false)
        AuthService.shared.logout()
        showAuthFlow = true
    }
    
    func prepareImageData(_ data: Data) -> Data? {
        guard let image = UIImage(data: data) else {
            return data
        }
        
        let maxDimension: CGFloat = 1024
        let maxBytes = 900_000 // ~0.9 MB pour éviter "message trop long"
        
        let largestSide = max(image.size.width, image.size.height)
        let scale = largestSide > maxDimension ? maxDimension / largestSide : 1
        let targetSize = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        let resizedImage = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: targetSize))
        }
        
        var compression: CGFloat = 0.8
        var jpegData = resizedImage.jpegData(compressionQuality: compression)
        
        while let data = jpegData, data.count > maxBytes, compression > 0.3 {
            compression -= 0.1
            jpegData = resizedImage.jpegData(compressionQuality: compression)
        }
        
        return jpegData ?? data
    }
}

// Shared state to pass onComplete callback to SurveyScreen
class SurveyCompletionHandler: ObservableObject {
    static let shared = SurveyCompletionHandler()
    var onComplete: (() -> Void)?
    private init() {}
}

// Wrapper to allow passing onComplete closure to SurveyScreen
struct SurveyScreenWithCompletionWrapper: View {
    let onComplete: (() -> Void)?
    
    var body: some View {
        SurveyScreen()
            .onAppear {
                SurveyCompletionHandler.shared.onComplete = onComplete
            }
            .onDisappear {
                SurveyCompletionHandler.shared.onComplete = nil
            }
    }
}

// MARK: - Compact Points View (left of photo)
struct CompactPointsView: View {
    let totalPoints: Int
    let scheme: ColorScheme
    
    @State private var sparkleScale: CGFloat = 1.0
    
    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                Color(red: 1.0, green: 0.757, blue: 0.027), // #FFC107
                                Color(red: 1.0, green: 0.596, blue: 0.0)    // #FF9800
                            ],
                            center: .center,
                            startRadius: 0,
                            endRadius: 20
                        )
                    )
                    .frame(width: 40, height: 40)
                
                Image(systemName: "trophy.fill")
                    .foregroundColor(.white)
                    .font(.system(size: 20))
                    .scaleEffect(sparkleScale)
            }
            
            Text("\(totalPoints)")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(Color(red: 1.0, green: 0.757, blue: 0.027))
            
            Text("Points")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(ThemeColors.secondaryText(scheme))
        }
        .frame(width: 80)
        .onAppear {
            withAnimation(
                Animation.easeInOut(duration: 1.5)
                    .repeatForever(autoreverses: true)
            ) {
                sparkleScale = 1.15
            }
        }
    }
}

// MARK: - Compact Lifetime View (right of photo)
struct CompactLifetimeView: View {
    let lifetimePoints: Int
    let scheme: ColorScheme
    
    var body: some View {
        VStack(spacing: 6) {
            Text("Lifetime")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(ThemeColors.secondaryText(scheme))
            
            Text("\(lifetimePoints)")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(ThemeColors.primaryText(scheme))
            
            Text("Available now")
                .font(.system(size: 10, weight: .medium))
                .foregroundColor(Color(red: 0.298, green: 0.686, blue: 0.314)) // #4CAF50
        }
        .frame(width: 80)
    }
}

// MARK: - Compact Day Streak View (below photo)
struct CompactDayStreakView: View {
    let currentStreak: Int
    let longestStreak: Int
    let scheme: ColorScheme
    
    @State private var fireScale: CGFloat = 1.0
    
    private var progress: Double {
        min(Double(currentStreak) / 30.0, 1.0)
    }
    
    private var daysUntilMilestone: Int {
        max(30 - currentStreak, 0)
    }
    
    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 6) {
                Image(systemName: "flame.fill")
                    .foregroundColor(Color(red: 1.0, green: 0.341, blue: 0.133)) // #FF5722
                    .font(.system(size: 24))
                    .scaleEffect(fireScale)
                
                Text("\(currentStreak)")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(Color(red: 1.0, green: 0.341, blue: 0.133))
                
                Text("Day Streak")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(ThemeColors.secondaryText(scheme))
            }
            
            // Progress bar
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(
                            Color(red: 1.0, green: 0.341, blue: 0.133)
                                .opacity(scheme == .dark ? 0.25 : 0.2)
                        )
                        .frame(height: 6)
                    
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(red: 1.0, green: 0.341, blue: 0.133))
                        .frame(width: geometry.size.width * progress, height: 6)
                }
            }
            .frame(height: 6)
            .frame(maxWidth: 200)
            
            Text("\(daysUntilMilestone) days until next milestone!")
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(Color(red: 1.0, green: 0.341, blue: 0.133))
                .multilineTextAlignment(.center)
        }
        .onAppear {
            withAnimation(
                Animation.easeInOut(duration: 1.0)
                    .repeatForever(autoreverses: true)
            ) {
                fireScale = 1.1
            }
        }
    }
}

struct ProfileView_Previews: PreviewProvider {
    static var previews: some View {
        ProfileView()
    }
}
