import SwiftUI
import MapKit

struct MapMemoriesView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = MapMemoriesViewModel()
    @State private var selectedCountry: CountryMemory?
    @State private var showCountryMemories = false
    @State private var selectedMemory: MapMemory?
    @State private var showMemoryDetail = false
    @State private var cameraPosition = MapCameraPosition.region(MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 50.0, longitude: 10.0), // Europe center
        span: MKCoordinateSpan(latitudeDelta: 60.0, longitudeDelta: 60.0)
    ))
    
    // For zoom calculation
    @State private var currentSpan: MKCoordinateSpan = MKCoordinateSpan(latitudeDelta: 60.0, longitudeDelta: 60.0)
    
    // Calculate zoom emoji based on latitudeDelta
    // Smaller latitudeDelta = closer zoom, larger = farther zoom
    // Using different emojis with same meaning (close to far) - not copying Snapchat
    private var zoomEmoji: String {
        let delta = currentSpan.latitudeDelta
        if delta < 0.1 {
            return "🐛" // Very close - insect on ground (very close to terrain)
        } else if delta < 1.0 {
            return "🦋" // Close but a bit far - butterfly flying low
        } else if delta < 10.0 {
            return "🪁" // A bit far - kite flying at medium altitude
        } else if delta < 50.0 {
            return "☁️" // Far - cloud in the sky
        } else if delta < 180.0 {
            return "🌙" // Very far - moon in space
        } else {
            return "🌍" // Entire planet - Earth view
        }
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                // Map
                Map(position: $cameraPosition) {
                    // Afficher par pays (comme avant) - avec les photos des voyages dans les marqueurs
                    let countries = viewModel.mapMemories?.countries ?? []
                    ForEach(countries) { country in
                        Annotation(country.country, coordinate: CLLocationCoordinate2D(latitude: country.lat, longitude: country.lng)) {
                            Button(action: {
                                selectedCountry = country
                                showCountryMemories = true
                                // Animate to country location
                                withAnimation {
                                    let newSpan = MKCoordinateSpan(latitudeDelta: 10.0, longitudeDelta: 10.0)
                                    cameraPosition = .region(MKCoordinateRegion(
                                        center: CLLocationCoordinate2D(latitude: country.lat, longitude: country.lng),
                                        span: newSpan
                                    ))
                                    currentSpan = newSpan
                                }
                            }) {
                                CountryMarker(country: country)
                            }
                        }
                    }
                }
                .onMapCameraChange { context in
                    currentSpan = context.region.span
                }
                .ignoresSafeArea()
                
                // Zoom indicator emoji (top right)
                // Made more visible with larger size and better contrast
                VStack {
                    HStack {
                        Spacer()
                        Text(zoomEmoji)
                            .font(.system(size: 40))
                            .frame(width: 56, height: 56)
                            .background(
                                Circle()
                                    .fill(Color.white)
                                    .shadow(color: Color.black.opacity(0.4), radius: 8, x: 0, y: 4)
                            )
                            .overlay(
                                Circle()
                                    .stroke(Color.black.opacity(0.1), lineWidth: 1)
                            )
                            .padding(.top, 60) // Below status bar
                            .padding(.trailing, 16)
                    }
                    Spacer()
                }
                
                // Loading state
                if viewModel.isLoading {
                    ProgressView()
                        .scaleEffect(1.5)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color.black.opacity(0.1))
                }
                
                // Error state
                if let error = viewModel.errorMessage, !viewModel.isLoading {
                    VStack(spacing: 16) {
                        Text(error)
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                            .padding()
                        
                        Button(action: {
                            Task {
                                await viewModel.loadMapMemories()
                            }
                        }) {
                            Text(String(localized: "map_memories_retry"))
                                .foregroundColor(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 12)
                                .background(ThemeColors.accent())
                                .cornerRadius(12)
                        }
                    }
                    .padding()
                    .background(ThemeColors.surface(colorScheme))
                    .cornerRadius(16)
                    .shadow(radius: 8)
                }
                
                // Empty state
                if (viewModel.mapMemories?.countries.isEmpty ?? true) && 
                   !viewModel.isLoading && 
                   viewModel.errorMessage == nil {
                    VStack(spacing: 16) {
                        Image(systemName: "map")
                            .font(.system(size: 64))
                            .foregroundColor(ThemeColors.accent().opacity(0.6))
                        
                        Text(String(localized: "map_memories_empty_title"))
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                        
                        Text(String(localized: "map_memories_empty_message"))
                            .font(.body)
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .padding(32)
                    .background(ThemeColors.surface(colorScheme))
                    .cornerRadius(24)
                    .shadow(radius: 12)
                }
                
                // Bottom sheet with countries list
                if let countries = viewModel.mapMemories?.countries, !countries.isEmpty {
                    VStack {
                        Spacer()
                        CountriesBottomSheet(
                            countries: countries,
                            onCountryTap: { country in
                                selectedCountry = country
                                showCountryMemories = true
                                withAnimation {
                                    let newSpan = MKCoordinateSpan(latitudeDelta: 10.0, longitudeDelta: 10.0)
                                    cameraPosition = .region(MKCoordinateRegion(
                                        center: CLLocationCoordinate2D(latitude: country.lat, longitude: country.lng),
                                        span: newSpan
                                    ))
                                    currentSpan = newSpan
                                }
                            }
                        )
                    }
                }
            }
            .navigationTitle(String(localized: "map_memories_title"))
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(true)
            .sheet(isPresented: $showCountryMemories) {
                if let country = selectedCountry {
                    CountryMemoriesSheet(
                        country: country,
                        onDismiss: {
                            showCountryMemories = false
                            selectedCountry = nil
                        },
                        onMemoryTap: { trip in
                            // Navigate to journey detail
                            showCountryMemories = false
                            selectedCountry = nil
                        }
                    )
                }
            }
            .sheet(isPresented: $showMemoryDetail) {
                if let memory = selectedMemory {
                    MemoryDetailSheet(
                        memory: memory,
                        onDismiss: {
                            showMemoryDetail = false
                            selectedMemory = nil
                        }
                    )
                }
            }
            .task {
                await viewModel.loadGoogleMapsApiKey()
                await viewModel.loadMapMemories()
            }
            .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("JourneyShared"))) { _ in
                // Rafraîchir la carte quand un voyage est partagé
                Task {
                    await viewModel.loadMapMemories()
                }
            }
        }
    }
}

// Marker pour les mémoires individuelles (style Snapchat)
struct MemoryMarker: View {
    let memory: MapMemory
    @State private var image: UIImage?
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        ZStack(alignment: .topTrailing) {
            // Marker image - photo du voyage partagé
            if let image = image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 70, height: 70)
                    .clipShape(Circle())
                    .overlay(
                        Circle()
                            .stroke(Color.white, lineWidth: 3)
                            .shadow(color: Color.black.opacity(0.3), radius: 2, x: 0, y: 1)
                    )
                    .shadow(color: Color.black.opacity(0.3), radius: 6, x: 0, y: 3)
            } else {
                Circle()
                    .fill(ThemeColors.accent())
                    .frame(width: 70, height: 70)
                    .overlay(
                        Image(systemName: "photo.fill")
                            .foregroundColor(.white)
                            .font(.system(size: 28))
                    )
                    .overlay(
                        Circle()
                            .stroke(Color.white, lineWidth: 3)
                    )
                    .shadow(color: Color.black.opacity(0.3), radius: 6, x: 0, y: 3)
            }
        }
        .onAppear {
            loadImage()
        }
    }
    
    private func loadImage() {
        // Charger la première image du voyage
        guard let firstImageUrl = memory.trip.images.first,
              let url = URL(string: firstImageUrl) else {
            return
        }
        
        Task {
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                if let uiImage = UIImage(data: data) {
                    await MainActor.run {
                        self.image = uiImage
                    }
                }
            } catch {
                print("Failed to load memory marker image: \(error)")
            }
        }
    }
}

struct CountryMarker: View {
    let country: CountryMemory
    @State private var image: UIImage?
    
    var body: some View {
        ZStack(alignment: .topTrailing) {
            // Marker image
            if let image = image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 60, height: 60)
                    .clipShape(Circle())
                    .overlay(
                        Circle()
                            .stroke(ThemeColors.accent(), lineWidth: 3)
                    )
                    .shadow(radius: 4)
            } else {
                Circle()
                    .fill(ThemeColors.accent())
                    .frame(width: 60, height: 60)
                    .overlay(
                        Image(systemName: "location.fill")
                            .foregroundColor(.white)
                            .font(.system(size: 24))
                    )
                    .shadow(radius: 4)
            }
            
            // Count badge
            if country.count > 1 {
                Text("\(country.count)")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.white)
                    .padding(6)
                    .background(ThemeColors.accent())
                    .clipShape(Circle())
                    .offset(x: 8, y: -8)
            }
        }
        .onAppear {
            loadImage()
        }
    }
    
    private func loadImage() {
        guard let firstImageUrl = country.trips.first?.images.first,
              let url = URL(string: firstImageUrl) else {
            return
        }
        
        Task {
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                if let uiImage = UIImage(data: data) {
                    await MainActor.run {
                        self.image = uiImage
                    }
                }
            } catch {
                print("Failed to load marker image: \(error)")
            }
        }
    }
}

struct CountriesBottomSheet: View {
    let countries: [CountryMemory]
    let onCountryTap: (CountryMemory) -> Void
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        VStack(spacing: 0) {
            // Handle
            RoundedRectangle(cornerRadius: 3)
                .fill(ThemeColors.secondaryText(colorScheme).opacity(0.3))
                .frame(width: 40, height: 4)
                .padding(.top, 8)
            
            // Header
            HStack {
                        Text(String(format: String(localized: "map_memories_countries_visited"), countries.count))
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 12)
            
            // Countries list
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(countries) { country in
                        CountryChip(
                            country: country,
                            onTap: { onCountryTap(country) }
                        )
                    }
                }
                .padding(.horizontal, 20)
            }
            .padding(.bottom, 20)
        }
        .background(ThemeColors.surface(colorScheme))
        .cornerRadius(20, corners: [.topLeft, .topRight])
        .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: -5)
        .frame(height: 260)
    }
}

struct CountryChip: View {
    let country: CountryMemory
    let onTap: () -> Void
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                Image(systemName: "location.fill")
                    .font(.system(size: 16))
                    .foregroundColor(ThemeColors.accent())
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(country.country)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.black)
                    
                    Text(String(format: String(localized: "map_memories_memory_count"), country.count))
                        .font(.system(size: 13))
                        .foregroundColor(.gray)
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.gray.opacity(0.2), lineWidth: 1)
            )
        }
    }
}

struct MemoryDetailSheet: View {
    let memory: MapMemory
    let onDismiss: () -> Void
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Images du voyage
                    if !memory.trip.images.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(Array(memory.trip.images.enumerated()), id: \.offset) { index, imageUrl in
                                    if let url = URL(string: imageUrl) {
                                        AsyncImage(url: url) { phase in
                                            switch phase {
                                            case .success(let image):
                                                image
                                                    .resizable()
                                                    .scaledToFill()
                                            case .failure, .empty:
                                                RoundedRectangle(cornerRadius: 12)
                                                    .fill(ThemeColors.surface(colorScheme))
                                                    .overlay(
                                                        Image(systemName: "photo")
                                                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                                    )
                                            @unknown default:
                                                RoundedRectangle(cornerRadius: 12)
                                                    .fill(ThemeColors.surface(colorScheme))
                                            }
                                        }
                                        .frame(width: 300, height: 300)
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                    }
                                }
                            }
                            .padding(.horizontal, 4)
                        }
                    }
                    
                    // Titre
                    Text(memory.trip.title)
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                    
                    // Description
                    if let description = memory.trip.description, !description.isEmpty {
                        Text(description)
                            .font(.body)
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    }
                    
                    // Tags
                    if !memory.trip.tags.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(memory.trip.tags, id: \.self) { tag in
                                    Text("#\(tag)")
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundColor(ThemeColors.accent())
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(
                                            Capsule()
                                                .fill(ThemeColors.accent().opacity(0.1))
                                        )
                                }
                            }
                        }
                    }
                    
                    // Stats
                    HStack(spacing: 24) {
                        HStack(spacing: 4) {
                            Image(systemName: "heart.fill")
                                .foregroundColor(.red)
                            Text("\(memory.trip.likesCount)")
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        }
                        
                        HStack(spacing: 4) {
                            Image(systemName: "bubble.left.and.bubble.right.fill")
                                .foregroundColor(ThemeColors.accent())
                            Text("\(memory.trip.commentsCount)")
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        }
                    }
                    .font(.system(size: 16))
                }
                .padding()
            }
            .navigationTitle(memory.trip.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onDismiss) {
                        Text(String(localized: "generic_ok"))
                    }
                }
            }
        }
    }
}

struct CountryMemoriesSheet: View {
    let country: CountryMemory
    let onDismiss: () -> Void
    let onMemoryTap: (SharedTrip) -> Void
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        NavigationView {
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(country.trips) { trip in
                        MemoryCard(trip: trip, onTap: { onMemoryTap(trip) })
                    }
                }
                .padding()
            }
            .navigationTitle("\(String(localized: "map_memories_memories")) - \(country.country)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onDismiss) {
                        Text(String(localized: "generic_ok"))
                    }
                }
            }
        }
    }
}

struct MemoryCard: View {
    let trip: SharedTrip
    let onTap: () -> Void
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                if let firstImage = trip.images.first {
                    AsyncImage(url: URL(string: firstImage)) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                        case .failure, .empty:
                            RoundedRectangle(cornerRadius: 8)
                                .fill(ThemeColors.surface(colorScheme))
                                .overlay(
                                    Image(systemName: "photo")
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                )
                        @unknown default:
                            RoundedRectangle(cornerRadius: 8)
                                .fill(ThemeColors.surface(colorScheme))
                        }
                    }
                    .frame(width: 80, height: 80)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                } else {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(ThemeColors.surface(colorScheme))
                        .frame(width: 80, height: 80)
                        .overlay(
                            Image(systemName: "photo")
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        )
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(trip.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                        .lineLimit(2)
                    
                    if let description = trip.description {
                        Text(description)
                            .font(.system(size: 14))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .lineLimit(2)
                    }
                }
                
                Spacer()
            }
            .padding()
            .background(ThemeColors.surface(colorScheme))
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
}

// RoundedCorner extension is defined in ActivitiesPreviewView.swift



