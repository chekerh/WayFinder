import SwiftUI

struct ReservationScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var bookingViewModel = BookingViewModel()
    let destinationId: String
    let destination: FlightDestination?
    var onBackToHome: (() -> Void)? = nil
    
    @State private var cardNumber = ""
    @State private var cardHolderName = ""
    @State private var expiryDate = ""
    @State private var cvv = ""
    @State private var showConfirmation = false
    @State private var confirmationNumber: String?
    @State private var navigateToConfirmation = false
    
    var body: some View {
        ZStack {
            backgroundView
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    // Back button at top
                    HStack {
                        Button(action: { dismiss() }) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                                .frame(width: 44, height: 44)
                                .background(
                                    ThemeColors.surface(colorScheme)
                                        .opacity(colorScheme == .dark ? 0.9 : 0.95)
                                )
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .padding(.leading, 24)
                        .padding(.top, 16)
                        
                        Spacer()
                    }
                    
                    contentView
                }
            }
            .safeAreaPadding(.horizontal)
        }
        .navigationBarHidden(true)
        .navigationDestination(isPresented: $navigateToConfirmation) {
            confirmationScreen
        }
    }
    
    private var backgroundView: some View {
        ThemeColors.background(colorScheme)
            .ignoresSafeArea()
    }
    
    private var contentView: some View {
        VStack(spacing: 20) {
                    if let destination = destination {
                        // Destination Card
                        VStack(alignment: .leading, spacing: 12) {
                            Text(destination.name)
                                .font(.system(size: 28, weight: .bold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            Text("\(destination.country) • \(destination.airline ?? "N/A")")
                                .font(.body)
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            
                            if let departureDate = destination.departureDate,
                               let arrivalDate = destination.arrivalDate {
                                Text(String(format: String(localized: "reservation_departure_return"), formatDate(departureDate), formatDate(arrivalDate)))
                                    .font(.body)
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            }
                            
                            if let price = destination.price, price > 0 {
                                Text(String(format: "%.2f %@", price, destination.currency))
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                    .padding(.top, 4)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(20)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(cardBackground)
                        )
                        .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
                    }
                    
                    // Payment Information Card
                    VStack(alignment: .leading, spacing: 16) {
                        HStack(spacing: 8) {
                            Image(systemName: "creditcard.fill")
                                .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                            Text("reservation_payment_info")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        }
                        .padding(.bottom, 8)
                        
                        // Card Number with icon
                        HStack {
                            Image(systemName: "creditcard")
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            TextField(String(localized: "reservation_card_number"), text: $cardNumber)
                                .keyboardType(.numberPad)
                                .onChange(of: cardNumber) { oldValue, newValue in
                                    // Limiter à 16 chiffres maximum
                                    let digitsOnly = newValue.filter { $0.isNumber }
                                    if digitsOnly.count <= 16 {
                                        // Format card number (add spaces every 4 digits)
                                        let formatted = digitsOnly
                                            .chunked(into: 4)
                                            .joined(separator: " ")
                                        cardNumber = formatted
                                    } else {
                                        // Garder l'ancienne valeur si on dépasse 16 chiffres
                                        cardNumber = oldValue
                                    }
                                }
                        }
                        .padding(12)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(fieldBackground)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(fieldBorder, lineWidth: 1)
                                )
                        )
                        
                        // Card Holder Name with icon
                        HStack {
                            Image(systemName: "person")
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            TextField("Nom sur la carte", text: $cardHolderName)
                        }
                        .padding(12)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(fieldBackground)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(fieldBorder, lineWidth: 1)
                                )
                        )
                        
                        // Expiry and CVV side by side
                        HStack(spacing: 12) {
                            HStack {
                                Image(systemName: "calendar")
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                TextField("Expiration", text: $expiryDate)
                                    .keyboardType(.numberPad)
                                    .onChange(of: expiryDate) { oldValue, newValue in
                                        // Format MM/YY
                                        let formatted = newValue.filter { $0.isNumber }
                                        if formatted.count <= 4 {
                                            expiryDate = formatted.count > 2
                                                ? "\(formatted.prefix(2))/\(formatted.dropFirst(2))"
                                                : formatted
                                        }
                                    }
                            }
                            .padding(12)
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(fieldBackground)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(fieldBorder, lineWidth: 1)
                                    )
                            )
                            
                            HStack {
                                Image(systemName: "lock")
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                TextField("CVV", text: $cvv)
                                    .keyboardType(.numberPad)
                                    .onChange(of: cvv) { oldValue, newValue in
                                        if newValue.allSatisfy({ $0.isNumber }) && newValue.count <= 3 {
                                            cvv = newValue
                                        }
                                    }
                            }
                            .padding(12)
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(fieldBackground)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(fieldBorder, lineWidth: 1)
                                    )
                            )
                        }
                    }
                    .padding(20)
                    .background(
                        RoundedRectangle(cornerRadius: 16)
                            .fill(cardBackground)
                    )
                    .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
                    
                    // Summary Card
                    VStack(alignment: .leading, spacing: 16) {
                        Text("reservation_summary")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        VStack(spacing: 12) {
                            HStack {
                                Text("reservation_flight_price")
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                Spacer()
                                if let price = destination?.price, price > 0 {
                                    Text(String(format: "%.2f %@", price, destination?.currency ?? "EUR"))
                                        .fontWeight(.medium)
                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                } else {
                                    Text("generic_not_available")
                                        .fontWeight(.medium)
                                }
                            }
                            
                            HStack {
                                Text("reservation_taxes")
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                Spacer()
                                let taxes = 30.0
                                Text(String(format: "%.2f %@", taxes, destination?.currency ?? "EUR"))
                                    .fontWeight(.medium)
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                            
                            HStack {
                                Text("reservation_baggage_services")
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                Spacer()
                                let baggage = 30.0
                                Text(String(format: "%.2f %@", baggage, destination?.currency ?? "EUR"))
                                    .fontWeight(.medium)
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                        }
                        
                        Divider()
                        
                        HStack {
                            Text("reservation_total")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            Spacer()
                            let total = (destination?.price ?? 0) + 30.0 + 30.0
                            Text(String(format: "%.2f %@", total, destination?.currency ?? "EUR"))
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                        }
                    }
                    .padding(20)
                    .background(
                        RoundedRectangle(cornerRadius: 16)
                            .fill(cardBackground)
                    )
                    .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
                    
                    // Confirm Button
                    Button {
                        Task {
                            do {
                                // Prepare payment details (all values must be strings for the API)
                                let paymentDetails: [String: Any] = [
                                    "method": "credit_card" as Any,
                                    "card_number": cardNumber.replacingOccurrences(of: " ", with: "") as Any,
                                    "card_holder": cardHolderName as Any,
                                    "expiry_date": expiryDate as Any,
                                    "cvv": cvv as Any
                                ]
                                
                                // Calculate total price
                                let totalPrice = (destination?.price ?? 0) + 30.0 + 30.0
                                
                                let response = try await bookingViewModel.confirmBooking(
                                    offerId: destinationId,
                                    paymentDetails: paymentDetails,
                                    totalPrice: totalPrice
                                )
                                confirmationNumber = response.confirmationNumber
                                navigateToConfirmation = true
                            } catch {
                                // Handle error
                                print("Error confirming booking: \(error)")
                            }
                        }
                    } label: {
                        Text("reservation_confirm")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(
                                Color(red: 0.098, green: 0.463, blue: 0.824)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .disabled(
                        cardNumber.replacingOccurrences(of: " ", with: "").count < 16 ||
                        cardHolderName.isEmpty ||
                        expiryDate.count < 5 ||
                        cvv.count < 3 ||
                        destination == nil
                    )
                    .opacity(
                        cardNumber.replacingOccurrences(of: " ", with: "").count < 16 ||
                        cardHolderName.isEmpty ||
                        expiryDate.count < 5 ||
                        cvv.count < 3 ||
                        destination == nil
                        ? 0.6 : 1.0
                    )
                    .padding(.bottom, 32)
        }
        .padding(.horizontal, 24)
        .padding(.top, 8)
        .padding(.bottom, 24)
    }
    
    private var cardBackground: Color {
        ThemeColors.surface(colorScheme)
    }
    
    private var fieldBackground: Color {
        colorScheme == .dark ? Color.white.opacity(0.08) : Color.white.opacity(0.95)
    }
    
    private var fieldBorder: Color {
        ThemeColors.border(colorScheme)
    }
    
    private func formatDate(_ dateString: String) -> String {
        if let tIndex = dateString.firstIndex(of: "T") {
            return String(dateString[..<tIndex])
        }
        return dateString
    }
    
    @ViewBuilder
    private var confirmationScreen: some View {
        if let confirmationNumber = confirmationNumber {
            ConfirmationScreen(
                confirmationNumber: confirmationNumber,
                destination: destination,
                onBackToHome: {
                    // Fermer ConfirmationScreen d'abord
                    navigateToConfirmation = false
                    // Puis fermer ReservationScreen
                    Task { @MainActor in
                        // Attendre que ConfirmationScreen soit fermé
                        try? await Task.sleep(nanoseconds: 300_000_000) // 0.3 secondes
                        // Fermer ReservationScreen
                        dismiss()
                        // Si onBackToHome est fourni depuis FlightDetailScreen, l'utiliser après un court délai
                        // Cela fermera FlightDetailScreen et reviendra à Home
                        if let onBackToHome = onBackToHome {
                            try? await Task.sleep(nanoseconds: 200_000_000) // 0.2 secondes
                            onBackToHome()
                        }
                    }
                }
            )
        } else {
            EmptyView()
        }
    }
}

extension String {
    func chunked(into size: Int) -> [String] {
        var chunks: [String] = []
        var index = startIndex
        
        while index < endIndex {
            let offset = min(size, distance(from: index, to: endIndex))
            let nextIndex = self.index(index, offsetBy: offset)
            chunks.append(String(self[index..<nextIndex]))
            index = nextIndex
        }
        
        return chunks
    }
}

