//
//  ConfirmationScreen.swift
//  WayFinder
//
//  Created by sarrachmek on 20/11/2025.
//

import SwiftUI

struct ConfirmationScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @Environment(\.presentationMode) var presentationMode
    let confirmationNumber: String
    let destination: FlightDestination?
    var onBackToHome: (() -> Void)? = nil
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 32) {
                    // Header with title
                    HStack {
                        Text("confirmation_title")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        Spacer()
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 16)
                    
                    // Success Icon and Message
                    VStack(spacing: 20) {
                        // Green checkmark circle
                        ZStack {
                            Circle()
                                .fill(Color(red: 0.133, green: 0.694, blue: 0.298)) // Green
                                .frame(width: 100, height: 100)
                            
                            Image(systemName: "checkmark")
                                .font(.system(size: 50, weight: .bold))
                                .foregroundColor(.white)
                        }
                        
                        // Confirmation message
                        VStack(spacing: 12) {
                            Text("confirmation_success_title")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(Color(red: 0.133, green: 0.694, blue: 0.298))
                            
                            Text("confirmation_success_message")
                                .font(.body)
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 24)
                        }
                    }
                    .padding(.top, 24)
                    
                    // Reservation Details Card
                    VStack(alignment: .leading, spacing: 16) {
                        Text("confirmation_details_title")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        VStack(spacing: 16) {
                            HStack {
                                Text("confirmation_number_label")
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                Spacer()
                                Text(confirmationNumber)
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                            
                            HStack {
                                Text("confirmation_status")
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                Spacer()
                                Text("confirmation_status_confirmed")
                                    .font(.system(size: 16, weight: .medium))
                                    .foregroundColor(Color(red: 0.133, green: 0.694, blue: 0.298))
                            }
                        }
                    }
                    .padding(20)
                    .background(
                        RoundedRectangle(cornerRadius: 16)
                            .fill(Color.white)
                    )
                    .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
                    .padding(.horizontal, 24)
                    
                    Spacer()
                        .frame(height: 40)
                    
                    // Action Buttons
                    VStack(spacing: 16) {
                        // View My Reservations Button (Blue)
                        NavigationLink(destination: BookingHistoryView()) {
                            Text("confirmation_view_reservations")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(Color(red: 0.098, green: 0.463, blue: 0.824))
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                        .buttonStyle(.plain)
                        
                        // Return to Home Button (White with blue border)
                        Button(action: {
                            // Appeler le callback pour fermer toutes les vues et revenir à Home
                            // Fermer d'abord cette vue
                            dismiss()
                            // Puis appeler le callback parent si disponible
                            if let onBackToHome = onBackToHome {
                                // Utiliser un petit délai pour s'assurer que cette vue est fermée
                                Task { @MainActor in
                                    try? await Task.sleep(nanoseconds: 300_000_000) // 0.3 secondes
                                    onBackToHome()
                                }
                            }
                        }) {
                            Text("confirmation_back_home")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(Color.white)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(Color(red: 0.098, green: 0.463, blue: 0.824), lineWidth: 2)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 32)
                }
            }
        }
        .navigationBarHidden(true)
    }
}

#Preview {
    NavigationStack {
        ConfirmationScreen(
            confirmationNumber: "CONF-AF9A0UJF",
            destination: FlightDestination(
                id: "1",
                name: "Rome",
                city: "Rome",
                country: "Italy",
                imageUrl: nil,
                price: 191.33,
                currency: "EUR",
                description: nil,
                departureDate: "2025-12-03T07:55:00",
                arrivalDate: "2025-12-03T09:20:00",
                airline: "TU"
            )
        )
    }
}

