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
    @State private var email = ""
    @State private var password = ""
    @State private var isEmailFocused = false
    @State private var isPasswordFocused = false
    @State private var emailError: String?
    @State private var passwordError: String?
    @State private var loginError: String?
    @State private var isLoggedIn = false // Etat pour déterminer si l'utilisateur est connecté
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
                            NavigationLink(value: "signUp") {
                                EmptyView()
                            }
                            .hidden()
                            .navigationDestination(item: Binding(
                                get: { showSignUp ? "signUp" : nil },
                                set: { showSignUp = $0 != nil }
                            )) { _ in
                                SignInView()
                                    .navigationBarBackButtonHidden(true)
                            }
                            
                            NavigationLink(value: "otpLogin") {
                                EmptyView()
                            }
                            .hidden()
                            .navigationDestination(item: Binding(
                                get: { showOTPLogin ? "otpLogin" : nil },
                                set: { showOTPLogin = $0 != nil }
                            )) { _ in
                                EmailOTPEntryView()
                                    .navigationBarBackButtonHidden(true)
                            }
                            
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
                                        .background(Color.white)
                                        .cornerRadius(14)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                                .stroke(borderColor(for: emailError, isFocused: isEmailFocused), lineWidth: 1.5)
                                        )
                                        .autocapitalization(.none)
                                        .keyboardType(.emailAddress)
                                        .textContentType(.emailAddress)
                                        .autocorrectionDisabled(true)
                                        .onTapGesture {
                                            isEmailFocused = true
                                            isPasswordFocused = false
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
                                    
                                    SecureField(LocalizedStringKey("login_password_placeholder"), text: $password)
                                        .padding()
                                        .background(Color.white)
                                        .cornerRadius(14)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                                .stroke(borderColor(for: passwordError, isFocused: isPasswordFocused), lineWidth: 1.5)
                                        )
                                        .onTapGesture {
                                            isPasswordFocused = true
                                            isEmailFocused = false
                                        }
                                        .onChange(of: password) { _, _ in
                                            passwordError = nil
                                        }
                                    
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
                                .disabled(activeLoginFlow != nil || !canUseGoogleSignIn())
                                .navigationDestination(item: Binding(
                                    get: { isLoggedIn ? "home" : nil },
                                    set: { isLoggedIn = $0 != nil }
                                )) { _ in
                                    HomeScreen(initialName: loggedInUserName)
                                        .navigationBarBackButtonHidden(true)
                                }
                                
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
                                    Text("Se connecter avec un code")
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundColor(ThemeColors.accent())
                                }
                                .padding(.top, 8)
                                
                                HStack {
                                    Rectangle()
                                        .fill(Color.gray.opacity(0.2))
                                        .frame(height: 1)
                                    
                                    Text("login_or")
                                        .font(.system(size: 14))
                                        .foregroundColor(.gray)
                                        .padding(.horizontal, 8)
                                    
                                    Rectangle()
                                        .fill(Color.gray.opacity(0.2))
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
                                            Text("Google")
                                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                                        }
                                        .padding(.vertical, 10)
                                        .padding(.horizontal, 18)
                                        .background(
                                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                                .fill(Color.white)
                                                .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 4)
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
                                            Text("Apple")
                                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                                        }
                                        .padding(.vertical, 10)
                                        .padding(.horizontal, 18)
                                        .background(
                                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                                .fill(Color.white)
                                                .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 4)
                                        )
                                    }
                                    .disabled(activeLoginFlow != nil)
                                }
                            }
                            .padding(24)
                            .frame(maxWidth: 420)
                            .background(
                                RoundedRectangle(cornerRadius: 32, style: .continuous)
                                    .fill(Color.white)
                            )
                            .shadow(color: Color.black.opacity(0.08), radius: 20, x: 0, y: 10)
                            .padding(.horizontal, 24)
                            .padding(.top, 24)
                            .padding(.bottom, geometry.safeAreaInsets.bottom > 0 ? 30 : 50)
                        } // Fin VStack
                    } // Fin ScrollView
                } // Fin GeometryReader
            } // Fin ZStack
            .navigationBarHidden(true)
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
    
    @MainActor
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
        
        activeLoginFlow = .password
        defer { activeLoginFlow = nil }
        
        do {
            let user = try await AuthService.shared.login(email: email, password: password)
            loggedInUserName = user.firstName ?? user.username ?? (user.email?.split(separator: "@").first.map(String.init))
            completeLogin()
        } catch {
            loginError = error.localizedDescription
            print("Login error: \(error)")
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
            
            let user = try await AuthService.shared.loginWithGoogle(idToken: idToken)
            loggedInUserName = user.firstName ?? user.username ?? (user.email?.split(separator: "@").first.map(String.init))
            completeLogin()
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
            
            let user = try await AuthService.shared.loginWithApple(
                identityToken: identityToken,
                email: email,
                firstName: firstName,
                lastName: lastName
            )
            loggedInUserName = user.firstName ?? user.username ?? (user.email?.split(separator: "@").first.map(String.init))
            completeLogin()
        } catch {
            loginError = error.localizedDescription
        }
    }
    
    @MainActor
    func completeLogin() {
        loginError = nil
        isLoggedIn = true
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
        let pattern = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return NSPredicate(format: "SELF MATCHES %@", pattern).evaluate(with: email)
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
