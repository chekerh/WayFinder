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
    @State private var showPassword: Bool = false
    @State private var showConfirmPassword: Bool = false
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showSuccess = false
    @State private var acceptTerms = false
    @State private var showTermsAlert = false
    @State private var navigateToHome = false
    @State private var loggedInUserName: String?
    
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
                    passwordField(title: LocalizedStringKey("signin_password_placeholder"), text: $password, showPassword: $showPassword)
                    passwordField(title: LocalizedStringKey("signin_confirm_password_placeholder"), text: $confirmPassword, showPassword: $showConfirmPassword)
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
        .fullScreenCover(isPresented: $navigateToHome) {
            HomeScreen(initialName: loggedInUserName)
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
    
    private func passwordField(title: LocalizedStringKey, text: Binding<String>, showPassword: Binding<Bool>) -> some View {
        HStack {
            Group {
                if showPassword.wrappedValue {
                    TextField(title, text: text)
                } else {
                    SecureField(title, text: text)
                }
            }
            .textFieldStyle(PlainTextFieldStyle())
            
            Button(action: {
                showPassword.wrappedValue.toggle()
            }) {
                Image(systemName: showPassword.wrappedValue ? "eye.slash.fill" : "eye.fill")
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    .font(.system(size: 16))
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .background(ThemeColors.surface(colorScheme))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(ThemeColors.border(colorScheme), lineWidth: 1)
        )
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
            
            // Generate a unique username using UUID for maximum uniqueness
            // Format: user_ + UUID (32 chars) = guaranteed unique
            let uuidString = UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()
            var username = "user_\(uuidString)"
            var attempts = 0
            let maxAttempts = 5 // Keep some attempts in case of extremely rare collision
            
            print("🔍 [SignIn] Generated username: \(username) (UUID-based for guaranteed uniqueness)")
            
            // Try to register with the base username, if it fails due to username conflict, add a random suffix
            while attempts < maxAttempts {
                do {
                    // Verify all fields are not empty before sending
                    guard !username.isEmpty, !trimmedEmail.isEmpty, !trimmedFirstName.isEmpty, !trimmedLastName.isEmpty, !password.isEmpty else {
                        errorMessage = "Tous les champs sont requis."
                        return
                    }
                    
                    print("🔄 [SignIn] Attempt \(attempts + 1)/\(maxAttempts) - Registering with:")
                    print("   Username: \(username)")
                    print("   Email: \(trimmedEmail)")
                    print("   First Name: \(trimmedFirstName)")
                    print("   Last Name: \(trimmedLastName)")
                    print("   Password: \(password.isEmpty ? "(empty)" : "***")")
                    
                    let registerResponse = try await AuthService.shared.register(
                        username: username,
                        email: trimmedEmail,
                        firstName: trimmedFirstName,
                        lastName: trimmedLastName,
                        password: password
                    )
                    print("✅ [SignIn] Registration response: \(registerResponse.message)")
                    print("✅ [SignIn] Registration successful! Auto-logging in...")
                    
                    // Auto-login after successful registration
                    do {
                        let loginResult = try await AuthService.shared.loginWithResponse(email: trimmedEmail, password: password)
                        if let user = loginResult.user {
                            loggedInUserName = user.displayNameValue
                            UserStorage.saveProfile(user)
                            print("✅ [SignIn] Auto-login successful, navigating to home")
                            navigateToHome = true
                        } else {
                            // If auto-login fails, just show success and dismiss
                            showSuccess = true
                        }
                    } catch {
                        print("⚠️ [SignIn] Auto-login failed, showing success alert: \(error.localizedDescription)")
                        showSuccess = true
                    }
                    return // Success, exit the function
                } catch {
                    let errorMessage = error.localizedDescription.lowercased()
                    print("❌ [SignIn] Registration attempt \(attempts + 1) failed: \(error.localizedDescription)")
                    
                    // First check if it's an email conflict - if so, stop immediately
                    // The backend checks email first (line 58 in auth.service.ts), so if email exists, we should not retry
                    // Check both English and French error messages
                    let isEmailConflict = errorMessage.contains("email already exists") || 
                                        errorMessage.contains("email already exist") ||
                                        errorMessage.contains("email") && errorMessage.contains("déjà") && !errorMessage.contains("username") ||
                                        errorMessage.contains("email") && errorMessage.contains("utilisé") && !errorMessage.contains("nom d'utilisateur")
                    
                    if isEmailConflict {
                        // Email conflict - don't try to generate new username, just show error
                        print("❌ [SignIn] Email conflict detected - stopping")
                        self.errorMessage = "Cet email est déjà utilisé. Veuillez utiliser un autre email."
                        return
                    }
                    
                    // Handle ambiguous "Email or username already exists" message
                    // The backend checks email first, then username. If we get this ambiguous message,
                    // it's likely from a MongoDB duplicate key error (line 109 in auth.service.ts)
                    // If we've tried multiple different usernames and still get this error, it's definitely the email
                    if errorMessage.contains("email or username already exists") ||
                       errorMessage.contains("email or username already exist") ||
                       errorMessage.contains("email ou nom d'utilisateur") {
                        // If we've already tried 1+ time with different usernames, it's definitely the email
                        // (since UUID usernames are guaranteed unique, if it fails it must be the email)
                        if attempts >= 1 {
                            print("❌ [SignIn] Ambiguous error after \(attempts + 1) attempt(s) with different UUID usernames - must be email conflict")
                            self.errorMessage = "Cet email est déjà utilisé. Veuillez utiliser un autre email."
                            return
                        }
                        // First attempt: try generating a new username (in case it was really a username conflict)
                        let uuidString = UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()
                        username = "user_\(uuidString)"
                        attempts += 1
                        print("🔄 [SignIn] Ambiguous error (email or username, attempt \(attempts)/\(maxAttempts)), trying new username: \(username)")
                        continue // Try again with the new username
                    }
                    
                    // Check if it's specifically a username conflict error (not email)
                    // Check both English and French error messages
                    let isUsernameConflict = errorMessage.contains("username already exists") || 
                                           errorMessage.contains("username already exist") ||
                                           errorMessage.contains("nom d'utilisateur") && errorMessage.contains("déjà") ||
                                           errorMessage.contains("nom d'utilisateur") && errorMessage.contains("utilisé") ||
                                           (errorMessage.contains("username") && errorMessage.contains("déjà") && !errorMessage.contains("email"))
                    
                    if isUsernameConflict {
                        // Generate a completely new username with fresh UUID
                        let uuidString = UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()
                        username = "user_\(uuidString)"
                        attempts += 1
                        print("🔄 [SignIn] Username conflict detected (attempt \(attempts)/\(maxAttempts)), trying new username: \(username)")
                        continue // Try again with the new username
                    } else {
                        // It's a different error, show it
                        print("❌ [SignIn] Unknown error: \(error.localizedDescription)")
                        self.errorMessage = error.localizedDescription
                        return
                    }
                }
            }
            
            // If we exhausted all attempts
            print("❌ [SignIn] Exhausted all attempts to generate unique username")
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
