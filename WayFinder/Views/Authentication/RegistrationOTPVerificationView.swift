//
//  RegistrationOTPVerificationView.swift
//  WayFinder
//
//  OTP verification screen for registration
//

import SwiftUI

struct RegistrationOTPVerificationView: View {
    let email: String
    let firstName: String
    let lastName: String
    let password: String
    let onVerificationComplete: (String) -> Void
    let onDismiss: () -> Void
    
    @Environment(\.colorScheme) private var colorScheme
    @State private var codeDigits: [String] = Array(repeating: "", count: 4)
    @State private var isLoading = false
    @State private var isVerifying = false
    @State private var errorMessage: String?
    @State private var timer: Timer?
    @State private var remainingSeconds = 300 // 5 minutes
    @State private var canResend = false
    @FocusState private var focusedField: Int?
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                VStack(spacing: 32) {
                    Spacer(minLength: 40)
                    
                    // Logo
                    Image("LogoWayFinder")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 120, height: 120)
                    
                    // Title
                    VStack(spacing: 8) {
                        Text(String(localized: "otp_title"))
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                        
                        Text("Vérifiez votre email pour continuer")
                            .font(.system(size: 14))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .multilineTextAlignment(.center)
                    }
                    .padding(.horizontal, 40)
                    
                    // OTP Input Fields
                    HStack(spacing: 12) {
                        ForEach(0..<4, id: \.self) { index in
                            TextField("", text: Binding(
                                get: { codeDigits[index] },
                                set: { newValue in
                                    // Only allow single digit
                                    let filtered = newValue.filter { $0.isNumber }.prefix(1)
                                    codeDigits[index] = String(filtered)
                                    
                                    // Auto-advance to next field
                                    if !filtered.isEmpty && index < 3 {
                                        focusedField = index + 1
                                    }
                                    
                                    // Auto-verify when all fields are filled
                                    if index == 3 && !filtered.isEmpty {
                                        verifyOTP()
                                    }
                                }
                            ))
                            .keyboardType(.numberPad)
                            .textContentType(.oneTimeCode)
                            .multilineTextAlignment(.center)
                            .font(.system(size: 24, weight: .bold))
                            .frame(width: 60, height: 60)
                            .background(ThemeColors.surface(colorScheme))
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(focusedField == index ? ThemeColors.accent() : ThemeColors.border(colorScheme), lineWidth: 2)
                            )
                            .focused($focusedField, equals: index)
                        }
                    }
                    .padding(.horizontal, 40)
                    
                    // Error message
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.system(size: 14))
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 40)
                    }
                    
                    // Email info
                    VStack(spacing: 4) {
                        Text("Code envoyé à")
                            .font(.system(size: 14))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        Text(email)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                    .padding(.vertical, 8)
                    
                    // Verify button
                    Button(action: {
                        verifyOTP()
                    }) {
                        if isVerifying {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text(String(localized: "otp_button"))
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(isAllDigitsFilled ? ThemeColors.accent() : Color.gray)
                    .cornerRadius(12)
                    .padding(.horizontal, 40)
                    .disabled(!isAllDigitsFilled || isVerifying || isLoading)
                    
                    // Resend OTP
                    VStack(spacing: 8) {
                        if canResend {
                            Button(action: {
                                resendOTP()
                            }) {
                                Text("Renvoyer le code")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(ThemeColors.accent())
                            }
                            .disabled(isLoading)
                        } else {
                            Text("Renvoyer le code dans \(formatTime(remainingSeconds))")
                                .font(.system(size: 14))
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        }
                    }
                    .padding(.top, 8)
                    
                    Spacer()
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        onDismiss()
                    }) {
                        Image(systemName: "xmark")
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                }
            }
            .onAppear {
                startTimer()
                focusedField = 0
            }
            .onDisappear {
                timer?.invalidate()
            }
        }
    }
    
    private var isAllDigitsFilled: Bool {
        codeDigits.allSatisfy { !$0.isEmpty }
    }
    
    private func startTimer() {
        remainingSeconds = 300 // 5 minutes
        canResend = false
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            if remainingSeconds > 0 {
                remainingSeconds -= 1
            } else {
                canResend = true
                timer?.invalidate()
            }
        }
    }
    
    private func formatTime(_ seconds: Int) -> String {
        let minutes = seconds / 60
        let secs = seconds % 60
        if minutes > 0 {
            return String(format: "%dm %02ds", minutes, secs)
        } else {
            return "\(secs)s"
        }
    }
    
    private func resendOTP() {
        isLoading = true
        errorMessage = nil
        Task {
            do {
                let response = try await AuthService.shared.sendOTPForRegistration(email: email)
                print("✅ [RegistrationOTP] OTP resent: \(response.message)")
                startTimer()
            } catch {
                errorMessage = "Erreur lors de l'envoi du code. Veuillez réessayer."
                print("❌ [RegistrationOTP] Failed to resend OTP: \(error.localizedDescription)")
            }
            isLoading = false
        }
    }
    
    private func verifyOTP() {
        guard isAllDigitsFilled else { return }
        
        isVerifying = true
        errorMessage = nil
        
        // Joindre les chiffres et nettoyer le code
        let otpCode = codeDigits.joined().trimmingCharacters(in: .whitespacesAndNewlines)
        
        print("🔄 [RegistrationOTP] Verifying OTP code: \(otpCode) for email: \(email)")
        
        Task {
            do {
                print("🔄 [RegistrationOTP] Verifying OTP and registering user...")
                let response = try await AuthService.shared.registerWithOTP(
                    email: email,
                    firstName: firstName,
                    lastName: lastName,
                    password: password,
                    otpCode: otpCode
                )
                
                print("✅ [RegistrationOTP] Registration successful: \(response.message)")
                
                // Get user name from response or use firstName
                let userName = response.user?.displayNameValue ?? firstName
                
                // Auto-login is handled in registerWithOTP, so we can complete
                await MainActor.run {
                    isVerifying = false
                    onVerificationComplete(userName)
                }
            } catch {
                await MainActor.run {
                    isVerifying = false
                    let errorMsg = error.localizedDescription.lowercased()
                    if errorMsg.contains("code") && (errorMsg.contains("incorrect") || errorMsg.contains("incorrect") || errorMsg.contains("invalide")) {
                        errorMessage = "Code incorrect. Veuillez réessayer."
                        // Clear OTP fields
                        codeDigits = Array(repeating: "", count: 4)
                        focusedField = 0
                    } else if errorMsg.contains("expiré") || errorMsg.contains("expired") {
                        errorMessage = "Le code a expiré. Veuillez demander un nouveau code."
                        codeDigits = Array(repeating: "", count: 4)
                        focusedField = 0
                    } else {
                        errorMessage = "Erreur lors de la vérification. Veuillez réessayer."
                    }
                    print("❌ [RegistrationOTP] Verification failed: \(error.localizedDescription)")
                }
            }
        }
    }
}

