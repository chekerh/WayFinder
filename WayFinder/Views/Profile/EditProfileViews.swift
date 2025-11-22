//
//  EditProfileViews.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

// MARK: - Edit Name View
struct EditNameView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = EditNameViewModel()
    
    var body: some View {
        NavigationStack {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 24) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Prénom")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        TextField("Entrez votre prénom", text: $viewModel.firstName)
                            .textFieldStyle(CustomTextFieldStyle(colorScheme: colorScheme))
                    }
                    
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Nom")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        TextField("Entrez votre nom", text: $viewModel.lastName)
                            .textFieldStyle(CustomTextFieldStyle(colorScheme: colorScheme))
                    }
                    
                    if let error = viewModel.errorMessage {
                        Text(error)
                            .font(.subheadline)
                            .foregroundColor(.red)
                            .padding(.horizontal)
                    }
                    
                    Button(action: {
                        Task {
                            await viewModel.updateName()
                            if viewModel.success {
                                dismiss()
                            }
                        }
                    }) {
                        Text("Enregistrer")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(ThemeColors.accent())
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .disabled(viewModel.isLoading || viewModel.firstName.isEmpty || viewModel.lastName.isEmpty)
                    .opacity(viewModel.isLoading || viewModel.firstName.isEmpty || viewModel.lastName.isEmpty ? 0.6 : 1.0)
                }
                .padding(20)
            }
        }
        .navigationTitle("Modifier le nom")
        .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") {
                        dismiss()
                    }
                }
            }
        .task {
            await viewModel.loadProfile()
            }
        }
    }
}

// MARK: - Change Password View
struct ChangePasswordView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ChangePasswordViewModel()
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Mot de passe actuel")
                                .font(.headline)
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            SecureField("Entrez votre mot de passe actuel", text: $viewModel.currentPassword)
                                .textFieldStyle(CustomTextFieldStyle(colorScheme: colorScheme))
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Nouveau mot de passe")
                                .font(.headline)
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            SecureField("Entrez votre nouveau mot de passe", text: $viewModel.newPassword)
                                .textFieldStyle(CustomTextFieldStyle(colorScheme: colorScheme))
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Confirmer le nouveau mot de passe")
                                .font(.headline)
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            SecureField("Confirmez votre nouveau mot de passe", text: $viewModel.confirmPassword)
                                .textFieldStyle(CustomTextFieldStyle(colorScheme: colorScheme))
                        }
                        
                        if let error = viewModel.errorMessage {
                            Text(error)
                                .font(.subheadline)
                                .foregroundColor(.red)
                                .padding(.horizontal)
                        }
                        
                        Button(action: {
                            Task {
                                await viewModel.updatePassword()
                                if viewModel.success {
                                    dismiss()
                                }
                            }
                        }) {
                            Text("Changer le mot de passe")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(ThemeColors.accent())
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .disabled(viewModel.isLoading || !viewModel.isValid)
                        .opacity(viewModel.isLoading || !viewModel.isValid ? 0.6 : 1.0)
                    }
                    .padding(20)
                }
            }
            .navigationTitle("Changer le mot de passe")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") {
                        dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Change Email View
struct ChangeEmailView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ChangeEmailViewModel()
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Nouvelle adresse e-mail")
                                .font(.headline)
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            TextField("Entrez votre nouvelle adresse e-mail", text: $viewModel.email)
                                .textFieldStyle(CustomTextFieldStyle(colorScheme: colorScheme))
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                        }
                        
                        if let error = viewModel.errorMessage {
                            Text(error)
                                .font(.subheadline)
                                .foregroundColor(.red)
                                .padding(.horizontal)
                        }
                        
                        Button(action: {
                            Task {
                                await viewModel.updateEmail()
                                if viewModel.success {
                                    dismiss()
                                }
                            }
                        }) {
                            Text("Changer l'adresse e-mail")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(ThemeColors.accent())
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .disabled(viewModel.isLoading || !viewModel.isValidEmail)
                        .opacity(viewModel.isLoading || !viewModel.isValidEmail ? 0.6 : 1.0)
                    }
                    .padding(20)
                }
            }
            .navigationTitle("Modifier l'adresse e-mail")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") {
                        dismiss()
                    }
                }
            }
            .task {
                await viewModel.loadProfile()
            }
        }
    }
}

// MARK: - Edit Profile View (Main View)
struct EditProfileView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @State private var showEditName = false
    @State private var showChangeEmail = false
    @State private var showChangePassword = false
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 16) {
                    // Modifier le nom
                    Button(action: {
                        showEditName = true
                    }) {
                        ProfileEditOptionCard(
                            icon: "pencil",
                            title: String(localized: "profile_edit_name"),
                            colorScheme: colorScheme
                        )
                    }
                    .buttonStyle(.plain)
                    
                    // Modifier l'adresse e-mail
                    Button(action: {
                        showChangeEmail = true
                    }) {
                        ProfileEditOptionCard(
                            icon: "envelope",
                            title: String(localized: "profile_change_email"),
                            colorScheme: colorScheme
                        )
                    }
                    .buttonStyle(.plain)
                    
                    // Changer le mot de passe
                    Button(action: {
                        showChangePassword = true
                    }) {
                        ProfileEditOptionCard(
                            icon: "lock",
                            title: String(localized: "profile_change_password"),
                            colorScheme: colorScheme
                        )
                    }
                    .buttonStyle(.plain)
                }
                .padding(20)
            }
        }
        .navigationTitle(String(localized: "profile_edit_profile"))
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showEditName) {
            EditNameView()
                .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $showChangeEmail) {
            ChangeEmailView()
                .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $showChangePassword) {
            ChangePasswordView()
                .presentationDetents([.medium, .large])
        }
    }
}

// MARK: - Profile Edit Option Card
struct ProfileEditOptionCard: View {
    let icon: String
    let title: String
    let colorScheme: ColorScheme
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(ThemeColors.accent())
                .frame(width: 32, height: 32)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(ThemeColors.surface(colorScheme).opacity(0.5))
                )
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.08), radius: 4, x: 0, y: 3)
            
            Text(title)
                .font(.system(size: 16, weight: .regular))
                .foregroundColor(ThemeColors.primaryText(colorScheme))
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

// MARK: - Custom Text Field Style
struct CustomTextFieldStyle: TextFieldStyle {
    let colorScheme: ColorScheme
    
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(16)
            .background(ThemeColors.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(ThemeColors.border(colorScheme), lineWidth: 1)
            )
    }
}

