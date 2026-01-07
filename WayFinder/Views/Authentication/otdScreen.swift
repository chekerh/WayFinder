//
//  otdScreen.swift
//  WayFinder
//
//  Created by sarrachmek on 6/11/2025.
//

import SwiftUI

struct OTPScreenView: View {
    let email: String
    @Environment(\.dismiss) private var dismiss
    @State private var codeDigits: [String] = Array(repeating: "", count: 4)
    @State private var isLoading = false
    @State private var isVerifying = false
    @State private var errorMessage: String?
    @State private var successMessage: String?
    @State private var showHome = false
    @State private var loggedInUserName: String?
    @State private var timer: Timer?
    @State private var remainingSeconds = 300 // 5 minutes
    @State private var canResend = false
    @FocusState private var focusedField: Int?
    
    init(email: String) {
        self.email = email
    }
    
    var body: some View {
        NavigationStack {
            VStack {
                HeaderShape()
                    .fill(Color(red: 0.90, green: 0.95, blue: 1.0))
                    .frame(height: 320)
                    .overlay(headerContent, alignment: .topLeading)
                    .overlay(codeSection, alignment: .bottom)
                    .padding(.bottom, -80)
                
                Spacer()
                
                // Email info
                VStack(spacing: 8) {
                    Text("Code envoyé à")
                        .font(.system(size: 14))
                        .foregroundColor(.gray)
                    Text(email)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color(red: 0.18, green: 0.20, blue: 0.23))
                }
                .padding(.vertical, 16)
                
                // Error message
                if let errorMessage {
                    Text(errorMessage)
                        .font(.system(size: 14))
                        .foregroundColor(.red)
                        .padding(.horizontal, 24)
                        .multilineTextAlignment(.center)
                }
                
                // Success message
                if let successMessage {
                    Text(successMessage)
                        .font(.system(size: 14))
                        .foregroundColor(.green)
                        .padding(.horizontal, 24)
                        .multilineTextAlignment(.center)
                }
                
                // Resend code button
                if canResend {
                    Button(action: {
                        Task { await sendOTP() }
                    }) {
                        Text("Renvoyer le code")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(Color(red: 0.18, green: 0.55, blue: 0.99))
                    }
                    .padding(.vertical, 8)
                } else if remainingSeconds > 0 {
                    Text("Renvoyer le code dans \(formatTime(remainingSeconds))")
                        .font(.system(size: 14))
                        .foregroundColor(.gray)
                        .padding(.vertical, 8)
                }
                
                Spacer()
                
                VStack(spacing: 8) {
                    Button(action: {
                        Task { await verifyOTP() }
                    }) {
                        if isVerifying {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            HStack(spacing: 16) {
                                Text("Se connecter")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(.white)
                            }
                        }
                    }
                    .padding(.horizontal, 32)
                    .padding(.vertical, 16)
                    .background(isOTPComplete ? Color(red: 0.18, green: 0.55, blue: 0.99) : Color.gray)
                    .clipShape(Capsule())
                    .disabled(!isOTPComplete || isVerifying || isLoading)
                    .padding(.horizontal, 80)
                    
                    termsText
                        .padding(.horizontal, 24)
                }
                .padding(.bottom, 24)
            }
            .background(Color(red: 0.90, green: 0.95, blue: 1.0).ignoresSafeArea())
            .navigationDestination(item: Binding(
                get: { showHome ? "home" : nil },
                set: { showHome = $0 != nil }
            )) { _ in
                if let name = loggedInUserName {
                    HomeScreen(initialName: name)
                        .navigationBarBackButtonHidden(true)
                }
            }
            .onAppear {
                Task { await sendOTP() }
            }
            .onDisappear {
                timer?.invalidate()
            }
        }
    }
    
    private var isOTPComplete: Bool {
        codeDigits.allSatisfy { !$0.isEmpty && $0.count == 1 }
    }
    
    private var headerContent: some View {
        VStack(alignment: .leading, spacing: 22) {
            HStack {
                Button(action: {
                    dismiss()
                }) {
                    Circle()
                        .fill(Color.white.opacity(0.75))
                        .frame(width: 54, height: 54)
                        .overlay(
                            Image(systemName: "chevron.left")
                                .font(.system(size: 22, weight: .semibold))
                                .foregroundColor(Color(red: 0.18, green: 0.20, blue: 0.23))
                        )
                        .shadow(color: Color.black.opacity(0.08), radius: 6, x: 0, y: 4)
                }
                .padding(.leading, 28)
                .padding(.top,12)
                
                Spacer()
            }
            
            Text("Entrer le code")
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(Color(red: 0.16, green: 0.17, blue: 0.20))
                .padding(.leading, 40)
        }
    }
    
    private var codeSection: some View {
        VStack(spacing: 36) {
            HStack(spacing: 28) {
                ForEach(0..<4, id: \.self) { index in
                    OTPDigitField(
                        text: $codeDigits[index],
                        isFocused: focusedField == index
                    )
                    .focused($focusedField, equals: index)
                    .onChange(of: codeDigits[index]) { oldValue, newValue in
                        // Clear error message when user starts typing
                        if !newValue.isEmpty && oldValue != newValue {
                            errorMessage = nil
                        }
                        handleOTPChange(at: index, newValue: newValue)
                    }
                }
            }
            .padding(.horizontal, 40)
            .padding(.bottom, 60)
        }
    }
    
    private func handleOTPChange(at index: Int, newValue: String) {
        // Remove non-digit characters
        let filtered = newValue.filter { $0.isNumber }
        if filtered.count > 1 {
            codeDigits[index] = String(filtered.prefix(1))
        } else {
            codeDigits[index] = filtered
        }
        
        // Auto-advance to next field
        if !filtered.isEmpty && index < 3 {
            focusedField = index + 1
        }
        
        // Note: Auto-submit désactivé - l'utilisateur doit cliquer sur "Se connecter" manuellement
        // Cela évite les erreurs prématurées si le code n'est pas encore complètement saisi
    }
    
    @MainActor
    private func sendOTP() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            let response = try await AuthService.shared.sendOTP(email: email)
            successMessage = response.message
            startResendTimer()
        } catch {
            errorMessage = "Erreur lors de l'envoi du code: \(error.localizedDescription)"
        }
    }
    
    @MainActor
    private func verifyOTP() async {
        guard isOTPComplete else { return }
        
        isVerifying = true
        errorMessage = nil
        defer { isVerifying = false }
        
        // Joindre les chiffres et nettoyer le code
        let otpCode = codeDigits.joined().trimmingCharacters(in: .whitespacesAndNewlines)
        
        print("🔄 [OTPScreen] Verifying OTP code: \(otpCode) for email: \(email)")
        
        do {
            let user = try await AuthService.shared.verifyOTP(email: email, code: otpCode)
            print("✅ [OTPScreen] OTP verification successful")
            loggedInUserName = user.firstName ?? user.username ?? (user.email?.split(separator: "@").first.map(String.init))
            showHome = true
        } catch {
            print("❌ [OTPScreen] OTP verification failed: \(error.localizedDescription)")
            let errorMsg = error.localizedDescription.lowercased()
            if errorMsg.contains("code") && (errorMsg.contains("incorrect") || errorMsg.contains("invalid") || errorMsg.contains("invalide")) {
                errorMessage = "Code incorrect. Veuillez réessayer."
            } else if errorMsg.contains("expiré") || errorMsg.contains("expired") {
                errorMessage = "Le code a expiré. Veuillez demander un nouveau code."
            } else {
                errorMessage = "Erreur lors de la vérification. Veuillez réessayer."
            }
            // Clear OTP fields on error
            codeDigits = Array(repeating: "", count: 4)
            focusedField = 0
        }
    }
    
    private func startResendTimer() {
        canResend = false
        remainingSeconds = 300 // 5 minutes
        
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { timer in
            remainingSeconds -= 1
            if remainingSeconds <= 0 {
                canResend = true
                timer.invalidate()
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
    
    private var termsText: some View {
        VStack(spacing: 4) {
            Text("signup_notice_prefix")
                .font(.system(size: 13))
                .foregroundColor(Color(red: 0.35, green: 0.36, blue: 0.40))
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
            .foregroundColor(Color(red: 0.35, green: 0.36, blue: 0.40))
        + Text(" ")
        + Text("signup_privacy")
            .foregroundColor(accent)
            .underline()
    }
    
    private struct HeaderShape: Shape {
        func path(in rect: CGRect) -> Path {
            var path = Path()
            let radius: CGFloat = 60
            
            path.move(to: CGPoint(x: 0, y: radius))
            path.addQuadCurve(to: CGPoint(x: radius, y: 0),
                              control: CGPoint(x: 0, y: 0))
            path.addLine(to: CGPoint(x: rect.width - radius, y: 0))
            path.addQuadCurve(to: CGPoint(x: rect.width, y: radius),
                              control: CGPoint(x: rect.width, y: 0))
            path.addLine(to: CGPoint(x: rect.width, y: rect.height))
            path.addLine(to: CGPoint(x: 0, y: rect.height))
            path.closeSubpath()
            return path
        }
    }
}

private struct OTPDigitField: View {
    @Binding var text: String
    let isFocused: Bool
    
    var body: some View {
        TextField("", text: $text)
            .keyboardType(.numberPad)
            .textContentType(.oneTimeCode)
            .multilineTextAlignment(.center)
            .frame(width: 64, height: 58)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.white)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isFocused ? Color(red: 0.18, green: 0.55, blue: 0.99) : Color(red: 0.76, green: 0.80, blue: 0.85), lineWidth: isFocused ? 3 : 2)
            )
            .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
            .font(.system(size: 24, weight: .semibold))
    }
}

struct OTPScreenView_Previews: PreviewProvider {
    static var previews: some View {
        OTPScreenView(email: "test@example.com")
    }
}
