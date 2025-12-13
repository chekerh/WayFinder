//
//  PaymentSummaryView.swift
//  WayFinder
//
//  Payment summary screen with 5% fee breakdown, matching Android PaymentSummaryScreen
//

import SwiftUI

struct PaymentSummaryView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    
    let destination: FlightDestination?
    let accommodation: Accommodation?
    let nights: Int
    let onPayWithPayPal: () -> Void
    
    @State private var isProcessing = false
    
    private let wayfinderFeePercent: Double = 5.0
    
    private var flightPrice: Double {
        destination?.price ?? 0
    }
    
    private var currency: String {
        destination?.currency ?? accommodation?.currency ?? "EUR"
    }
    
    private var hotelTotal: Double {
        (accommodation?.price ?? 0) * Double(nights)
    }
    
    private var subtotal: Double {
        flightPrice + hotelTotal
    }
    
    private var serviceFee: Double {
        subtotal * (wayfinderFeePercent / 100)
    }
    
    private var totalPrice: Double {
        subtotal + serviceFee
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                }
                
                Spacer()
                
                Text("Résumé du paiement")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                
                Spacer()
                
                Color.clear.frame(width: 20, height: 20)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(ThemeColors.surface(colorScheme))
            
            ScrollView {
                VStack(spacing: 20) {
                    // Trip Summary Card
                    VStack(alignment: .leading, spacing: 16) {
                        HStack(spacing: 12) {
                            Image(systemName: "airplane.departure")
                                .font(.system(size: 24))
                                .foregroundColor(ThemeColors.primary(colorScheme))
                            
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Votre voyage à")
                                    .font(.system(size: 14))
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                Text("\(destination?.name ?? "Destination"), \(destination?.country ?? "")")
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                            }
                        }
                        
                        Divider()
                        
                        // Flight details
                        if let dest = destination {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Vol aller-retour")
                                        .font(.system(size: 14))
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                    Text(dest.airline ?? "Compagnie aérienne")
                                        .font(.system(size: 12))
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme).opacity(0.7))
                                }
                                
                                Spacer()
                                
                                Text(String(format: "%.2f %@", flightPrice, currency))
                                    .font(.system(size: 16, weight: .medium))
                                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                            }
                        }
                        
                        // Hotel details
                        if let hotel = accommodation, hotelTotal > 0 {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(hotel.name)
                                        .font(.system(size: 14))
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                    Text("\(nights) nuits × \(String(format: "%.2f", hotel.price)) \(currency)")
                                        .font(.system(size: 12))
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme).opacity(0.7))
                                }
                                
                                Spacer()
                                
                                Text(String(format: "%.2f %@", hotelTotal, currency))
                                    .font(.system(size: 16, weight: .medium))
                                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                            }
                        }
                    }
                    .padding(20)
                    .background(ThemeColors.surface(colorScheme))
                    .cornerRadius(20)
                    .shadow(color: Color.black.opacity(0.05), radius: 8, y: 2)
                    
                    // Price Breakdown Card
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Détail du prix")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                        
                        Divider()
                        
                        // Subtotal
                        HStack {
                            Text("Sous-total")
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            Spacer()
                            Text(String(format: "%.2f %@", subtotal, currency))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                        }
                        
                        // Service fee
                        HStack {
                            HStack(spacing: 4) {
                                Text("Frais de service WayFinder")
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                Text("(\(Int(wayfinderFeePercent))%)")
                                    .font(.system(size: 12))
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme).opacity(0.7))
                            }
                            Spacer()
                            Text(String(format: "%.2f %@", serviceFee, currency))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                        }
                        
                        Divider()
                            .frame(height: 2)
                            .background(ThemeColors.primaryText(colorScheme).opacity(0.2))
                        
                        // Total
                        HStack {
                            Text("TOTAL")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                            Spacer()
                            Text(String(format: "%.2f %@", totalPrice, currency))
                                .font(.system(size: 22, weight: .bold))
                                .foregroundColor(ThemeColors.primary(colorScheme))
                        }
                    }
                    .padding(20)
                    .background(ThemeColors.surface(colorScheme))
                    .cornerRadius(20)
                    .shadow(color: Color.black.opacity(0.05), radius: 8, y: 2)
                    
                    // Info card
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: "info.circle.fill")
                            .font(.system(size: 20))
                            .foregroundColor(ThemeColors.primary(colorScheme))
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Paiement sécurisé")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                            Text("Vous payez WayFinder qui s'occupe de toutes les réservations. Vos billets et confirmations seront disponibles immédiatement.")
                                .font(.system(size: 12))
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        }
                    }
                    .padding(16)
                    .background(ThemeColors.primary(colorScheme).opacity(0.1))
                    .cornerRadius(16)
                    
                    Spacer().frame(height: 20)
                    
                    // Payment method
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Méthode de paiement")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                        
                        // PayPal button
                        Button(action: {
                            if !isProcessing {
                                isProcessing = true
                                onPayWithPayPal()
                            }
                        }) {
                            HStack {
                                if isProcessing {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Image(systemName: "creditcard.fill")
                                        .font(.system(size: 20))
                                }
                                
                                Text("Payer avec PayPal")
                                    .font(.system(size: 16, weight: .bold))
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(ThemeColors.primary(colorScheme))
                            .cornerRadius(16)
                        }
                        .disabled(isProcessing)
                        
                        Text("Carte bancaire également acceptée via PayPal")
                            .font(.system(size: 12))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .frame(maxWidth: .infinity, alignment: .center)
                    }
                    
                    Spacer().frame(height: 40)
                }
                .padding(24)
            }
        }
        .background(ThemeColors.background(colorScheme))
        .navigationBarHidden(true)
    }
}

#Preview {
    PaymentSummaryView(
        destination: FlightDestination(
            id: "1",
            name: "Paris",
            city: "Paris",
            country: "France",
            imageUrl: nil,
            price: 350,
            currency: "EUR",
            description: nil,
            departureDate: nil,
            arrivalDate: nil,
            airline: "Air France"
        ),
        accommodation: Accommodation(
            id: "1",
            name: "Grand Hotel Paris",
            type: "hotel",
            price: 180,
            currency: "EUR",
            rating: 4.5,
            imageUrl: nil,
            location: "Paris",
            address: nil,
            cityCode: nil,
            description: nil,
            photos: [],
            reviews: [],
            amenities: [],
            contact: nil,
            latitude: nil,
            longitude: nil,
            checkInTime: nil,
            checkOutTime: nil,
            roomType: nil,
            boardType: nil,
            guestRating: nil,
            userRatingsTotal: nil,
            pricePerNight: nil,
            totalPrice: nil
        ),
        nights: 3,
        onPayWithPayPal: {}
    )
}

