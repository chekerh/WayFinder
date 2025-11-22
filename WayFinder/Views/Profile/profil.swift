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
    
    private let actions: [ProfileAction] = [
        .init(icon: "slider.horizontal.3", titleKey: "profile_preferences"),
        .init(icon: "pencil", titleKey: "profile_edit_profile", destination: .editProfile),
        .init(icon: "calendar", titleKey: "profile_list_reservations", destination: .bookingHistory),
        .init(icon: "gearshape", titleKey: "profile_settings", destination: .settings),
        .init(icon: "arrow.right.square", titleKey: "profile_logout", isDestructive: true)
    ]
    
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
        
        return ZStack {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(ThemeColors.accentGradient(scheme))
                .shadow(color: Color.black.opacity(scheme == .dark ? 0.3 : 0.12), radius: 20, x: 0, y: 10)
            
            VStack(spacing: 16) {
                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                ZStack(alignment: .bottomTrailing) {
                        // Image de profil ou placeholder
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
                                    Circle()
                                        .fill(ThemeColors.surface(scheme))
                                        .overlay(
                                            Image(systemName: "person.fill")
                                                .resizable()
                                                .scaledToFit()
                                                .foregroundColor(ThemeColors.accent())
                                                .padding(24)
                                        )
                                        .frame(width: 120, height: 120)
                                @unknown default:
                                    Circle()
                                        .fill(ThemeColors.surface(scheme))
                                        .overlay(
                                            Image(systemName: "person.fill")
                                                .resizable()
                                                .scaledToFit()
                                                .foregroundColor(ThemeColors.accent())
                                                .padding(24)
                                        )
                                        .frame(width: 120, height: 120)
                                }
                            }
                        } else {
                    Circle()
                        .fill(ThemeColors.surface(scheme))
                        .overlay(
                            Image(systemName: "person.fill")
                                .resizable()
                                .scaledToFit()
                                .foregroundColor(ThemeColors.accent())
                                .padding(24)
                        )
                        .frame(width: 120, height: 120)
                        }
                    
                        // Bouton d'édition
                    Circle()
                        .fill(Color(red: 0.98, green: 0.82, blue: 0.18))
                        .frame(width: 36, height: 36)
                            .overlay(
                                Group {
                                    if isUploading {
                                        ProgressView()
                                            .tint(.white)
                                            .scaleEffect(0.7)
                                    } else {
                                        Image(systemName: "camera.fill")
                                            .font(.system(size: 16, weight: .semibold))
                                            .foregroundColor(.white)
                                    }
                                }
                            )
                        .offset(x: 6, y: 6)
                    }
                }
                .disabled(viewModel.isUploadingImage)
                
                Text(viewModel.displayName)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(.white)
            }
            .padding(.vertical, 32)
        }
        .frame(maxWidth: .infinity)
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
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(action.isDestructive ? Color(red: 0.98, green: 0.90, blue: 0.90) : ThemeColors.surface(colorScheme).opacity(0.5))
                )
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.08), radius: 4, x: 0, y: 3)
            
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
        }
    }
    
    func handleAction(_ action: ProfileAction) {
        if action.isDestructive {
            showLogoutConfirmation = true
        }
        // TODO: add routing for other actions when backend endpoints are ready
    }
    
    func performLogout() {
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

struct ProfileView_Previews: PreviewProvider {
    static var previews: some View {
        ProfileView()
    }
}
