import SwiftUI
import PhotosUI
import UIKit

struct OutfitUploadView: View {
    let bookingId: String
    @StateObject private var viewModel = OutfitWeatherViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @State private var selectedImage: UIImage?
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var showCamera = false
    @State private var showImagePicker = false
    @State private var resultOutfitId: String?
    @State private var navigateToResult = false
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                HStack {
                    Button(action: {
                        dismiss()
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                    .buttonStyle(.plain)
                    
                    Text("outfit_check_my_outfit")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    Spacer()
                    
                    Button(action: {
                        dismiss()
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24, weight: .medium))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 8)
                
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 24) {
                        // Instructions Card
                        VStack(alignment: .leading, spacing: 12) {
                            Text("outfit_take_photo_title")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                            
                            Text("outfit_take_photo_description")
                                .font(.system(size: 14))
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(20)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(ThemeColors.accent().opacity(0.1))
                        )
                        .padding(.horizontal, 24)
                        .padding(.top, 16)
                        
                        // Image Preview or Upload Options
                        if let image = selectedImage {
                            // Image Preview
                            VStack(spacing: 16) {
                                Image(uiImage: image)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(height: 400)
                                    .clipShape(RoundedRectangle(cornerRadius: 16))
                                    .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 8, x: 0, y: 4)
                                
                                // Analyze Button
                                Button(action: {
                                    Task {
                                        // Optimiser l'image avant l'upload : redimensionner et compresser agressivement
                                        // Réduire à 800px max pour accélérer l'upload et l'analyse
                                        let optimizedImage = image.resized(to: 800) // Max 800px de largeur/hauteur
                                        guard let imageData = optimizedImage.jpegData(compressionQuality: 0.5) else { return }
                                        
                                        print("📊 [OutfitUploadView] Original size: \(image.size), Optimized size: \(optimizedImage.size), Data size: \(imageData.count) bytes")
                                        
                                        await viewModel.uploadOutfit(imageData: imageData, bookingId: bookingId)
                                        
                                        if case .success(let outfit) = viewModel.uiState {
                                            resultOutfitId = outfit.id
                                            navigateToResult = true
                                        }
                                    }
                                }) {
                                    if viewModel.isLoading {
                                        VStack(spacing: 8) {
                                            HStack(spacing: 8) {
                                                ProgressView()
                                                    .tint(.white)
                                                Text("outfit_analyzing")
                                                    .font(.system(size: 18, weight: .bold))
                                                    .foregroundColor(.white)
                                            }
                                            Text("outfit_analyzing_wait")
                                                .font(.system(size: 12))
                                                .foregroundColor(.white.opacity(0.8))
                                        }
                                    } else {
                                        Text("outfit_analyze")
                                            .font(.system(size: 18, weight: .bold))
                                            .foregroundColor(.white)
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(ThemeColors.accent())
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                                .disabled(viewModel.isLoading || selectedImage == nil)
                                
                                // Change Photo Button
                                Button(action: {
                                    selectedImage = nil
                                    selectedPhoto = nil
                                }) {
                                    Text("outfit_change_photo")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(ThemeColors.accent())
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 14)
                                        .background(ThemeColors.surface(colorScheme))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 16)
                                                .stroke(ThemeColors.accent(), lineWidth: 1)
                                        )
                                }
                            }
                            .padding(.horizontal, 24)
                        } else {
                            // Upload Options
                            HStack(spacing: 16) {
                                // Camera Button
                                Button(action: {
                                    showCamera = true
                                }) {
                                    VStack(spacing: 12) {
                                        Image(systemName: "camera.fill")
                                            .font(.system(size: 64))
                                            .foregroundColor(ThemeColors.accent())
                                        
                                        Text("outfit_camera")
                                            .font(.system(size: 16, weight: .medium))
                                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    }
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 200)
                                    .background(
                                        RoundedRectangle(cornerRadius: 16)
                                            .fill(ThemeColors.surface(colorScheme))
                                    )
                                }
                                .buttonStyle(.plain)
                                
                                // Gallery Button
                                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                                    VStack(spacing: 12) {
                                        Image(systemName: "photo.on.rectangle")
                                            .font(.system(size: 64))
                                            .foregroundColor(ThemeColors.accent())
                                        
                                        Text("outfit_gallery")
                                            .font(.system(size: 16, weight: .medium))
                                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    }
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 200)
                                    .background(
                                        RoundedRectangle(cornerRadius: 16)
                                            .fill(ThemeColors.surface(colorScheme))
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                            .padding(.horizontal, 24)
                        }
                        
                        // Error Message
                        if let error = viewModel.errorMessage {
                            VStack(spacing: 8) {
                                Text("outfit_error")
                                    .font(.headline)
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                Text(error)
                                    .font(.subheadline)
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                    .multilineTextAlignment(.center)
                            }
                            .padding(16)
                            .background(
                                RoundedRectangle(cornerRadius: 16)
                                    .fill(Color.red.opacity(0.1))
                            )
                            .padding(.horizontal, 24)
                        }
                        
                        // Outfit History Section
                        if !viewModel.outfitHistory.isEmpty {
                            OutfitHistorySection(
                                outfits: viewModel.outfitHistory,
                                colorScheme: colorScheme,
                                onOutfitTap: { outfitId in
                                    resultOutfitId = outfitId
                                    navigateToResult = true
                                },
                                onOutfitDelete: { outfitId in
                                    Task {
                                        await viewModel.deleteOutfit(outfitId: outfitId, bookingId: bookingId)
                                    }
                                }
                            )
                            .padding(.top, 8)
                        }
                    }
                    .padding(.bottom, 32)
                }
            }
        }
        .navigationBarHidden(true)
        .sheet(isPresented: $showCamera) {
            ImagePicker(sourceType: .camera, selectedImage: $selectedImage)
        }
        .onChange(of: selectedPhoto) { _, newValue in
            Task {
                if let newValue = newValue {
                    if let data = try? await newValue.loadTransferable(type: Data.self),
                       let image = UIImage(data: data) {
                        selectedImage = image
                    }
                }
            }
        }
        .navigationDestination(isPresented: $navigateToResult) {
            if let outfitId = resultOutfitId {
                OutfitResultView(outfitId: outfitId)
            }
        }
        .onAppear {
            Task {
                await viewModel.loadOutfitHistory(bookingId: bookingId)
            }
        }
    }
}

// MARK: - Outfit History Section
struct OutfitHistorySection: View {
    let outfits: [Outfit]
    let colorScheme: ColorScheme
    let onOutfitTap: (String) -> Void
    let onOutfitDelete: (String) -> Void
    
    private var groupedOutfits: [String: [Outfit]] {
        Dictionary(grouping: outfits) { outfit in
            if let outfitDate = outfit.outfitDate {
                return outfitDate
            } else if let createdAt = outfit.createdAt {
                // Extraire la date depuis createdAt (format ISO8601)
                let formatter = ISO8601DateFormatter()
                formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                if let date = formatter.date(from: createdAt) {
                    let dateFormatter = DateFormatter()
                    dateFormatter.dateFormat = "yyyy-MM-dd"
                    return dateFormatter.string(from: date)
                }
            }
            return "Unknown"
        }
    }
    
    private var sortedDates: [String] {
        groupedOutfits.keys.sorted(by: >)
    }
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        if let date = formatter.date(from: dateString) {
            formatter.dateFormat = "EEEE, MMM d"
            formatter.locale = Locale.current
            return formatter.string(from: date)
        }
        return dateString
    }
    
    private func getScoreColor(score: Int) -> Color {
        if score >= 80 {
            return Color(red: 0.298, green: 0.686, blue: 0.314) // Green
        } else if score >= 60 {
            return Color(red: 1.0, green: 0.596, blue: 0.0) // Orange
        } else {
            return Color(red: 0.956, green: 0.262, blue: 0.212) // Red
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            // Section Header
            HStack(spacing: 8) {
                Image(systemName: "calendar.badge.clock")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(
                        LinearGradient(
                            gradient: Gradient(colors: [Color.purple, Color.blue]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                
                Text("outfit_history_title")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                
                Spacer()
                
                Text("\(outfits.count)")
                    .font(.system(size: 16, weight: .semibold, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(
                        Capsule()
                            .fill(ThemeColors.accent())
                    )
            }
            .padding(.horizontal, 24)
            
            // Outfits by Date
            ForEach(sortedDates, id: \.self) { date in
                if let dayOutfits = groupedOutfits[date] {
                    VStack(alignment: .leading, spacing: 12) {
                        // Date Header
                        HStack {
                            Text(formatDate(date))
                                .font(.system(size: 18, weight: .bold, design: .rounded))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            Spacer()
                            
                            Text("\(dayOutfits.count) \(dayOutfits.count > 1 ? String(localized: "outfit_outfits") : String(localized: "outfit_outfit"))")
                                .font(.system(size: 14, weight: .medium, design: .rounded))
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        }
                        .padding(.horizontal, 24)
                        
                        // Outfit Cards for this day
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 16) {
                                ForEach(dayOutfits) { outfit in
                                    OutfitHistoryCard(
                                        outfit: outfit,
                                        colorScheme: colorScheme,
                                        scoreColor: getScoreColor(score: outfit.recommendation?.score ?? 0),
                                        onTap: {
                                            onOutfitTap(outfit.id)
                                        },
                                        onDelete: {
                                            onOutfitDelete(outfit.id)
                                        }
                                    )
                                }
                            }
                            .padding(.horizontal, 24)
                        }
                    }
                }
            }
        }
        .padding(.vertical, 24)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(ThemeColors.surface(colorScheme))
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.1), radius: 15, x: 0, y: -5)
        )
        .padding(.top, 16)
    }
}

// MARK: - Outfit History Card
struct OutfitHistoryCard: View {
    let outfit: Outfit
    let colorScheme: ColorScheme
    let scoreColor: Color
    let onTap: () -> Void
    let onDelete: () -> Void
    
    @State private var showDeleteConfirmation = false
    
    var body: some View {
        ZStack(alignment: .topTrailing) {
            Button(action: onTap) {
            VStack(spacing: 0) {
                // Image
                AsyncImage(url: URL(string: outfit.imageUrl)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(width: 140, height: 140)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    case .failure, .empty:
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(ThemeColors.surface(colorScheme).opacity(0.5))
                            .frame(width: 140, height: 140)
                            .overlay(
                                Image(systemName: "photo")
                                    .font(.system(size: 32, weight: .light))
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme).opacity(0.5))
                            )
                    @unknown default:
                        EmptyView()
                    }
                }
                .overlay(
                    // Score Badge (left) and Delete Button (right)
                    VStack {
                        HStack {
                            // Score Badge - Left
                            ZStack {
                                Circle()
                                    .fill(
                                        LinearGradient(
                                            gradient: Gradient(colors: [
                                                scoreColor,
                                                scoreColor.opacity(0.8)
                                            ]),
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(width: 44, height: 44)
                                    .shadow(color: scoreColor.opacity(0.4), radius: 8, x: 0, y: 4)
                                
                                Text("\(outfit.recommendation?.score ?? 0)")
                                    .font(.system(size: 16, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                            }
                            .padding(.leading, 8)
                            .padding(.top, 8)
                            
                            Spacer()
                            
                            // Delete Button - Right (X gris très clair) - Coin supérieur droit
                            Button(action: {
                                showDeleteConfirmation = true
                            }) {
                                Image(systemName: "xmark")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme).opacity(0.5))
                                    .frame(width: 24, height: 24)
                                    .background(
                                        Circle()
                                            .fill(ThemeColors.surface(colorScheme).opacity(0.8))
                                    )
                            }
                            .buttonStyle(.plain)
                            .padding(.trailing, 4)
                            .padding(.top, 4)
                        }
                        Spacer()
                    }
                )
                
                // Info Section
                VStack(spacing: 6) {
                    // Temperature
                    if let weather = outfit.weatherData {
                        HStack(spacing: 4) {
                            Image(systemName: "thermometer")
                                .font(.system(size: 10, weight: .medium))
                                .foregroundColor(Color.orange)
                            Text("\(weather.temperature)°C")
                                .font(.system(size: 12, weight: .semibold, design: .rounded))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        }
                    }
                    
                    // Status Badge
                    HStack(spacing: 4) {
                        Image(systemName: outfit.recommendation?.isSuitable == true ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(outfit.recommendation?.isSuitable == true ? Color.green : Color.orange)
                        
                        Text(outfit.recommendation?.isSuitable == true ? "OK" : "À améliorer")
                            .font(.system(size: 11, weight: .medium, design: .rounded))
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(ThemeColors.surface(colorScheme).opacity(0.5))
                )
            }
            .frame(width: 140)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(ThemeColors.surface(colorScheme))
                    .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.1), radius: 10, x: 0, y: 5)
            )
        }
        .buttonStyle(.plain)
        .alert(String(localized: "outfit_delete_title"), isPresented: $showDeleteConfirmation) {
            Button(String(localized: "outfit_delete_cancel"), role: .cancel) { }
            Button(String(localized: "outfit_delete_confirm"), role: .destructive) {
                onDelete()
            }
        } message: {
            Text(String(localized: "outfit_delete_message"))
        }
        }
    }
}

// Image Picker for Camera
struct ImagePicker: UIViewControllerRepresentable {
    let sourceType: UIImagePickerController.SourceType
    @Binding var selectedImage: UIImage?
    @Environment(\.dismiss) private var dismiss
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = sourceType
        picker.delegate = context.coordinator
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: ImagePicker
        
        init(_ parent: ImagePicker) {
            self.parent = parent
        }
        
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.selectedImage = image
            }
            parent.dismiss()
        }
        
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}

// Extension pour redimensionner les images
extension UIImage {
    func resized(to size: CGSize) -> UIImage {
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { _ in
            self.draw(in: CGRect(origin: .zero, size: size))
        }
    }
    
    func resized(to maxDimension: CGFloat) -> UIImage {
        let ratio = min(maxDimension / size.width, maxDimension / size.height)
        let newSize = CGSize(width: size.width * ratio, height: size.height * ratio)
        return resized(to: newSize)
    }
}

