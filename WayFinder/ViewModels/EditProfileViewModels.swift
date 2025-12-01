//
//  EditProfileViewModels.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import Foundation

@MainActor
final class EditNameViewModel: ObservableObject {
    @Published var firstName: String = ""
    @Published var lastName: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var success: Bool = false
    
    private let userService: UserService
    
    init(userService: UserService = UserService.shared) {
        self.userService = userService
    }
    
    func loadProfile() async {
        isLoading = true
        defer { isLoading = false }
        
        do {
            let profile = try await userService.fetchProfile()
            firstName = profile.firstName ?? ""
            lastName = profile.lastName ?? ""
        } catch {
            errorMessage = "Erreur lors du chargement du profil: \(error.localizedDescription)"
        }
    }
    
    func updateName() async {
        guard !firstName.isEmpty && !lastName.isEmpty else {
            errorMessage = "Veuillez remplir tous les champs"
            return
        }
        
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            _ = try await userService.updateName(firstName: firstName, lastName: lastName)
            success = true
        } catch {
            errorMessage = "Erreur lors de la mise à jour: \(error.localizedDescription)"
        }
    }
}

@MainActor
final class ChangePasswordViewModel: ObservableObject {
    @Published var currentPassword: String = ""
    @Published var newPassword: String = ""
    @Published var confirmPassword: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var success: Bool = false
    
    private let userService: UserService
    
    init(userService: UserService = UserService.shared) {
        self.userService = userService
    }
    
    var isValid: Bool {
        !currentPassword.isEmpty &&
        !newPassword.isEmpty &&
        !confirmPassword.isEmpty &&
        newPassword == confirmPassword &&
        newPassword.count >= 8
    }
    
    func updatePassword() async {
        guard isValid else {
            if newPassword != confirmPassword {
                errorMessage = "Les mots de passe ne correspondent pas"
            } else if newPassword.count < 8 {
                errorMessage = "Le mot de passe doit contenir au moins 8 caractères"
            } else {
                errorMessage = "Veuillez remplir tous les champs"
            }
            return
        }
        
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            try await userService.updatePassword(currentPassword: currentPassword, newPassword: newPassword)
            success = true
        } catch {
            errorMessage = "Erreur lors de la mise à jour: \(error.localizedDescription)"
        }
    }
}

@MainActor
final class ChangeEmailViewModel: ObservableObject {
    @Published var email: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var success: Bool = false
    
    private let userService: UserService
    
    init(userService: UserService = UserService.shared) {
        self.userService = userService
    }
    
    var isValidEmail: Bool {
        return EmailValidator.isValid(email)
    }
    
    func loadProfile() async {
        isLoading = true
        defer { isLoading = false }
        
        do {
            let profile = try await userService.fetchProfile()
            email = profile.email ?? ""
        } catch {
            errorMessage = "Erreur lors du chargement du profil: \(error.localizedDescription)"
        }
    }
    
    func updateEmail() async {
        guard isValidEmail else {
            errorMessage = "Veuillez entrer une adresse e-mail valide"
            return
        }
        
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            _ = try await userService.updateEmail(email: email)
            success = true
        } catch {
            errorMessage = "Erreur lors de la mise à jour: \(error.localizedDescription)"
        }
    }
}

