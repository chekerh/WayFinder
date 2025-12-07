import SwiftUI

struct LodgingChoiceView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    let destinationId: String
    let destination: FlightDestination?
    @State private var selectedType: String? = nil
    @State private var navigateToAccommodations = false
    @State private var selectedAccommodationType: String? = nil
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
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
        .navigationDestination(isPresented: $navigateToAccommodations) {
            if let type = selectedAccommodationType {
                AccommodationsListView(
                    destinationId: destinationId,
                    destination: destination,
                    accommodationType: type
                )
            }
        }
    }
    
    private var contentView: some View {
        VStack(alignment: .leading, spacing: 20) {
            // Header
            VStack(alignment: .leading, spacing: 8) {
                Text("Choisir le type de logement")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                
                Text("Sélectionnez le type de logement souhaité")
                    .font(.subheadline)
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            }
            .padding(.horizontal, 24)
            .padding(.top, 8)
            .padding(.bottom, 16)
            
            // Accommodation types
            VStack(spacing: 16) {
                ForEach(AccommodationType.allTypes) { type in
                    AccommodationTypeCard(
                        type: type,
                        isSelected: selectedType == type.id,
                        onTap: {
                            selectedType = type.id
                            selectedAccommodationType = type.id
                            navigateToAccommodations = true
                        }
                    )
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
    }
}

struct AccommodationTypeCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let type: AccommodationType
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 16) {
                Image(systemName: type.icon)
                    .font(.system(size: 32))
                    .foregroundColor(isSelected ? ThemeColors.accent() : ThemeColors.primaryText(colorScheme))
                    .frame(width: 48, height: 48)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(type.name)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(isSelected ? ThemeColors.accent() : ThemeColors.primaryText(colorScheme))
                    
                    Text(type.description)
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                
                Spacer()
                
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(ThemeColors.accent())
                }
            }
            .padding(20)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(isSelected ? ThemeColors.accent().opacity(0.1) : ThemeColors.surface(colorScheme))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? ThemeColors.accent() : Color.clear, lineWidth: 2)
            )
            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: isSelected ? 8 : 4, x: 0, y: 4)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    LodgingChoiceView(
        destinationId: "test",
        destination: nil
    )
}

