import SwiftUI
import PhotosUI

@MainActor
struct ShareTripView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var journeyViewModel = JourneyViewModel()
    @StateObject private var bookingViewModel = BookingViewModel()
    
    @State private var selectedBooking: Booking?
    @State private var selectedPhotos: [PhotosPickerItem] = []
    @State private var selectedImages: [UIImage] = []
    @State private var isLoadingImages = false
    @State private var additionalPhotos: [PhotosPickerItem] = []
    @State private var description: String = ""
    @State private var tags: [String] = []
    @State private var currentTag: String = ""
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var showSuccess = false
    @State private var showBookingSelection = false
    
    private let maxPhotos = 20
    
    var body: some View {
        NavigationView {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Sélection de destination
                        destinationSection
                        
                        // Sélection de photos
                        photosSection
                        
                        // Description
                        descriptionSection
                        
                        // Tags
                        tagsSection
                        
                        // Bouton de partage
                        shareButton
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                }
            }
            .navigationTitle("Partager mon voyage")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        dismiss()
                    }) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(ThemeColors.accent())
                    }
                }
            }
            .alert("Erreur", isPresented: $showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(errorMessage)
            }
            .alert("Succès", isPresented: $showSuccess) {
                Button("OK") {
                    dismiss()
                }
            } message: {
                Text("Votre voyage a été partagé avec succès !")
            }
            .task {
                // Attendre un peu pour éviter les appels simultanés
                try? await Task.sleep(nanoseconds: 200_000_000) // 0.2 secondes
                await bookingViewModel.loadHistory()
            }
        }
    }
    
    private var destinationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Destination")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(ThemeColors.primaryText(colorScheme))
            
            Button(action: {
                showBookingSelection = true
            }) {
                HStack {
                    if let booking = selectedBooking {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(DestinationHelper.getFullDestinationName(from: booking.destination))
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                            if let confirmationNumber = booking.confirmationNumber as String? {
                                Text("Confirmation: \(confirmationNumber)")
                                    .font(.system(size: 13))
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            }
                        }
                        Spacer()
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(ThemeColors.accent())
                    } else {
                        Text("Choisir une destination")
                            .font(.system(size: 16))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    }
                }
                .padding(16)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(ThemeColors.surface(colorScheme))
                )
            }
            .buttonStyle(.plain)
            .sheet(isPresented: $showBookingSelection) {
                BookingSelectionView(bookings: bookingViewModel.bookings, selectedBooking: $selectedBooking)
            }
        }
    }
    
    private var photosSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Photos")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                Spacer()
                Text("\(selectedImages.count)/\(maxPhotos)")
                    .font(.system(size: 14))
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
            }
            
            if selectedImages.isEmpty {
                PhotosPicker(
                    selection: $selectedPhotos,
                    maxSelectionCount: maxPhotos,
                    matching: .images
                ) {
                    VStack(spacing: 12) {
                        Image(systemName: "photo.on.rectangle.angled")
                            .font(.system(size: 40))
                            .foregroundColor(ThemeColors.accent())
                        Text("Ajouter des photos")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(ThemeColors.accent())
                        Text("Maximum \(maxPhotos) photos")
                            .font(.system(size: 13))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(ThemeColors.surface(colorScheme))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(ThemeColors.accent().opacity(0.3), style: StrokeStyle(lineWidth: 2, dash: [8, 4]))
                            )
                    )
                }
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(Array(selectedImages.enumerated()), id: \.offset) { index, image in
                            ZStack(alignment: .topTrailing) {
                                Image(uiImage: image)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 100, height: 100)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                
                                Button(action: {
                                    withAnimation {
                                        // Supprimer l'image et l'item correspondant
                                        selectedImages.remove(at: index)
                                        // Synchroniser selectedPhotos avec selectedImages
                                        if selectedPhotos.count > index {
                                            selectedPhotos.remove(at: index)
                                        } else if selectedPhotos.count > selectedImages.count {
                                            // Si les arrays sont désynchronisés, réajuster
                                            selectedPhotos = Array(selectedPhotos.prefix(selectedImages.count))
                                        }
                                    }
                                }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundColor(.white)
                                        .background(Circle().fill(Color.black.opacity(0.6)))
                                }
                                .offset(x: 8, y: -8)
                            }
                        }
                        
                        if selectedImages.count < maxPhotos {
                            PhotosPicker(
                                selection: $additionalPhotos,
                                maxSelectionCount: maxPhotos - selectedImages.count,
                                matching: .images
                            ) {
                                VStack {
                                    Image(systemName: "plus")
                                        .font(.system(size: 24))
                                        .foregroundColor(ThemeColors.accent())
                                    Text("Ajouter")
                                        .font(.system(size: 12))
                                        .foregroundColor(ThemeColors.accent())
                                }
                                .frame(width: 100, height: 100)
                                .background(
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(ThemeColors.surface(colorScheme))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 8)
                                                .stroke(ThemeColors.accent().opacity(0.3), style: StrokeStyle(lineWidth: 1, dash: [4, 2]))
                                        )
                                )
                            }
                        }
                    }
                    .padding(.horizontal, 4)
                }
            }
        }
        .onChange(of: selectedPhotos) { oldItems, newItems in
            // Ne charger que si de nouveaux items ont été ajoutés et qu'on n'est pas déjà en train de charger
            if !isLoadingImages && newItems.count > oldItems.count {
                let newItemsOnly = Array(newItems.suffix(newItems.count - oldItems.count))
                Task {
                    await loadImages(from: newItemsOnly)
                }
            } else if newItems.count < oldItems.count {
                // Si des items ont été supprimés, synchroniser les images
                // On garde seulement les images correspondant aux items restants
                if selectedImages.count > newItems.count {
                    selectedImages = Array(selectedImages.prefix(newItems.count))
                }
            }
        }
        .onChange(of: additionalPhotos) { oldItems, newItems in
            // Gérer les photos supplémentaires ajoutées via le bouton "Ajouter"
            if !isLoadingImages && !newItems.isEmpty {
                Task {
                    await loadImages(from: newItems)
                    // Réinitialiser additionalPhotos après chargement
                    additionalPhotos = []
                }
            }
        }
    }
    
    private var descriptionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("share_trip_description_optional")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(ThemeColors.primaryText(colorScheme))
            
            TextEditor(text: $description)
                .frame(height: 120)
                .padding(12)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(ThemeColors.surface(colorScheme))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(ThemeColors.accent().opacity(0.2), lineWidth: 1)
                )
                .scrollContentBackground(.hidden)
        }
    }
    
    private var tagsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("share_trip_tags_optional")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(ThemeColors.primaryText(colorScheme))
            
            // Tags existants
            if !tags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(tags.enumerated()), id: \.offset) { index, tag in
                            HStack(spacing: 6) {
                                Text("#\(tag)")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(ThemeColors.accent())
                                
                                Button(action: {
                                    tags.remove(at: index)
                                }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.system(size: 14))
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(
                                Capsule()
                                    .fill(ThemeColors.accent().opacity(0.1))
                            )
                        }
                    }
                    .padding(.horizontal, 4)
                }
            }
            
            // Champ pour ajouter un tag
            HStack {
                TextField("Ajouter un tag", text: $currentTag)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(ThemeColors.surface(colorScheme))
                    )
                    .onSubmit {
                        addTag()
                    }
                
                Button(action: addTag) {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(ThemeColors.accent())
                }
                .disabled(currentTag.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
    }
    
    private var shareButton: some View {
        Button(action: {
            Task {
                await shareTrip()
            }
        }) {
            HStack {
                if journeyViewModel.isUploading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(0.9)
                    Text("Partage en cours...")
                        .font(.system(size: 16, weight: .semibold))
                } else {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Partager")
                        .font(.system(size: 16, weight: .semibold))
                }
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(canShare ? ThemeColors.accent() : ThemeColors.accent().opacity(0.5))
            )
        }
        .disabled(!canShare || journeyViewModel.isUploading)
        .padding(.top, 8)
    }
    
    private var canShare: Bool {
        selectedBooking != nil && !selectedImages.isEmpty
    }
    
    private func addTag() {
        let trimmedTag = currentTag.trimmingCharacters(in: .whitespaces)
        if !trimmedTag.isEmpty && !tags.contains(trimmedTag) {
            tags.append(trimmedTag)
            currentTag = ""
        }
    }
    
    private func loadImages(from items: [PhotosPickerItem]) async {
        guard !isLoadingImages else { return }
        isLoadingImages = true
        defer { isLoadingImages = false }
        
        // Limiter à maxPhotos
        let remainingSlots = maxPhotos - selectedImages.count
        guard remainingSlots > 0 else { return }
        
        var newImages: [UIImage] = []
        var validItems: [PhotosPickerItem] = []
        
        // Charger seulement le nombre d'images nécessaires
        let itemsToLoad = Array(items.prefix(remainingSlots))
        
        for item in itemsToLoad {
            if let data = try? await item.loadTransferable(type: Data.self),
               let image = UIImage(data: data) {
                // Vérifier si l'image n'existe pas déjà en comparant les données
                let imageData = image.jpegData(compressionQuality: 0.8)
                let isDuplicate = selectedImages.contains { existingImage in
                    if let existingData = existingImage.jpegData(compressionQuality: 0.8) {
                        return existingData == imageData
                    }
                    return false
                }
                
                if !isDuplicate {
                    newImages.append(image)
                    validItems.append(item)
                }
            }
        }
        
        // Ajouter les nouvelles images et items
        selectedImages.append(contentsOf: newImages)
        selectedPhotos.append(contentsOf: validItems)
    }
    
    private func shareTrip() async {
        guard let booking = selectedBooking else { return }
        
        do {
            // Utiliser JourneyViewModel pour créer le voyage (backend gère l'upload vers imgbb)
            let destination = booking.destination
            let tagsList = tags.isEmpty ? nil : tags
            
            let journey = try await journeyViewModel.createJourney(
                images: selectedImages,
                bookingId: booking.id,
                destination: destination,
                description: description.isEmpty ? nil : description,
                tags: tagsList,
                isPublic: true
            )
            
            print("✅ [ShareTripView] Journey created successfully: \(journey.id)")
            showSuccess = true
            
            // Notifier que la carte doit être rafraîchie
            NotificationCenter.default.post(name: NSNotification.Name("JourneyShared"), object: nil)
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}

@MainActor
struct BookingSelectionView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    let bookings: [Booking]
    @Binding var selectedBooking: Booking?
    
    var body: some View {
        NavigationView {
            List {
                ForEach(bookings) { booking in
                    Button(action: {
                        selectedBooking = booking
                        dismiss()
                    }) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(DestinationHelper.getFullDestinationName(from: booking.destination))
                                    .font(.system(size: 16, weight: .medium))
                                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                                Text("Confirmation: \(booking.confirmationNumber)")
                                    .font(.system(size: 13))
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            }
                            Spacer()
                            if selectedBooking?.id == booking.id {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(ThemeColors.accent())
                            }
                        }
                        .padding(.vertical, 4)
                    }
                    .buttonStyle(.plain)
                }
            }
            .navigationTitle("Choisir une destination")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Annuler") {
                        dismiss()
                    }
                }
            }
        }
    }
}

