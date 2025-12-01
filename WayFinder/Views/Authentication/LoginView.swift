//
//  LoginView.swift
//  WayFinder
//
//  Created by sarrachmek on 4/11/2025.
//

import Foundation
import SwiftUI
import AuthenticationServices
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif
import UIKit

struct LoginView: View {
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var languageManager: LanguageManager
    @FocusState private var focusedField: Field?
    @State private var email = ""
    @State private var password = ""
    @State private var showPassword = false
    
    enum Field {
        case email, password
    }
    @State private var emailError: String?
    @State private var passwordError: String?
    @State private var loginError: String?
    @State private var isLoggedIn = false // Etat pour déterminer si l'utilisateur est connecté
    @State private var navigateToDestination: String? = nil // Destination de navigation après login
    @State private var activeLoginFlow: LoginFlow?
    @StateObject private var appleSignInCoordinator = AppleSignInCoordinator()
    @State private var loggedInUserName: String?
    @State private var showSignUp = false
    @State private var showOTPLogin = false
    
    private let googleLoginEnabled = true
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                GeometryReader { geometry in
                    ScrollView {
                        VStack(spacing: 0) {
                            
                            // Titre "Bienvenue sur Wayfindr" - en haut
                            Spacer().frame(height: geometry.safeAreaInsets.top > 0 ? 10 : 20)
                        
                            // Logo
                            Image("LogoWayFinder")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 150, height: 150)
                                .padding(.top, 16)
                            
                            // Message
                            Text("login_message")
                                .font(.system(size: 16))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                                .padding(.top, 12)
                            
                            // Card container
                            VStack(spacing: 18) {
                                VStack(spacing: 15) {
                                    TextField(LocalizedStringKey("login_email_placeholder"), text: $email)
                                        .padding()
                                        .background(ThemeColors.surface(colorScheme))
                                        .cornerRadius(14)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                                .stroke(borderColor(for: emailError, isFocused: focusedField == .email), lineWidth: 1.5)
                                        )
                                        .autocapitalization(.none)
                                        .keyboardType(.emailAddress)
                                        .textContentType(.emailAddress)
                                        .autocorrectionDisabled(true)
                                        .focused($focusedField, equals: .email)
                                        .onSubmit {
                                            focusedField = .password
                                        }
                                        .onChange(of: email) { _, _ in
                                            emailError = nil
                                        }
                                    
                                    if let emailError {
                                        Text(emailError)
                                            .font(.caption)
                                            .foregroundColor(.red)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                    }
                                    
                                    HStack {
                                        Group {
                                            if showPassword {
                                                TextField(LocalizedStringKey("login_password_placeholder"), text: $password)
                                            } else {
                                                SecureField(LocalizedStringKey("login_password_placeholder"), text: $password)
                                            }
                                        }
                                        .focused($focusedField, equals: .password)
                                        .onSubmit {
                                            focusedField = nil
                                            Task { await validateAndSubmit() }
                                        }
                                        .onChange(of: password) { _, _ in
                                            passwordError = nil
                                        }
                                        
                                        Button(action: {
                                            showPassword.toggle()
                                        }) {
                                            Image(systemName: showPassword ? "eye.slash.fill" : "eye.fill")
                                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                                .font(.system(size: 16))
                                        }
                                    }
                                    .padding()
                                    .background(ThemeColors.surface(colorScheme))
                                    .cornerRadius(14)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                                            .stroke(borderColor(for: passwordError, isFocused: focusedField == .password), lineWidth: 1.5)
                                    )
                                    
                                    if let passwordError {
                                        Text(passwordError)
                                            .font(.caption)
                                            .foregroundColor(.red)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                    }
                                }
                                
                                Button {
                                    Task { await validateAndSubmit() }
                                } label: {
                                    Text("login_sign_in_button")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 14)
                                        .background(ThemeColors.accent())
                                        .cornerRadius(14)
                                }
                                .disabled(activeLoginFlow != nil)
                                
                                if let loginError {
                                    Text(loginError)
                                        .font(.caption)
                                        .foregroundColor(.red)
                                        .frame(maxWidth: .infinity, alignment: .center)
                                }
                                
                                HStack {
                                    Button(action: {
                                        showSignUp = true
                                    }) {
                                        Text("login_no_account")
                                            .font(.system(size: 14))
                                            .foregroundColor(ThemeColors.accent())
                                    }
                                    
                                    Spacer()
                                    
                                    Button(action: {
                                        // TODO: reset password flow
                                    }) {
                                        Text("login_forgot_password")
                                            .font(.system(size: 14))
                                            .foregroundColor(ThemeColors.accent())
                                    }
                                }
                                
                                Button(action: {
                                    showOTPLogin = true
                                }) {
                                    Text("login_with_code")
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundColor(ThemeColors.accent())
                                }
                                .padding(.top, 8)
                                
                                HStack {
                                    Rectangle()
                                        .fill(ThemeColors.secondaryText(colorScheme).opacity(0.3))
                                        .frame(height: 1)
                                    
                                    Text("login_or")
                                        .font(.system(size: 14))
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                        .padding(.horizontal, 8)
                                    
                                    Rectangle()
                                        .fill(ThemeColors.secondaryText(colorScheme).opacity(0.3))
                                        .frame(height: 1)
                                }
                                
                                HStack(spacing: 16) {
                                    // Google button
                                    Button(action: {
                                        Task { await handleGoogleSignIn() }
                                    }) {
                                        HStack(spacing: 8) {
                                            Image("ic_google")
                                                .resizable()
                                                .scaledToFit()
                                                .frame(width: 20, height: 20)
                                            Text("social_google")
                                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                                        }
                                        .padding(.vertical, 10)
                                        .padding(.horizontal, 18)
                                        .background(
                                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                                .fill(ThemeColors.surface(colorScheme))
                                                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.05), radius: 8, x: 0, y: 4)
                                        )
                                    }
                                    .disabled(activeLoginFlow != nil || !googleLoginEnabled)
                                    
                                    // Apple button
                                    Button(action: {
                                        Task { await handleAppleSignIn() }
                                    }) {
                                        HStack(spacing: 8) {
                                            Image("ic_apple")
                                                .resizable()
                                                .scaledToFit()
                                                .frame(width: 18, height: 18)
                                            Text("social_apple")
                                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                                        }
                                        .padding(.vertical, 10)
                                        .padding(.horizontal, 18)
                                        .background(
                                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                                .fill(ThemeColors.surface(colorScheme))
                                                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.05), radius: 8, x: 0, y: 4)
                                        )
                                    }
                                    .disabled(activeLoginFlow != nil)
                                }
                            }
                            .padding(24)
                            .frame(maxWidth: 420)
                            .background(
                                RoundedRectangle(cornerRadius: 32, style: .continuous)
                                    .fill(ThemeColors.surface(colorScheme))
                            )
                            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.4 : 0.08), radius: 20, x: 0, y: 10)
                            .padding(.horizontal, 24)
                            .padding(.top, 24)
                            .padding(.bottom, geometry.safeAreaInsets.bottom > 0 ? 30 : 50)
                        } // Fin VStack
                    } // Fin ScrollView
                } // Fin GeometryReader
            } // Fin ZStack
            .navigationBarHidden(true)
            .navigationDestination(item: Binding(
                get: { showSignUp ? "signUp" : nil },
                set: { showSignUp = $0 != nil }
            )) { _ in
                SignInView(showSignUp: $showSignUp)
                    .environmentObject(languageManager)
                    .navigationBarBackButtonHidden(true)
            }
            .navigationDestination(item: Binding(
                get: { showOTPLogin ? "otpLogin" : nil },
                set: { showOTPLogin = $0 != nil }
            )) { _ in
                EmailOTPEntryView()
                    .environmentObject(languageManager)
                    .navigationBarBackButtonHidden(true)
            }
            .navigationDestination(item: Binding(
                get: { navigateToDestination },
                set: { navigateToDestination = $0 }
            )) { destination in
                if destination == "home" {
                    HomeScreen(initialName: loggedInUserName)
                        .environmentObject(languageManager)
                        .navigationBarBackButtonHidden(true)
                } else if destination == "onboarding" {
                    // TODO: Navigate to onboarding screen when implemented
                    HomeScreen(initialName: loggedInUserName)
                        .environmentObject(languageManager)
                        .navigationBarBackButtonHidden(true)
                } else {
                    EmptyView()
                }
            }
            .onAppear {
                // Pré-remplir l'email depuis UserStorage si disponible et si l'email est vide
                if email.isEmpty, let storedEmail = UserDefaults.standard.string(forKey: UserStorage.userEmailKey) {
                    email = storedEmail
                }
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
    
    func dismissKeyboard() {
        focusedField = nil
    }
    
    @MainActor
    func validateAndSubmit() async {
        emailError = nil
        passwordError = nil
        loginError = nil
        
        // Only validate that fields are not empty (like Android)
        guard !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            emailError = String(localized: "validation_email_invalid")
            return
        }
        
        guard !password.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            passwordError = String(localized: "signin_error_empty_fields")
            return
        }
        
        // No password strength validation for login (only for registration)
        
        activeLoginFlow = .password
        defer { activeLoginFlow = nil }
        
        do {
            print("🔐 [LoginView] Attempting login with email: \(email)")
            let loginResult = try await AuthService.shared.loginWithResponse(email: email, password: password)
            
            // Get user from response (backend always returns user)
            guard let user = loginResult.user else {
                print("❌ [LoginView] No user in login response")
                loginError = "Erreur: Aucun utilisateur dans la réponse"
                return
            }
            
            print("✅ [LoginView] Login successful for user: \(user.username ?? "unknown")")
            // Utiliser displayNameValue qui gère correctement firstName, username, email
            loggedInUserName = user.displayNameValue
            print("✅ [LoginView] Setting loggedInUserName: \(loggedInUserName)")
            print("✅ [LoginView] User profile - firstName: \(user.firstName ?? "nil"), username: \(user.username ?? "nil"), email: \(user.email ?? "nil")")
            print("✅ [LoginView] Onboarding status - completed: \(loginResult.onboardingCompleted ?? false), skipped: \(user.onboardingSkipped ?? false)")
            
            // Sauvegarder le profil utilisateur pour que HomeScreen puisse vérifier l'état d'onboarding
            UserStorage.saveProfile(user)
            
            // Navigate to home (popup will show if onboarding not completed, like Android)
            let destination = "home"
            print("✅ [LoginView] Navigating to: \(destination) (onboardingCompleted: \(loginResult.onboardingCompleted ?? false))")
            
            // Trigger navigation on main thread
            await MainActor.run {
                navigateToDestination = destination
                isLoggedIn = true
                print("✅ [LoginView] Navigation triggered, navigateToDestination: \(navigateToDestination ?? "nil"), isLoggedIn: \(isLoggedIn)")
            }
        } catch {
            loginError = error.localizedDescription
            print("❌ [LoginView] Login error: \(error)")
            print("❌ [LoginView] Error description: \(error.localizedDescription)")
        }
    }
    
    /// Gère la connexion avec Google Sign In
    /// - Note: Utilise GOOGLE_CLIENT_ID depuis Info.plist pour l'authentification côté client.
    ///   Le token est ensuite envoyé au backend Render qui doit avoir GOOGLE_CLIENT_ID_WEB configuré.
    @MainActor
    func handleGoogleSignIn() async {
#if canImport(GoogleSignIn)
        guard googleLoginEnabled else {
            loginError = SocialLoginError.googleDisabled.localizedDescription
            return
        }
        guard activeLoginFlow == nil else { return }
        loginError = nil
        
        guard let presentingViewController = findPresentingViewController() else {
            loginError = SocialLoginError.missingPresenter.localizedDescription
            return
        }
        
        activeLoginFlow = .google
        defer { activeLoginFlow = nil }
        
        do {
            if GIDSignIn.sharedInstance.configuration == nil {
                // Récupérer GOOGLE_CLIENT_ID depuis Info.plist (configuré pour iOS)
                guard let clientID = Bundle.main.object(forInfoDictionaryKey: "GOOGLE_CLIENT_ID") as? String else {
                    throw SocialLoginError.missingGoogleClientID
                }
                GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            }

            let signInResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController)
            guard let idToken = signInResult.user.idToken?.tokenString else {
                throw SocialLoginError.missingGoogleToken
            }
            
            print("🔐 [LoginView] Google Sign-In successful, sending token to backend...")
            let loginResult = try await AuthService.shared.loginWithGoogle(idToken: idToken)
            
            // Get user from response
            guard let user = loginResult.user else {
                print("❌ [LoginView] No user in Google login response")
                loginError = "Erreur: Aucun utilisateur dans la réponse"
                return
            }
            
            print("✅ [LoginView] Google login successful for user: \(user.username ?? "unknown")")
            loggedInUserName = user.displayNameValue
            print("✅ [LoginView] Setting loggedInUserName: \(loggedInUserName)")
            print("✅ [LoginView] Onboarding status - completed: \(loginResult.onboardingCompleted ?? false)")
            
            // Navigate to home (onboarding popup will show if needed)
            let destination = "home"
            print("✅ [LoginView] Navigating to: \(destination) (onboardingCompleted: \(loginResult.onboardingCompleted ?? false))")
            
            await MainActor.run {
                navigateToDestination = destination
                isLoggedIn = true
                print("✅ [LoginView] Google login navigation triggered")
            }
        } catch {
            loginError = error.localizedDescription
        }
#else
        loginError = SocialLoginError.googleDisabled.localizedDescription
#endif
    }
    
    @MainActor
    func handleAppleSignIn() async {
        guard activeLoginFlow == nil else { return }
        loginError = nil
        
        activeLoginFlow = .apple
        defer { activeLoginFlow = nil }
        
        do {
            let credential = try await appleSignInCoordinator.signIn()
            guard let tokenData = credential.identityToken,
                  let identityToken = String(data: tokenData, encoding: .utf8) else {
                throw SocialLoginError.missingAppleToken
            }
            
            let email = credential.email
            let firstName = credential.fullName?.givenName
            let lastName = credential.fullName?.familyName
            
            let loginResult = try await AuthService.shared.loginWithApple(
                identityToken: identityToken,
                email: email,
                firstName: firstName,
                lastName: lastName
            )
            
            // Get user from response
            guard let user = loginResult.user else {
                print("❌ [LoginView] No user in Apple login response")
                loginError = "Erreur: Aucun utilisateur dans la réponse"
                return
            }
            
            print("✅ [LoginView] Apple login successful for user: \(user.username ?? "unknown")")
            loggedInUserName = user.displayNameValue
            print("✅ [LoginView] Onboarding status - completed: \(loginResult.onboardingCompleted ?? false)")
            
            // Navigate to home (onboarding popup will show if needed)
            let destination = "home"
            await MainActor.run {
                navigateToDestination = destination
                isLoggedIn = true
                print("✅ [LoginView] Apple login navigation triggered")
            }
        } catch {
            loginError = error.localizedDescription
        }
    }
    
    func findPresentingViewController(base: UIViewController? = UIApplication.shared.connectedScenes
        .compactMap { ($0 as? UIWindowScene)?.windows.first(where: { $0.isKeyWindow }) }
        .first?.rootViewController) -> UIViewController? {
        if let nav = base as? UINavigationController {
            return findPresentingViewController(base: nav.visibleViewController)
        }
        if let tab = base as? UITabBarController, let selected = tab.selectedViewController {
            return findPresentingViewController(base: selected)
        }
        if let presented = base?.presentedViewController {
            return findPresentingViewController(base: presented)
        }
        return base
    }
    
    func canUseGoogleSignIn() -> Bool {
#if canImport(GoogleSignIn)
        return googleLoginEnabled
#else
        return false
#endif
    }
    
    func isValidEmail(_ email: String) -> Bool {
        return EmailValidator.isValid(email)
    }
    
    func isValidPassword(_ password: String) -> Bool {
        let pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[ !$&'()*+,-./:;<=>?@\\[\\]^_`{|}~\"])[A-Za-z\\d !$&'()*+,-./:;<=>?@\\[\\]^_`{|}~\"]{8,}$"
        return NSPredicate(format: "SELF MATCHES %@", pattern).evaluate(with: password)
    }
}

private enum LoginFlow {
    case password
    case google
    case apple
}

private enum SocialLoginError: LocalizedError {
    case missingPresenter
    case missingGoogleClientID
    case missingGoogleToken
    case missingAppleToken
    case missingGoogleSDK
    case googleDisabled

    var errorDescription: String? {
        switch self {
        case .missingPresenter:
            return "Impossible d'afficher l'écran de connexion."
        case .missingGoogleClientID:
            return "Client ID Google manquant. Vérifiez la configuration."
        case .missingGoogleToken:
            return "Token Google introuvable. Veuillez réessayer."
        case .missingAppleToken:
            return "Token Apple introuvable. Veuillez réessayer."
        case .missingGoogleSDK:
            return "Module GoogleSignIn absent. Ajoutez-le via Swift Package Manager."
        case .googleDisabled:
            return "Google login n'est pas encore disponible sur le backend."
        }
    }
}

final class AppleSignInCoordinator: NSObject, ObservableObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private var continuation: CheckedContinuation<ASAuthorizationAppleIDCredential, Error>?

    @MainActor
    func signIn() async throws -> ASAuthorizationAppleIDCredential {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<ASAuthorizationAppleIDCredential, Error>) in
            self.continuation = continuation

            let provider = ASAuthorizationAppleIDProvider()
            let request = provider.createRequest()
            request.requestedScopes = [.fullName, .email]

            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            controller.performRequests()
        }
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            continuation?.resume(throwing: SocialLoginError.missingAppleToken)
            continuation = nil
            return
        }
        continuation?.resume(returning: credential)
        continuation = nil
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        continuation?.resume(throwing: error)
        continuation = nil
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}
