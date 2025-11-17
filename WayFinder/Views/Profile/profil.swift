//
//  profil.swift
//  WayFinder
//
//  Created by sarrachmek on 7/11/2025.
//

import SwiftUI

struct ProfileView: View {
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = ProfileViewModel()
    @State private var showLogoutConfirmation = false
    @State private var showAuthFlow = false
    
    private let actions: [ProfileAction] = [
        .init(icon: "airplane", titleKey: "profile_bookings", destination: .bookingHistory),
        .init(icon: "pencil", titleKey: "profile_edit_name", destination: .editName),
        .init(icon: "checklist", titleKey: "profile_list_project"),
        .init(icon: "lock", titleKey: "profile_change_password", destination: .changePassword),
        .init(icon: "envelope", titleKey: "profile_change_email", destination: .changeEmail),
        .init(icon: "gearshape", titleKey: "profile_settings"),
        .init(icon: "slider.horizontal.3", titleKey: "profile_preferences"),
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
                            NavigationLink(destination: destinationView(for: destination)) {
                                ProfileRowContent(action: action)
                            }
                            .buttonStyle(.plain)
                            .padding(.horizontal, 24)
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
        .safeAreaPadding(.bottom, 80)
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
        .task {
            await viewModel.loadProfile()
        }
    }
    
    private var header: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(ThemeColors.accentGradient(colorScheme))
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.12), radius: 20, x: 0, y: 10)
            
            VStack(spacing: 16) {
                ZStack(alignment: .bottomTrailing) {
                    Circle()
                        .fill(ThemeColors.surface(colorScheme))
                        .overlay(
                            Image(systemName: "person.fill")
                                .resizable()
                                .scaledToFit()
                                .foregroundColor(ThemeColors.accent())
                                .padding(24)
                        )
                        .frame(width: 120, height: 120)
                    
                    Circle()
                        .fill(Color(red: 0.98, green: 0.82, blue: 0.18))
                        .frame(width: 36, height: 36)
                        .overlay(Image(systemName: "pencil")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(.white))
                        .offset(x: 6, y: 6)
                }
                
                Text(viewModel.displayName)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(.white)
            }
            .padding(.vertical, 32)
        }
        .frame(maxWidth: .infinity)
    }
}

private enum ProfileDestination {
    case bookingHistory
    case editName
    case changePassword
    case changeEmail
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
        case .editName:
            EditNameView()
        case .changePassword:
            ChangePasswordView()
        case .changeEmail:
            ChangeEmailView()
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
        PreferenceStorage.clearPreferenceId()
        showAuthFlow = true
    }
}

struct ProfileView_Previews: PreviewProvider {
    static var previews: some View {
        ProfileView()
    }
}
