//
//  LoginView.swift
//  WayFinder
//
//  Created by sarrachmek on 4/11/2025.
//

import Foundation
import SwiftUI

struct LoginView: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var email = ""
    @State private var password = ""
    @State private var isEmailFocused = false
    @State private var isPasswordFocused = false
    @State private var acceptTerms = true
    @State private var emailError: String?
    @State private var passwordError: String?
    @State private var showTermsAlert = false
    @State private var loginError: String?
    @State private var isLoading = false
    @State private var isLoggedIn = false // Etat pour déterminer si l'utilisateur est connecté
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                GeometryReader { geometry in
                    ScrollView {
                        VStack(spacing: 0) {
                            // Titre "Bienvenue sur Wayfindr" - en haut
                        HStack(spacing: 0) {
                            Text("login_title_prefix")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                            
                            Text("login_title_way")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(.red)
                            
                            Text("login_title_findr")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(.blue)
                        }
                            .frame(maxWidth: .infinity)
                            .padding(.top, geometry.safeAreaInsets.top > 0 ? 10 : 20)
                        
                            // Logo circulaire - juste en dessous du titre
                            Image("LogoWayFinder")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 200, height: 200)
                                .clipShape(Circle())
                                .padding(.top, 20)
            
            // Message
                        Text("login_message")
                                .font(.system(size: 16))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                                .padding(.top, 20)
                            
                            // Powered by Gemini
                        HStack(spacing: 4) {
                            Text("login_powered_prefix")
                                .font(.system(size: 14))
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            Text("login_powered_provider")
                                .font(.system(size: 14))
                                .foregroundColor(ThemeColors.accent())
                        }
                            .frame(maxWidth: .infinity)
                            .padding(.top, 5)
                
                            // Champs de saisie
                            VStack(spacing: 15) {
                                // Email field
                            TextField(LocalizedStringKey("login_email_placeholder"), text: $email)
                                .padding()
                                    .background(ThemeColors.surface(colorScheme))
                                    .cornerRadius(10)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(borderColor(for: emailError, isFocused: isEmailFocused), lineWidth: isEmailFocused ? 2 : 1)
                                    )
                .autocapitalization(.none)
                                    .keyboardType(.emailAddress)
                                    .textContentType(.emailAddress)
                                    .autocorrectionDisabled(true)
                                    .onTapGesture {
                                        isEmailFocused = true
                                        isPasswordFocused = false
                                    }
                                    .onChange(of: email) { _ in
                                        emailError = nil
                                    }

                                if let emailError {
                                    Text(emailError)
                                        .font(.caption)
                                        .foregroundColor(.red)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                }
                
                                // Password field
                            SecureField(LocalizedStringKey("login_password_placeholder"), text: $password)
                                .padding()
                                    .background(ThemeColors.surface(colorScheme))
                                    .cornerRadius(10)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(borderColor(for: passwordError, isFocused: isPasswordFocused), lineWidth: isPasswordFocused ? 2 : 1)
                                    )
                                    .onTapGesture {
                                        isPasswordFocused = true
                                        isEmailFocused = false
                                    }
                                    .onChange(of: password) { _ in
                                        passwordError = nil
                                    }

                                if let passwordError {
                                    Text(passwordError)
                                        .font(.caption)
                                        .foregroundColor(.red)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                }
                            }
                .padding(.horizontal, 40)
                            .padding(.top, 30)
                
                            // Bouton Se connecter
                            NavigationLink(
                                destination: SurveyScreen(),
                                isActive: $isLoggedIn
                            ) {
                                Button {
                                    Task { await validateAndSubmit() }
                                } label: {
                                    HStack {
                                        if isLoading {
                                            ProgressView()
                                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                        } else {
                                            Text("login_sign_in_button")
                                                .font(.system(size: 18, weight: .bold))
                                        }

                                        Spacer()

                                        Image(systemName: "globe")
                                            .foregroundColor(.white)
                                            .font(.system(size: 18))
                                    }
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding()
                                    .background(ThemeColors.accent())
                                    .cornerRadius(10)
                                }
                                .disabled(isLoading)
                            }
                            .padding(.horizontal, 40)
                            .padding(.top, 10)
                            
                            if let loginError {
                                Text(loginError)
                                    .font(.caption)
                                    .foregroundColor(.red)
                                    .frame(maxWidth: .infinity, alignment: .center)
                                    .padding(.top, 4)
                            }
                
                            // Checkbox avec conditions
                        HStack(alignment: .top, spacing: 10) {
                                Button(action: {
                                    acceptTerms.toggle()
                                }) {
                                    Image(systemName: acceptTerms ? "checkmark.square.fill" : "square")
                                        .foregroundColor(acceptTerms ? .purple : .gray)
                                        .font(.system(size: 20))
                                }
                                
                            consentText
                                .font(.system(size: 14))
                            }
                            .padding(.horizontal, 40)
                            .padding(.top, 10)
                
                            // Liens Pas de compte et Mot de passe oublié
                        NavigationLink(destination: SignInView()) {
                            HStack(spacing: 4) {
                                Text("login_no_account")
                                    .font(.system(size: 14))
                                    .foregroundColor(.gray)
                                Text("login_create_account")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(Color(red: 0.18, green: 0.55, blue: 0.99))
                            }
                        }
                            .padding(.horizontal, 40)
                            .padding(.top, 10)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        
                        Button(action: {
                            // TODO: reset password flow
                        }) {
                            Text("login_forgot_password")
                                .font(.system(size: 14))
                                .foregroundColor(Color(red: 0.18, green: 0.55, blue: 0.99))
                        }
                        .padding(.horizontal, 40)
                        .padding(.top, 4)
                        .frame(maxWidth: .infinity, alignment: .leading)
                
                            // Séparateur OU
            HStack {
                                Rectangle()
                                    .fill(Color.gray.opacity(0.3))
                                    .frame(height: 1)
                                
                            Text("login_or")
                                    .font(.system(size: 14))
                                    .foregroundColor(.gray)
                                    .padding(.horizontal, 10)
                                
                                Rectangle()
                                    .fill(Color.gray.opacity(0.3))
                                    .frame(height: 1)
                            }
                            .padding(.horizontal, 40)
                            .padding(.top, 20)
                        
                            // Social login buttons
                            HStack(spacing: 30) {
                                // Google button
                Button(action: {
                                    // Action Google login
                                }) {
                                    ZStack {
                                        Circle()
                                            .fill(Color.white)
                                            .frame(width: 60, height: 60)
                                            .shadow(color: .gray.opacity(0.3), radius: 5, x: 0, y: 2)
                                        
                                        Image("ic_google")
                                            .resizable()
                                            .scaledToFit()
                                            .frame(width: 40, height: 40)
                                    }
                                }
                                
                                // Apple button
                Button(action: {
                                    // Action Apple login
                                }) {
                                    ZStack {
                                        Circle()
                                            .fill(Color.white)
                                            .frame(width: 60, height: 60)
                                            .shadow(color: .gray.opacity(0.3), radius: 5, x: 0, y: 2)
                                        
                                        Image("ic_apple")
                                            .resizable()
                                            .scaledToFit()
                                            .frame(width: 30, height: 30)
                                    }
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.top, 20)
                            .padding(.bottom, geometry.safeAreaInsets.bottom > 0 ? 30 : 50)
                        } // Fin VStack
                    } // Fin ScrollView
                } // Fin GeometryReader
            } // Fin ZStack
            .navigationBarHidden(true)
            .alert(Text("alert_terms_title"), isPresented: $showTermsAlert) {
                Button(role: .cancel) {}
                    label: { Text("generic_ok") }
            } message: {
                Text("alert_terms_message")
            }
        }
    } // Fin body
} // Fin struct

private extension LoginView {
    func borderColor(for error: String?, isFocused: Bool) -> Color {
        if error != nil {
            return Color.red
        }
        return isFocused ? ThemeColors.accent() : ThemeColors.border(colorScheme)
    }
    
    var consentText: Text {
        let accent = Color(red: 0.99, green: 0.70, blue: 0.19)
        return Text("login_accept_prefix")
            .foregroundColor(Color(.label))
        + Text(" ")
        + Text("login_privacy")
            .foregroundColor(accent)
            .underline()
        + Text(" ")
        + Text("login_terms_connector")
            .foregroundColor(Color(.label))
        + Text(" ")
        + Text("login_terms_conditions")
            .foregroundColor(accent)
            .underline()
    }
    
    func validateAndSubmit() async {
        emailError = nil
        passwordError = nil
        loginError = nil
        
        if !isValidEmail(email) {
            emailError = String(localized: "validation_email_invalid")
        }
        
        if !isValidPassword(password) {
            passwordError = String(localized: "validation_password_invalid")
        }
        
        guard emailError == nil, passwordError == nil else { return }
        
        guard acceptTerms else {
            showTermsAlert = true
            return
        }
        
        isLoading = true
        defer { isLoading = false }
        
        do {
            let profile = try await AuthService.shared.login(email: email, password: password)
            loginError = nil
            isLoggedIn = true
        } catch {
            loginError = error.localizedDescription
            print("Login error: \(error)")
        }
    }
    
    func isValidEmail(_ email: String) -> Bool {
        let pattern = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return NSPredicate(format: "SELF MATCHES %@", pattern).evaluate(with: email)
    }
    
    func isValidPassword(_ password: String) -> Bool {
        let pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[ !$&'()*+,-./:;<=>?@\\[\\]^_`{|}~\"])[A-Za-z\\d !$&'()*+,-./:;<=>?@\\[\\]^_`{|}~\"]{8,}$"
        return NSPredicate(format: "SELF MATCHES %@", pattern).evaluate(with: password)
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}
