//
//  ForgotPasswordView.swift
//  WayFinder
//
//  Created by [Your Name] on 2025.
//

import SwiftUI
import Combine

struct ForgotPasswordView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @FocusState private var focusedField: Field?

    enum Field {
        case email, otpCode, newPassword, confirmPassword
    }

    @State private var currentStep: ResetStep = .enterEmail
    @State private var email = ""
    @State private var otpCode = ""
    @State private var newPassword = ""
    @State private var confirmPassword = ""

    @State private var emailError: String?
    @State private var otpError: String?
    @State private var passwordError: String?
    @State private var confirmPasswordError: String?

    @State private var isLoading = false
    @State private var generalError: String?

    @State private var showSuccessAlert = false
    @State private var successMessage = ""

    enum ResetStep {
        case enterEmail
        case enterOTPAndPassword
    }

    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()

            GeometryReader { geometry in
                ScrollView {
                    VStack(spacing: 0) {
                        Spacer().frame(height: geometry.safeAreaInsets.top > 0 ? 10 : 20)

                        // Back button
                        HStack {
                            Button(action: {
                                dismiss()
                            }) {
                                Image(systemName: "arrow.left")
                                    .font(.system(size: 20))
                                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                            }
                            Spacer()
                        }
                        .padding(.horizontal, 24)
                        .padding(.top, 16)

                        // Logo
                        Image("LogoWayFinder")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 150, height: 150)
                            .padding(.top, 16)

                        // Title
                        Text("forgot_password_title")
                            .font(.system(size: 26, weight: .bold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                            .padding(.top, 12)

                        // Subtitle
                        Text("forgot_password_subtitle")
                            .font(.system(size: 16))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                            .padding(.top, 8)

                        // Card container
                        VStack(spacing: 18) {
                            VStack(spacing: 15) {
                                if currentStep == .enterEmail {
                                    // Email field
                                    TextField(LocalizedStringKey("forgot_password_email_placeholder"), text: $email)
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
                                            focusedField = nil
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
                                } else {
                                    // OTP Code field
                                    TextField(LocalizedStringKey("forgot_password_otp_placeholder"), text: $otpCode)
                                        .padding()
                                        .background(ThemeColors.surface(colorScheme))
                                        .cornerRadius(14)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                                .stroke(borderColor(for: otpError, isFocused: focusedField == .otpCode), lineWidth: 1.5)
                                        )
                                        .keyboardType(.numberPad)
                                        .focused($focusedField, equals: .otpCode)
                                        .onChange(of: otpCode) { _, _ in
                                            otpError = nil
                                        }

                                    if let otpError {
                                        Text(otpError)
                                            .font(.caption)
                                            .foregroundColor(.red)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                    }

                                    // New Password field
                                    SecureField(LocalizedStringKey("forgot_password_new_password_placeholder"), text: $newPassword)
                                        .padding()
                                        .background(ThemeColors.surface(colorScheme))
                                        .cornerRadius(14)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                                .stroke(borderColor(for: passwordError, isFocused: focusedField == .newPassword), lineWidth: 1.5)
                                        )
                                        .textContentType(.newPassword)
                                        .focused($focusedField, equals: .newPassword)
                                        .onChange(of: newPassword) { _, _ in
                                            passwordError = nil
                                        }

                                    if let passwordError {
                                        Text(passwordError)
                                            .font(.caption)
                                            .foregroundColor(.red)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                    }

                                    // Confirm Password field
                                    SecureField(LocalizedStringKey("forgot_password_confirm_password_placeholder"), text: $confirmPassword)
                                        .padding()
                                        .background(ThemeColors.surface(colorScheme))
                                        .cornerRadius(14)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                                .stroke(borderColor(for: confirmPasswordError, isFocused: focusedField == .confirmPassword), lineWidth: 1.5)
                                        )
                                        .textContentType(.newPassword)
                                        .focused($focusedField, equals: .confirmPassword)
                                        .onChange(of: confirmPassword) { _, _ in
                                            confirmPasswordError = nil
                                        }

                                    if let confirmPasswordError {
                                        Text(confirmPasswordError)
                                            .font(.caption)
                                            .foregroundColor(.red)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                    }
                                }
                            }

                            // Action button
                            Button {
                                if currentStep == .enterEmail {
                                    requestPasswordReset()
                                } else {
                                    resetPassword()
                                }
                            } label: {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 14)
                                } else {
                                    Text(currentStep == .enterEmail ?
                                         LocalizedStringKey("forgot_password_send_code") :
                                         LocalizedStringKey("forgot_password_reset_button"))
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 14)
                                }
                            }
                            .background(ThemeColors.accent())
                            .cornerRadius(14)
                            .disabled(isLoading)

                            if let generalError {
                                Text(generalError)
                                    .font(.caption)
                                    .foregroundColor(.red)
                                    .frame(maxWidth: .infinity, alignment: .center)
                            }

                            // Back to login button
                            Button(action: {
                                if currentStep == .enterOTPAndPassword {
                                    currentStep = .enterEmail
                                    clearForm()
                                } else {
                                    dismiss()
                                }
                            }) {
                                Text("forgot_password_back_to_login")
                                    .font(.system(size: 14))
                                    .foregroundColor(ThemeColors.accent())
                            }
                            .padding(.top, 8)
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
                    }
                }
            }
        }
        .navigationBarHidden(true)
        .alert(isPresented: $showSuccessAlert) {
            Alert(
                title: Text("Succès"),
                message: Text(successMessage),
                dismissButton: .default(Text("OK")) {
                    dismiss()
                }
            )
        }
    }

    private func borderColor(for error: String?, isFocused: Bool) -> Color {
        if error != nil {
            return Color.red
        }
        return isFocused ? ThemeColors.accent() : ThemeColors.border(colorScheme)
    }

    private func requestPasswordReset() {
        emailError = nil
        generalError = nil

        guard !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            emailError = String(localized: "validation_email_invalid")
            return
        }

        guard isValidEmail(email) else {
            emailError = String(localized: "validation_email_invalid")
            return
        }

        isLoading = true

        Task {
            do {
                let response = try await AuthService.shared.requestPasswordResetOtp(email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())
                await MainActor.run {
                    isLoading = false
                    successMessage = response.message
                    currentStep = .enterOTPAndPassword
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    generalError = error.localizedDescription
                }
            }
        }
    }

    private func resetPassword() {
        otpError = nil
        passwordError = nil
        confirmPasswordError = nil
        generalError = nil

        guard !otpCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            otpError = "Veuillez saisir le code OTP"
            return
        }

        guard otpCode.count == 4, otpCode.allSatisfy({ $0.isNumber }) else {
            otpError = "Le code OTP doit contenir 4 chiffres"
            return
        }

        guard !newPassword.isEmpty else {
            passwordError = "Veuillez saisir un nouveau mot de passe"
            return
        }

        guard newPassword.count >= 6 else {
            passwordError = "Le mot de passe doit contenir au moins 6 caractères"
            return
        }

        guard !confirmPassword.isEmpty else {
            confirmPasswordError = "Veuillez confirmer le mot de passe"
            return
        }

        guard newPassword == confirmPassword else {
            confirmPasswordError = "Les mots de passe ne correspondent pas"
            return
        }

        isLoading = true

        Task {
            do {
                let response = try await AuthService.shared.resetPassword(
                    email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
                    otpCode: otpCode,
                    newPassword: newPassword
                )
                await MainActor.run {
                    isLoading = false
                    successMessage = response.message
                    showSuccessAlert = true
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    generalError = error.localizedDescription
                }
            }
        }
    }

    private func clearForm() {
        otpCode = ""
        newPassword = ""
        confirmPassword = ""
        otpError = nil
        passwordError = nil
        confirmPasswordError = nil
        generalError = nil
    }

    private func isValidEmail(_ email: String) -> Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return emailPredicate.evaluate(with: email)
    }
}

struct ForgotPasswordView_Previews: PreviewProvider {
    static var previews: some View {
        ForgotPasswordView()
    }
}
