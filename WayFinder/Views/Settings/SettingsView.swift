//
//  SettingsView.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

struct SettingsView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var languageManager: LanguageManager
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 16) {
                        // Section Langue
                        VStack(alignment: .leading, spacing: 12) {
                            Text(String(localized: "settings_language_title"))
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                .padding(.horizontal, 4)
                            
                            VStack(spacing: 8) {
                                ForEach(AppLanguage.allCases) { language in
                                    Button(action: {
                                        languageManager.selectedLanguage = language
                                    }) {
                                        HStack(spacing: 16) {
                                            Image(systemName: language == languageManager.selectedLanguage ? "checkmark.circle.fill" : "circle")
                                                .font(.system(size: 20))
                                                .foregroundColor(language == languageManager.selectedLanguage ? ThemeColors.accent() : ThemeColors.secondaryText(colorScheme))
                                            
                                            Text(language.localizedNameKey)
                                                .font(.system(size: 16, weight: .regular))
                                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                                            
                                            Spacer()
                                        }
                                        .padding(18)
                                        .background(
                                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                                .fill(ThemeColors.surface(colorScheme))
                                                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 10, x: 0, y: 6)
                                        )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 8)
                    }
                    .padding(.vertical, 20)
                }
            }
            .navigationTitle(String(localized: "profile_settings"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "profile_logout_cancel")) {
                        dismiss()
                    }
                }
            }
        }
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView()
            .environmentObject(LanguageManager())
    }
}

