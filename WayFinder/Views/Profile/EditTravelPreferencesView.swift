//
//  EditTravelPreferencesView.swift
//  WayFinder
//
//  Created for travel preferences management
//

import SwiftUI

// MARK: - Edit Travel Preferences View
struct EditTravelPreferencesView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = EditTravelPreferencesViewModel()
    
    // Same preferences list as Android
    private let availablePreferences = [
        "Beach", "Mountain", "City", "Culture", "Adventure", "Relaxation",
        "Food", "Nightlife", "Shopping", "Nature", "History", "Art"
    ]
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        Text("travel_preferences_title")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            .padding(.horizontal)
                        
                        // Preferences Grid (same layout as Android)
                        LazyVGrid(columns: [
                            GridItem(.flexible(), spacing: 12),
                            GridItem(.flexible(), spacing: 12)
                        ], spacing: 12) {
                            ForEach(availablePreferences, id: \.self) { preference in
                                PreferenceChip(
                                    title: preference,
                                    isSelected: viewModel.selectedPreferences.contains(preference),
                                    colorScheme: colorScheme
                                ) {
                                    viewModel.togglePreference(preference)
                                }
                            }
                        }
                        .padding(.horizontal)
                        
                        if let error = viewModel.errorMessage {
                            Text(error)
                                .font(.subheadline)
                                .foregroundColor(.red)
                                .padding(.horizontal)
                        }
                        
                        Button(action: {
                            Task {
                                await viewModel.savePreferences()
                                if viewModel.success {
                                    dismiss()
                                }
                            }
                        }) {
                            Text("travel_preferences_save")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(ThemeColors.accent())
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .disabled(viewModel.isLoading)
                        .opacity(viewModel.isLoading ? 0.6 : 1.0)
                        .padding(.horizontal)
                        .padding(.top, 8)
                    }
                    .padding(.vertical, 20)
                }
            }
            .navigationTitle(String(localized: "edit_profile_travel_preferences_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "edit_profile_cancel")) {
                        dismiss()
                    }
                }
            }
            .task {
                await viewModel.loadPreferences()
            }
        }
    }
}

// MARK: - Preference Chip
struct PreferenceChip: View {
    let title: String
    let isSelected: Bool
    let colorScheme: ColorScheme
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(isSelected ? .white : ThemeColors.primaryText(colorScheme))
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(isSelected ? ThemeColors.accent() : ThemeColors.surface(colorScheme))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(isSelected ? Color.clear : ThemeColors.border(colorScheme), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Preview
#Preview {
    EditTravelPreferencesView()
}

