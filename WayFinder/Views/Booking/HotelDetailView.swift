//
//  HotelDetailView.swift
//  WayFinder
//
//  Hotel detail screen with gallery and reviews, matching Android HotelDetailScreen
//

import SwiftUI

struct HotelDetailView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    
    let hotel: Accommodation
    let destination: FlightDestination?
    let onBookNow: () -> Void
    
    @State private var selectedPhotoIndex = 0
    @State private var scrollOffset: CGFloat = 0
    
    private var photos: [String] {
        if hotel.photos.isEmpty {
            return ["https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800"]
        }
        return hotel.photos
    }
    
    var body: some View {
        GeometryReader { geometry in
            let headerHeight: CGFloat = 300
            let parallaxOffset = scrollOffset * 0.5
            let alpha = max(0, min(1, 1 - (scrollOffset / 300)))
            
            ZStack(alignment: .bottom) {
                // Background
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 0) {
                        // Header Image with Parallax
                        ZStack(alignment: .topLeading) {
                            // Image
                            AsyncImage(url: URL(string: photos[selectedPhotoIndex])) { phase in
                                switch phase {
                                case .success(let image):
                                    image
                                        .resizable()
                                        .scaledToFill()
                                case .failure, .empty:
                                    LinearGradient(
                                        colors: [.blue.opacity(0.6), .blue],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                @unknown default:
                                    Color.gray
                                }
                            }
                            .frame(width: geometry.size.width, height: headerHeight)
                            .clipped()
                            .offset(y: parallaxOffset)
                            .opacity(alpha)
                            
                            // Gradient overlay
                            LinearGradient(
                                colors: [
                                    Color.black.opacity(0.3),
                                    Color.clear,
                                    Color.black.opacity(0.5)
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                            .frame(height: headerHeight)
                            .opacity(alpha)
                            
                            // Back button
                            Button(action: { dismiss() }) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 20, weight: .semibold))
                                    .foregroundColor(.white)
                                    .frame(width: 40, height: 40)
                                    .background(Color.black.opacity(0.3))
                                    .clipShape(Circle())
                            }
                            .padding(.top, geometry.safeAreaInsets.top + 8)
                            .padding(.leading, 16)
                            .opacity(alpha)
                            
                            // Photo indicators
                            if photos.count > 1 {
                                HStack(spacing: 8) {
                                    ForEach(0..<photos.count, id: \.self) { index in
                                        Circle()
                                            .fill(index == selectedPhotoIndex ? Color.white : Color.white.opacity(0.5))
                                            .frame(width: index == selectedPhotoIndex ? 10 : 8)
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.top, headerHeight - 50)
                                .opacity(alpha)
                            }
                        }
                        .frame(height: headerHeight)
                        
                        // Content Card
                        VStack(alignment: .leading, spacing: 20) {
                            // Hotel name and rating
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(hotel.name)
                                        .font(.system(size: 24, weight: .bold))
                                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    
                                    HStack(spacing: 4) {
                                        Image(systemName: "mappin.circle.fill")
                                            .font(.system(size: 14))
                                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                        Text(hotel.address ?? hotel.location)
                                            .font(.system(size: 14))
                                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                    }
                                }
                                
                                Spacer()
                                
                                // Rating badge
                                HStack(spacing: 4) {
                                    Image(systemName: "star.fill")
                                        .foregroundColor(.yellow)
                                    Text(String(format: "%.1f", hotel.rating))
                                        .font(.system(size: 16, weight: .bold))
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(ThemeColors.accent().opacity(0.1))
                                .cornerRadius(12)
                            }
                            
                            // Reviews count
                            if let count = hotel.userRatingsTotal {
                                Text("\(count) avis")
                                    .font(.system(size: 14))
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            }
                            
                            // Photo gallery
                            if photos.count > 1 {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Photos")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    
                                    ScrollView(.horizontal, showsIndicators: false) {
                                        HStack(spacing: 12) {
                                            ForEach(0..<photos.count, id: \.self) { index in
                                                AsyncImage(url: URL(string: photos[index])) { phase in
                                                    switch phase {
                                                    case .success(let image):
                                                        image
                                                            .resizable()
                                                            .scaledToFill()
                                                    default:
                                                        Color.gray.opacity(0.3)
                                                    }
                                                }
                                                .frame(width: 100, height: 100)
                                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                                .onTapGesture {
                                                    selectedPhotoIndex = index
                                                }
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 12)
                                                        .stroke(
                                                            index == selectedPhotoIndex
                                                                ? ThemeColors.accent()
                                                                : Color.clear,
                                                            lineWidth: 3
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Description
                            if let description = hotel.description, !description.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("À propos")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    
                                    Text(description)
                                        .font(.system(size: 14))
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                        .lineSpacing(6)
                                }
                            }
                            
                            // Amenities
                            if !hotel.amenities.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Équipements")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    
                                    ScrollView(.horizontal, showsIndicators: false) {
                                        HStack(spacing: 8) {
                                            ForEach(hotel.amenities, id: \.self) { amenity in
                                                AmenityChipView(amenity: amenity)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Check-in/Check-out times
                            if hotel.checkInTime != nil || hotel.checkOutTime != nil {
                                HStack(spacing: 16) {
                                    if let checkIn = hotel.checkInTime {
                                        TimeInfoView(
                                            icon: "arrow.right.circle.fill",
                                            label: "Check-in",
                                            time: checkIn
                                        )
                                    }
                                    
                                    if let checkOut = hotel.checkOutTime {
                                        TimeInfoView(
                                            icon: "arrow.left.circle.fill",
                                            label: "Check-out",
                                            time: checkOut
                                        )
                                    }
                                }
                            }
                            
                            // Reviews
                            if !hotel.reviews.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Avis des clients")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    
                                    ForEach(hotel.reviews.prefix(5), id: \.authorName) { review in
                                        ReviewCardView(review: review)
                                    }
                                }
                            }
                            
                            // Spacer for bottom bar
                            Spacer().frame(height: 100)
                        }
                        .padding(24)
                        .background(
                            RoundedRectangle(cornerRadius: 32)
                                .fill(ThemeColors.background(colorScheme))
                        )
                        .offset(y: -40)
                    }
                    .background(GeometryReader { geo in
                        Color.clear
                            .onChange(of: geo.frame(in: .named("scroll")).origin.y) { newY in
                                scrollOffset = -newY
                            }
                    })
                }
                .coordinateSpace(name: "scroll")
                
                // Bottom booking bar
                VStack(spacing: 0) {
                    Divider()
                    
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Prix par nuit")
                                .font(.system(size: 12))
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            Text("\(Int(hotel.price)) \(hotel.currency)")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(ThemeColors.accent())
                        }
                        
                        Spacer()
                        
                        Button(action: onBookNow) {
                            Text("Réserver maintenant")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 16)
                                .background(ThemeColors.accent())
                                .cornerRadius(16)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .padding(.bottom, geometry.safeAreaInsets.bottom)
                }
                .background(ThemeColors.surface(colorScheme))
            }
            .ignoresSafeArea(edges: .bottom)
        }
        .navigationBarHidden(true)
    }
}

struct AmenityChipView: View {
    @Environment(\.colorScheme) private var colorScheme
    let amenity: String
    
    private var icon: String {
        switch amenity.lowercased() {
        case "wifi", "free_wifi": return "wifi"
        case "pool", "swimming_pool": return "figure.pool.swim"
        case "parking", "free_parking": return "car.fill"
        case "restaurant": return "fork.knife"
        case "spa", "wellness": return "leaf.fill"
        case "gym", "fitness", "fitness_center": return "dumbbell.fill"
        case "air_conditioning": return "snowflake"
        case "bar": return "wineglass.fill"
        case "room_service": return "bell.fill"
        case "breakfast": return "cup.and.saucer.fill"
        default: return "checkmark.circle.fill"
        }
    }
    
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 14))
            Text(amenity.replacingOccurrences(of: "_", with: " ").capitalized)
                .font(.system(size: 12))
        }
        .foregroundColor(ThemeColors.secondaryText(colorScheme))
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(ThemeColors.surface(colorScheme))
        .cornerRadius(20)
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
        )
    }
}

struct TimeInfoView: View {
    @Environment(\.colorScheme) private var colorScheme
    let icon: String
    let label: String
    let time: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(ThemeColors.accent())
            
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.system(size: 12))
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                Text(time)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
            }
        }
        .padding(16)
        .background(ThemeColors.surface(colorScheme))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
        )
    }
}

struct ReviewCardView: View {
    @Environment(\.colorScheme) private var colorScheme
    let review: HotelReview
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                // Avatar
                ZStack {
                    Circle()
                        .fill(ThemeColors.accent().opacity(0.2))
                        .frame(width: 40, height: 40)
                    
                    Text(String(review.authorName.prefix(1)).uppercased())
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(ThemeColors.accent())
                }
                
                Text(review.authorName)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                
                Spacer()
                
                HStack(spacing: 4) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.yellow)
                    Text(String(format: "%.1f", review.rating))
                        .font(.system(size: 14, weight: .medium))
                }
            }
            
            Text(review.text)
                .font(.system(size: 14))
                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                .lineLimit(4)
        }
        .padding(16)
        .background(ThemeColors.surface(colorScheme))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
        )
    }
}

#Preview {
    HotelDetailView(
        hotel: Accommodation(
            id: "1",
            name: "Grand Hotel Paris",
            type: "hotel",
            price: 250,
            currency: "EUR",
            rating: 4.5,
            imageUrl: "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800",
            location: "Paris, France",
            address: "123 Rue de Rivoli, 75001 Paris",
            cityCode: "PAR",
            description: "Experience luxury in the heart of Paris with stunning views of the Eiffel Tower.",
            photos: [],
            reviews: [],
            amenities: ["WiFi", "Pool", "Spa", "Restaurant"],
            contact: nil,
            latitude: nil,
            longitude: nil,
            checkInTime: "14:00",
            checkOutTime: "11:00",
            roomType: nil,
            boardType: nil,
            guestRating: nil,
            userRatingsTotal: 1250,
            pricePerNight: nil,
            totalPrice: nil
        ),
        destination: nil,
        onBookNow: {}
    )
}

