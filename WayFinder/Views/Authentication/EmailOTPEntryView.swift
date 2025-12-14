//
//  EmailOTPEntryView.swift
//  WayFinder
//
//  Created by sarrachmek on 6/11/2025.
//

import SwiftUI

struct EmailOTPEntryView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var email: String = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showOTPScreen = false
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Spacer()
                    
                    // Logo
                    Image("LogoWayFinder")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 120, height: 120)
                        .padding(.bottom, 24)
                    
                    Text("Connexion par code")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                    
                    Text("Entrez votre email pour recevoir un code de vérification à 4 chiffres")
                        .font(.system(size: 14))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                    
                    VStack(spacing: 16) {
                        TextField("Email", text: $email)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                            .textContentType(.emailAddress)
                            .autocorrectionDisabled(true)
                            .padding()
                            .background(Color.white)
                            .cornerRadius(14)
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(errorMessage != nil ? Color.red : ThemeColors.border(colorScheme), lineWidth: 1.5)
                            )
                        
                        if let errorMessage {
                            Text(errorMessage)
                                .font(.caption)
                                .foregroundColor(.red)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        
                        Button(action: {
                            Task { await sendOTP() }
                        }) {
                            if isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("Recevoir le code")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(isValidEmail(email) ? ThemeColors.accent() : Color.gray)
                        .cornerRadius(14)
                        .disabled(!isValidEmail(email) || isLoading)
                    }
                    .padding(.horizontal, 40)
                    .padding(.top, 32)
                    
                    Spacer()
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        dismiss()
                    }) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                }
            }
            .navigationDestination(isPresented: $showOTPScreen) {
                OTPScreenView(email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())
                    .navigationBarBackButtonHidden(true)
            }
        }
    }
    
    @MainActor
    private func sendOTP() async {
        guard isValidEmail(email) else {
            errorMessage = "Email invalide"
            return
        }
        
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            _ = try await AuthService.shared.sendOTP(email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())
            showOTPScreen = true
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    private func isValidEmail(_ email: String) -> Bool {
        return EmailValidator.isValid(email)
    }
}

struct EmailOTPEntryView_Previews: PreviewProvider {
    static var previews: some View {
        EmailOTPEntryView()
    }
}

