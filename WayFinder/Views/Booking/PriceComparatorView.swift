//
//  PriceComparatorView.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

struct PriceComparatorView: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var sort: SortOption = .price
    @State private var oneStopOnly = false
    @State private var selectedFlightId: String?
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 16) {
                header
                sortChips
                
                ScrollView(showsIndicators: false) {
                    LazyVStack(spacing: 14) {
                        ForEach(filteredFlights) { flight in
                            FlightCard(
                                flight: flight,
                                isSelected: selectedFlightId == flight.id
                            ) {
                                selectedFlightId = flight.id
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 4)
                }
                
                Button(action: {}) {
                    Text("Confirmer la sélection")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .background(ThemeColors.accent())
                .clipShape(Capsule())
                .padding(.horizontal, 24)
                .padding(.bottom, 12)
            }
        }
        .navigationTitle("")
        .navigationBarHidden(true)
    }
    
    private var header: some View {
        HStack {
            Text("Comparateur des Prix")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
            Image(systemName: "dollarsign.circle.fill")
                .font(.system(size: 28, weight: .semibold))
                .foregroundColor(.green)
        }
        .padding(.horizontal, 24)
        .padding(.top, 12)
    }
    
    private var sortChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                SortChip(title: "Prix", isActive: sort == .price) { sort = .price }
                SortChip(title: "Durée", isActive: sort == .duration) { sort = .duration }
                SortChip(title: "Sans escale", isActive: oneStopOnly) { oneStopOnly.toggle() }
            }
            .padding(.horizontal, 16)
        }
    }
    
    private var filteredFlights: [FlightItem] {
        let flights = mockFlights
        let base = oneStopOnly ? flights.filter { $0.stops == 0 } : flights
        switch sort {
        case .price:
            return base.sorted { $0.price < $1.price }
        case .duration:
            return base.sorted { $0.durationMinutes < $1.durationMinutes }
        }
    }
    
    private var mockFlights: [FlightItem] {
        [
            FlightItem(id: "af432", code: "AF 432", durationMinutes: 125, airline: "Air France", price: 89, stops: 0),
            FlightItem(id: "tu908", code: "TU 908", durationMinutes: 150, airline: "Qatar Airways", price: 109, stops: 1),
            FlightItem(id: "ib612", code: "IB 612", durationMinutes: 180, airline: "Tunisair", price: 129, stops: 0),
            FlightItem(id: "tu908-2", code: "TU 908", durationMinutes: 150, airline: "Swiss Air", price: 109, stops: 1),
            FlightItem(id: "ib612-2", code: "IB 612", durationMinutes: 180, airline: "Boing", price: 129, stops: 0)
        ]
    }
    
    enum SortOption { case price, duration }
}

// MARK: - Components
private struct SortChip: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let isActive: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title + (isActive && title == "Prix" ? " ↑" : ""))
                .font(.subheadline.weight(.semibold))
                .foregroundColor(isActive ? .white : ThemeColors.primaryText(colorScheme))
                .padding(.vertical, 10)
                .padding(.horizontal, 16)
                .background(
                    Capsule()
                        .fill(isActive ? ThemeColors.accent() : ThemeColors.surface(colorScheme))
                )
        }
        .buttonStyle(.plain)
    }
}

private struct FlightCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let flight: FlightItem
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 10) {
                        Image(systemName: "airplane")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(ThemeColors.accent())
                        Text("Vol \(flight.code)  –  \(flight.durationFormatted)")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    }
                    Text(flight.airline)
                        .font(.caption)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                Spacer()
                Text("\(flight.price)TND")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(ThemeColors.surface(colorScheme))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(isSelected ? ThemeColors.accent() : Color.clear, lineWidth: 2)
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Model
struct FlightItem: Identifiable {
    let id: String
    let code: String
    let durationMinutes: Int
    let airline: String
    let price: Int
    let stops: Int
    
    var durationFormatted: String {
        let h = durationMinutes / 60
        let m = durationMinutes % 60
        return "\(h)h\(String(format: "%02d", m))"
    }
}

struct PriceComparatorView_Previews: PreviewProvider {
    static var previews: some View {
        PriceComparatorView()
    }
}


