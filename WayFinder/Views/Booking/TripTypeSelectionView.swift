//
//  TripTypeSelectionView.swift
//  WayFinder
//
//  Trip type selection screen matching Android TripTypeSelectionScreen
//

import SwiftUI

struct TripType: Identifiable {
    let id: String
    let name: String
    let icon: String
    let description: String
    let gradient: [Color]
}

struct TripTypeSelectionView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    
    let destination: FlightDestination?
    let onTripTypeSelected: (String) -> Void
    
    @State private var selectedTripType: String?
    
    private let tripTypes: [TripType] = [
        TripType(
            id: "business",
            name: String(localized: "Affaires"),
            icon: "briefcase.fill",
            description: String(localized: "Voyage professionnel"),
            gradient: [Color.blue.opacity(0.8), Color.blue]
        ),
        TripType(
            id: "romantic",
            name: String(localized: "Romantique"),
            icon: "heart.fill",
            description: String(localized: "Escapade en amoureux"),
            gradient: [Color.pink.opacity(0.8), Color.red]
        ),
        TripType(
            id: "family",
            name: String(localized: "Famille"),
            icon: "figure.2.and.child.holdinghands",
            description: String(localized: "Aventures en famille"),
            gradient: [Color.orange.opacity(0.8), Color.orange]
        ),
        TripType(
            id: "adventure",
            name: String(localized: "Aventure"),
            icon: "figure.hiking",
            description: String(localized: "Exploration et sensations fortes"),
            gradient: [Color.green.opacity(0.8), Color.green]
        ),
        TripType(
            id: "relaxation",
            name: String(localized: "Détente"),
            icon: "leaf.fill",
            description: String(localized: "Repos et bien-être"),
            gradient: [Color.teal.opacity(0.8), Color.cyan]
        ),
        TripType(
            id: "cultural",
            name: String(localized: "Culturel"),
            icon: "building.columns.fill",
            description: String(localized: "Découverte du patrimoine"),
            gradient: [Color.purple.opacity(0.8), Color.purple]
        ),
        TripType(
            id: "camping",
            name: String(localized: "Camping"),
            icon: "tent.fill",
            description: String(localized: "Nature et plein air"),
            gradient: [Color.brown.opacity(0.8), Color.brown]
        )
    ]
    
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
                
                Text("Choisir le type de voyage")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                
                Spacer()
                
                // Placeholder for symmetry
                Color.clear.frame(width: 20, height: 20)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(ThemeColors.surface(colorScheme))
            
            // Content
            ScrollView {
                VStack(spacing: 16) {
                    Text("Sélectionnez le type de voyage qui correspond le mieux à vos attentes.")
                        .font(.system(size: 16))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        .padding(.horizontal, 24)
                        .padding(.top, 16)
                    
                    LazyVStack(spacing: 12) {
                        ForEach(tripTypes) { tripType in
                            TripTypeCard(
                                tripType: tripType,
                                isSelected: selectedTripType == tripType.id,
                                onTap: {
                                    selectedTripType = tripType.id
                                    onTripTypeSelected(tripType.id)
                                }
                            )
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 24)
                }
            }
        }
        .background(ThemeColors.background(colorScheme))
        .navigationBarHidden(true)
    }
}

struct TripTypeCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let tripType: TripType
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 16) {
                // Icon with gradient background
                ZStack {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: tripType.gradient,
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 56, height: 56)
                    
                    Image(systemName: tripType.icon)
                        .font(.system(size: 24))
                        .foregroundColor(.white)
                }
                
                // Text content
                VStack(alignment: .leading, spacing: 4) {
                    Text(tripType.name)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(
                            isSelected 
                                ? ThemeColors.primary(colorScheme)
                                : ThemeColors.primaryText(colorScheme)
                        )
                    
                    Text(tripType.description)
                        .font(.system(size: 14))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                }
                
                Spacer()
                
                // Selection indicator
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(ThemeColors.primary(colorScheme))
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(
                        isSelected
                            ? ThemeColors.primary(colorScheme).opacity(0.1)
                            : ThemeColors.surface(colorScheme)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(
                        isSelected
                            ? ThemeColors.primary(colorScheme)
                            : Color.gray.opacity(0.2),
                        lineWidth: isSelected ? 2 : 1
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    TripTypeSelectionView(
        destination: nil,
        onTripTypeSelected: { _ in }
    )
}

