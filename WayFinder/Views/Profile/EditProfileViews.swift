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
        .task {
            await viewModel.loadProfile()
        }
    }
}

// MARK: - Change Password View
struct ChangePasswordView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ChangePasswordViewModel()
    
    var body: some View {
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
    }
}

// MARK: - Change Email View
struct ChangeEmailView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ChangeEmailViewModel()
    
    var body: some View {
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
        .task {
            await viewModel.loadProfile()
        }
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

