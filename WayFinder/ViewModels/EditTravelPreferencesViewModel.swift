//
//  EditTravelPreferencesViewModel.swift
//  WayFinder
//
//  Created for travel preferences management
//

import Foundation

@MainActor
final class EditTravelPreferencesViewModel: ObservableObject {
    @Published var selectedPreferences: Set<String> = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var success: Bool = false
    
    private let userService: UserService
    
    init(userService: UserService = UserService.shared) {
        self.userService = userService
    }
    
    func loadPreferences() async {
        isLoading = true
        defer { isLoading = false }
        
        do {
            let profile = try await userService.fetchProfile()
            selectedPreferences = Set(profile.preferences ?? [])
        } catch {
            errorMessage = "Erreur lors du chargement des préférences: \(error.localizedDescription)"
        }
    }
    
    func togglePreference(_ preference: String) {
        if selectedPreferences.contains(preference) {
            selectedPreferences.remove(preference)
        } else {
            selectedPreferences.insert(preference)
        }
    }
    
    func savePreferences() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            let preferencesArray = Array(selectedPreferences)
            _ = try await userService.updatePreferences(preferences: preferencesArray)
            success = true
        } catch {
            errorMessage = "Erreur lors de la sauvegarde: \(error.localizedDescription)"
        }
    }
}

