//
//  ProfileViewModel.swift
//  WayFinder
//
//  Created by sarrachmek on 17/11/2025.
//

import Foundation

@MainActor
final class ProfileViewModel: ObservableObject {
    @Published private(set) var displayName: String = UserStorage.fetchDisplayName() ?? "Utilisateur"
    @Published private(set) var profile: UserProfile?
    @Published var isLoading = false
    @Published var isUploadingImage = false
    @Published var errorMessage: String?
    
    nonisolated(unsafe) private var userService: UserService!
    private let profileImageService: ProfileImageService
    
    nonisolated init(userService: UserService? = nil,
                     profileImageService: ProfileImageService? = nil) {
        // Assigner userService (UserService n'est pas @MainActor)
        // Utiliser nonisolated(unsafe) pour contourner l'isolation MainActor
        if let userService = userService {
            nonisolated(unsafe) let captured = userService
            self.userService = captured
        } else {
            nonisolated(unsafe) let captured = UserService.shared
            self.userService = captured
        }
        
        // Assigner profileImageService (ProfileImageService est @MainActor)
        if let profileImageService = profileImageService {
            self.profileImageService = profileImageService
        } else {
            // Utiliser MainActor.assumeIsolated pour accéder à .shared (ProfileImageService est Sendable)
            self.profileImageService = MainActor.assumeIsolated {
                ProfileImageService.shared
            }
        }
        
        // Charger l'image persistée au démarrage
        Task { @MainActor in
            self.profileImageService.loadPersistedImage()
        }
    }
    
    func loadProfile() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        do {
            let fetchedProfile = try await userService.fetchProfile()
            profile = fetchedProfile
            displayName = fetchedProfile.displayNameValue
            
            // S'assurer que l'image est synchronisée dans UserStorage et le service centralisé
            if let imageUrl = fetchedProfile.resolvedProfileImageUrl {
                UserStorage.saveProfile(fetchedProfile)
                
                // Mettre à jour le service centralisé
                profileImageService.updateProfileImage(imageUrl, email: fetchedProfile.email)
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func uploadProfileImage(imageData: Data) async {
        isUploadingImage = true
        defer { isUploadingImage = false }
        errorMessage = nil
        
        do {
            let response = try await userService.uploadProfileImage(imageData: imageData)
            let newImageUrl = response.profileImageUrl
            
            // Mettre à jour le profil avec la nouvelle image
            if let updatedUser = response.user {
                profile = updatedUser
                displayName = updatedUser.displayNameValue
            } else {
                // Recharger le profil pour obtenir les données à jour
                // Attendre un peu pour que le backend ait fini de traiter
                try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 secondes
                await loadProfile()
            }
            
            // Mettre à jour le service centralisé (qui gère automatiquement les notifications)
            let email = response.user?.email ?? profile?.email
            profileImageService.updateProfileImage(newImageUrl, email: email)
            
            print("✅ [ProfileViewModel] Profile image updated and synchronized: \(newImageUrl)")
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [ProfileViewModel] Error uploading image: \(error.localizedDescription)")
            // Même en cas d'erreur de décodage, si l'upload a réussi (status 201),
            // on peut quand même recharger le profil
            await loadProfile()
        }
    }
    
    var profileImageUrl: String? {
        // Toujours retourner l'URL depuis le service centralisé en priorité (sauf image de test)
        if let serviceUrl = profileImageService.profileImageUrl, !serviceUrl.isEmpty, !serviceUrl.contains("pravatar.cc") {
            return serviceUrl
        }
        // Puis depuis UserStorage (sauf image de test)
        if let storedUrl = UserStorage.fetchProfileImageUrl(), !storedUrl.isEmpty, !storedUrl.contains("pravatar.cc") {
            // Mettre à jour le service avec l'URL stockée
            profileImageService.updateProfileImage(storedUrl, email: nil)
            return storedUrl
        }
        // Enfin depuis le profil
        return profile?.resolvedProfileImageUrl
    }
    
    var userId: String? {
        return profile?.id
    }
}

