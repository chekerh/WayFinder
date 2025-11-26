//
//  signIn.swift
//  WayFinder
//
//  Created by sarrachmek on 6/11/2025.
//

import SwiftUI

struct SignInView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @State private var email: String = ""
    @State private var firstName: String = ""
    @State private var lastName: String = ""
    @State private var password: String = ""
    @State private var confirmPassword: String = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showSuccess = false
    @State private var acceptTerms = false
    @State private var showTermsAlert = false
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 32) {
                Spacer(minLength: 24)
                
                Image("LogoWayFinder")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 160, height: 160)
                    .padding(.top, 16)
                
                VStack(spacing: 16) {
                    inputField(title: LocalizedStringKey("signin_email_placeholder"), text: $email)
                    inputField(title: LocalizedStringKey("signin_first_name_placeholder"), text: $firstName)
                    inputField(title: LocalizedStringKey("signin_last_name_placeholder"), text: $lastName)
                    secureField(title: LocalizedStringKey("signin_password_placeholder"), text: $password)
                    secureField(title: LocalizedStringKey("signin_confirm_password_placeholder"), text: $confirmPassword)
                }
                .padding(.horizontal, 40)
                
                Spacer()
                
                VStack(spacing: 8) {
                    HStack(alignment: .top, spacing: 10) {
                        Button(action: {
                            acceptTerms.toggle()
                        }) {
                            Image(systemName: acceptTerms ? "checkmark.square.fill" : "square")
                                .foregroundColor(acceptTerms ? ThemeColors.accent() : Color.gray)
                                .font(.system(size: 20))
                        }
                        
                        consentText
                            .font(.system(size: 13))
                    }
                    .padding(.horizontal, 24)
                    
                    Button(action: {
                        Task { await submit() }
                    }) {
                            if isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("signin_button")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 24)
                        }
                    }
                        .padding(.vertical, 16)
                        .background(ThemeColors.accent())
                        .clipShape(Capsule())
                    .disabled(isLoading)
                    
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.system(size: 14))
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                    }
                }
                .padding(.bottom, 28)
            }
        }
        .alert(String(localized: "signin_success_title"), isPresented: $showSuccess) {
            Button(String(localized: "generic_ok")) {
                dismiss()
            }
        } message: {
            Text(String(localized: "signin_success_message"))
        }
        .alert(Text("alert_terms_title"), isPresented: $showTermsAlert) {
            Button(role: .cancel) {}
                label: { Text("generic_ok") }
        } message: {
            Text("alert_terms_message")
        }
    }
    
    private func inputField(title: LocalizedStringKey, text: Binding<String>) -> some View {
        TextField(title, text: text)
            .textFieldStyle(PlainTextFieldStyle())
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .background(ThemeColors.surface(colorScheme))
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(ThemeColors.border(colorScheme), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 4, x: 0, y: 2)
    }
    
    private func secureField(title: LocalizedStringKey, text: Binding<String>) -> some View {
        SecureField(title, text: text)
            .textFieldStyle(PlainTextFieldStyle())
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .background(ThemeColors.surface(colorScheme))
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(ThemeColors.border(colorScheme), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 4, x: 0, y: 2)
    }
    
    private var termsText: some View {
        VStack(spacing: 4) {
            Text("signup_notice_prefix")
                .font(.system(size: 13))
                .foregroundColor(ThemeColors.secondaryText(colorScheme))
            signupLinks
                .font(.system(size: 13))
        }
        .multilineTextAlignment(.center)
    }
    
    private var signupLinks: Text {
        let accent = Color(red: 0.99, green: 0.70, blue: 0.19)
        return Text("signup_terms")
            .foregroundColor(accent)
            .underline()
        + Text(" ")
        + Text("signup_and")
            .foregroundColor(Color(.secondaryLabel))
        + Text(" ")
        + Text("signup_privacy")
            .foregroundColor(accent)
            .underline()
    }
    
    var consentText: Text {
        let accent = Color(red: 0.99, green: 0.70, blue: 0.19)
        return Text("login_accept_prefix")
            .foregroundColor(ThemeColors.primaryText(colorScheme))
        + Text(" ")
        + Text("login_privacy")
            .foregroundColor(accent)
            .underline()
        + Text(" ")
        + Text("login_terms_connector")
            .foregroundColor(ThemeColors.primaryText(colorScheme))
        + Text(" ")
        + Text("login_terms_conditions")
            .foregroundColor(accent)
            .underline()
    }
}

private extension SignInView {
    func submit() async {
        errorMessage = nil
        guard !email.isEmpty, !firstName.isEmpty, !lastName.isEmpty, !password.isEmpty, !confirmPassword.isEmpty else {
            errorMessage = String(localized: "signin_error_empty_fields")
            return
        }
        
        guard isValidEmail(email) else {
            errorMessage = String(localized: "validation_email_invalid")
            return
        }

        guard isValidPassword(password) else {
            errorMessage = String(localized: "validation_password_invalid")
            return
        }

        guard password == confirmPassword else {
            errorMessage = String(localized: "signin_error_password_mismatch")
            return
        }
        
        guard acceptTerms else {
            showTermsAlert = true
            return
        }
        
        isLoading = true
        defer { isLoading = false }
        
        do {
            let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
            let trimmedFirstName = firstName.trimmingCharacters(in: .whitespacesAndNewlines)
            let trimmedLastName = lastName.trimmingCharacters(in: .whitespacesAndNewlines)
            
            // Generate a unique username by adding a random suffix if needed
            var baseUsername = trimmedEmail.split(separator: "@").first.map(String.init)?.lowercased() ?? trimmedFirstName.lowercased()
            var username = baseUsername
            var attempts = 0
            let maxAttempts = 5
            
            // Try to register with the base username, if it fails due to username conflict, add a random suffix
            while attempts < maxAttempts {
                do {
                    _ = try await AuthService.shared.register(
                        username: username,
                        email: trimmedEmail,
                        firstName: trimmedFirstName,
                        lastName: trimmedLastName,
                        password: password
                    )
                    showSuccess = true
                    return // Success, exit the function
                } catch {
                    let errorMessage = error.localizedDescription.lowercased()
                    // Check if it's a username conflict error
                    if errorMessage.contains("nom d'utilisateur") || 
                       errorMessage.contains("username already exists") ||
                       errorMessage.contains("username") && errorMessage.contains("déjà") {
                        // Generate a new username with a random suffix
                        let randomSuffix = Int.random(in: 1000...9999)
                        username = "\(baseUsername)\(randomSuffix)"
                        attempts += 1
                        continue // Try again with the new username
                    } else {
                        // It's a different error (email conflict, etc.), show it
                        self.errorMessage = error.localizedDescription
                        return
                    }
                }
            }
            
            // If we exhausted all attempts
            errorMessage = "Impossible de générer un nom d'utilisateur unique. Veuillez réessayer."
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

private extension SignInView {
    func isValidEmail(_ email: String) -> Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return NSPredicate(format: "SELF MATCHES %@", emailRegex).evaluate(with: email)
    }

    func isValidPassword(_ password: String) -> Bool {
        let regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*(),.?\\\"{}|<>]).{8,}$"
        return NSPredicate(format: "SELF MATCHES %@", regex).evaluate(with: password)
    }
}

struct SignInView_Previews: PreviewProvider {
    static var previews: some View {
        SignInView()
    }
}
