//
//  ProfileViewModel.swift
//  WayFinder
//
//  Created by sarrachmek on 17/11/2025.
//

import Foundation

@MainActor
final class ProfileViewModel: ObservableObject {
    @Published private(set) var displayName: String = "Utilisateur"
    @Published private(set) var profile: UserProfile?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let userService = UserService.shared
    
    func loadProfile() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        do {
            let fetchedProfile = try await userService.fetchProfile()
            profile = fetchedProfile
            displayName = Self.buildDisplayName(from: fetchedProfile)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    private static func buildDisplayName(from profile: UserProfile) -> String {
        if let firstName = profile.firstName, !firstName.isEmpty,
           let lastName = profile.lastName, !lastName.isEmpty {
            return "\(firstName) \(lastName)"
        }
        if let username = profile.username, !username.isEmpty {
            return username
        }
        if let email = profile.email, !email.isEmpty {
            return email
        }
        return "Utilisateur"
    }
}

