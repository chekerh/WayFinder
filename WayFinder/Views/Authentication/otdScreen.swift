//
//  otdScreen.swift
//  WayFinder
//
//  Created by sarrachmek on 6/11/2025.
//

import SwiftUI

struct OTPScreenView: View {
    @State private var codeDigits: [String] = Array(repeating: "", count: 4)
    
    var body: some View {
        VStack {
            HeaderShape()
                .fill(Color(red: 0.90, green: 0.95, blue: 1.0))
                .frame(height: 320)
                .overlay(headerContent, alignment: .topLeading)
                .overlay(codeSection, alignment: .bottom)
                .padding(.bottom, -80)
            
            Spacer()
            
            Spacer()
            
            VStack(spacing: 8) {
                Button(action: {
                    // Action
                }) {
                    HStack(spacing: 16) {
                        Text("otp_button")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                        
                        Image(systemName: "globe")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 32)
                    .padding(.vertical, 16)
                    .background(Color(red: 0.18, green: 0.55, blue: 0.99))
                    .clipShape(Capsule())
                }
                .padding(.horizontal, 80)
                
                termsText
                    .padding(.horizontal, 24)
            }
            .padding(.bottom, 24)
        }
        .background(Color(red: 0.90, green: 0.95, blue: 1.0).ignoresSafeArea())
    }
    
    private var headerContent: some View {
        VStack(alignment: .leading, spacing: 22) {
            HStack {
                Button(action: {
                    // Back action
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
            
            Text("otp_title")
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(Color(red: 0.16, green: 0.17, blue: 0.20))
                .padding(.leading, 40)
        }
    }
    
    private var codeSection: some View {
        VStack(spacing: 36) {
            HStack(spacing: 28) {
                ForEach(0..<4, id: \.self) { index in
                    OTPDigitField(text: $codeDigits[index])
                }
            }
            .padding(.horizontal, 40)
            .padding(.bottom, 60)
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
    @FocusState private var isFocused: Bool
    
    var body: some View {
        TextField("", text: $text)
            .keyboardType(.numberPad)
            .textContentType(.oneTimeCode)
            .multilineTextAlignment(.center)
            .focused($isFocused)
            .frame(width: 64, height: 58)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.white)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color(red: 0.76, green: 0.80, blue: 0.85), lineWidth: 4)
            )
            .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
            .font(.system(size: 24, weight: .semibold))
            .onReceive(text.publisher.collect()) { value in
                if let first = value.first {
                    text = String(first)
                }
            }
    }
}

struct OTPScreenView_Previews: PreviewProvider {
    static var previews: some View {
        OTPScreenView()
    }
}
