//
//  AiTravelVideoGenerator.swift
//  WayFinder
//
//  AI Travel Video Generator component for creating travel videos from text prompts, images, and music
//

import SwiftUI
import PhotosUI

/// UI State for AI Travel Video generation
enum AiVideoState: Equatable {
    case idle
    case loading
    case generating(predictionId: String, progress: Int, enhancedPrompt: String)
    case completed(videoUrl: String, prompt: String)
    case error(message: String)
    case unavailable
}

/// Music Track Model
struct MusicTrack: Codable, Identifiable, Equatable {
    let id: String
    let name: String
    let genre: String
    let duration: String
    let previewUrl: String?
}

/// Travel Plan Suggestion Model
struct TravelPlanSuggestion: Codable, Identifiable, Equatable {
    let id: String
    let title: String
    let description: String
    let destinations: [String]
    let duration: String
    let activities: [String]
    let videoPrompt: String
}

/// ViewModel for AI Travel Video generation
@MainActor
class AiTravelVideoViewModel: ObservableObject {
    @Published var state: AiVideoState = .idle
    @Published var suggestions: [String] = []
    @Published var isServiceAvailable = false
    @Published var musicTracks: [MusicTrack] = []
    @Published var travelPlans: [TravelPlanSuggestion] = []
    @Published var selectedImages: [String] = []
    @Published var selectedMusicTrack: MusicTrack?
    @Published var isUploadingImage = false
    @Published var uploadError: String?
    
    private var pollingTask: Task<Void, Never>?
    
    init() {
        Task {
            await checkServiceStatus()
            await loadMusicTracks()
            await loadTravelPlans()
        }
    }
    
    /// Check if AI video generation service is available
    func checkServiceStatus() async {
        do {
            let response = try await APIService.shared.getAiVideoStatus()
            isServiceAvailable = response.available
            suggestions = response.suggestions
            
            if !response.available {
                state = .unavailable
            }
        } catch {
            isServiceAvailable = false
            state = .unavailable
        }
    }
    
    /// Load available music tracks
    func loadMusicTracks() async {
        do {
            let response = try await APIService.shared.getMusicTracks()
            if response.success {
                musicTracks = response.tracks
            }
        } catch {
            print("Failed to load music tracks: \(error)")
        }
    }
    
    /// Load travel plan suggestions
    func loadTravelPlans() async {
        do {
            let response = try await APIService.shared.getTravelPlans()
            if response.success {
                travelPlans = response.plans
            }
        } catch {
            print("Failed to load travel plans: \(error)")
        }
    }
    
    /// Add an image URL
    func addImage(_ url: String) {
        guard selectedImages.count < 20, !selectedImages.contains(url) else { return }
        selectedImages.append(url)
    }
    
    /// Remove an image URL
    func removeImage(_ url: String) {
        selectedImages.removeAll { $0 == url }
    }
    
    /// Clear all images
    func clearImages() {
        selectedImages.removeAll()
    }
    
    /// Select a music track
    func selectMusicTrack(_ track: MusicTrack?) {
        selectedMusicTrack = track
    }
    
    /// Upload image data to server
    func uploadImage(data: Data, fileName: String) async {
        isUploadingImage = true
        uploadError = nil
        
        do {
            let response = try await APIService.shared.uploadVideoImage(imageData: data, fileName: fileName)
            if response.success, let imageData = response.data {
                addImage(imageData.url)
            } else {
                uploadError = response.message ?? "Failed to upload image"
            }
        } catch {
            uploadError = error.localizedDescription
        }
        
        isUploadingImage = false
    }
    
    /// Clear upload error
    func clearUploadError() {
        uploadError = nil
    }
    
    /// Generate a travel video from a text prompt
    func generateVideo(prompt: String) async {
        guard !prompt.isEmpty else {
            state = .error(message: "Please enter a prompt")
            return
        }
        
        guard prompt.count >= 5 else {
            state = .error(message: "Prompt must be at least 5 characters")
            return
        }
        
        state = .loading
        
        do {
            let response: AiVideoGenerateResponse
            
            if !selectedImages.isEmpty || selectedMusicTrack != nil {
                response = try await APIService.shared.generateAiTravelVideoWithMedia(
                    prompt: prompt,
                    images: selectedImages,
                    musicTrackId: selectedMusicTrack?.id
                )
            } else {
                response = try await APIService.shared.generateAiTravelVideo(prompt: prompt)
            }
            
            if response.success, let data = response.data {
                state = .generating(
                    predictionId: data.predictionId,
                    progress: 0,
                    enhancedPrompt: data.enhancedPrompt
                )
                
                // Start polling for completion
                startPolling(predictionId: data.predictionId, originalPrompt: prompt)
            } else {
                state = .error(message: response.message ?? "Failed to start video generation")
            }
        } catch {
            state = .error(message: error.localizedDescription)
        }
    }
    
    /// Start polling for video generation completion
    private func startPolling(predictionId: String, originalPrompt: String) {
        pollingTask?.cancel()
        
        pollingTask = Task {
            var attempts = 0
            let maxAttempts = 120 // 6 minutes max
            
            while attempts < maxAttempts && !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 3_000_000_000) // 3 seconds
                
                do {
                    let statusResponse = try await APIService.shared.checkAiVideoStatus(predictionId: predictionId)
                    
                    if let data = statusResponse.data {
                        if data.isComplete, let videoUrl = data.videoUrl {
                            state = .completed(videoUrl: videoUrl, prompt: originalPrompt)
                            return
                        } else if data.isFailed {
                            state = .error(message: data.error ?? "Video generation failed")
                            return
                        } else {
                            // Still processing, update progress
                            if case .generating(let id, _, let enhanced) = state {
                                state = .generating(
                                    predictionId: id,
                                    progress: data.progress ?? (attempts * 100 / maxAttempts),
                                    enhancedPrompt: enhanced
                                )
                            }
                        }
                    }
                    
                    attempts += 1
                } catch {
                    attempts += 1
                }
            }
            
            // Timeout
            state = .error(message: "Video generation timed out. Please try again.")
        }
    }
    
    /// Cancel current video generation
    func cancelGeneration() {
        pollingTask?.cancel()
        
        if case .generating(let predictionId, _, _) = state {
            Task {
                try? await APIService.shared.cancelAiVideo(predictionId: predictionId)
            }
        }
        
        state = .idle
    }
    
    /// Reset state to idle
    func resetState() {
        pollingTask?.cancel()
        state = .idle
    }
}

/// AI Travel Video Generator Component
struct AiTravelVideoGenerator: View {
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = AiTravelVideoViewModel()
    @State private var promptText = ""
    @State private var isExpanded = false
    @State private var showMusicSelector = false
    @State private var showTravelPlans = false
    @State private var imageUrlInput = ""
    @State private var showUrlInput = false
    @State private var selectedPhotoItems: [PhotosPickerItem] = []
    @FocusState private var isTextFieldFocused: Bool
    
    var onVideoGenerated: ((String) -> Void)?
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            Button(action: { withAnimation { isExpanded.toggle() } }) {
                HStack(spacing: 12) {
                    // AI Icon with gradient
                    ZStack {
                        Circle()
                            .fill(
                                LinearGradient(
                                    colors: [Color(red: 0.4, green: 0.49, blue: 0.92), Color(red: 0.46, green: 0.29, blue: 0.64)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 44, height: 44)
                        
                        Image(systemName: "sparkles")
                            .font(.system(size: 20))
                            .foregroundColor(.white)
                    }
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Créer une vidéo IA")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                        
                        Text("Texte, photos & musique")
                            .font(.system(size: 12))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    }
                    
                    Spacer()
                    
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(ThemeColors.primary(colorScheme))
                }
            }
            .buttonStyle(.plain)
            .padding(16)
            
            // Expandable content
            if isExpanded {
                VStack(spacing: 12) {
                    // Service unavailable warning
                    if !viewModel.isServiceAvailable {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.orange)
                            Text("Service temporairement indisponible")
                                .font(.system(size: 13))
                                .foregroundColor(.orange)
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity)
                        .background(Color.orange.opacity(0.1))
                        .cornerRadius(12)
                    }
                    
                    // AI Travel Plans Section
                    if !viewModel.travelPlans.isEmpty {
                        HStack {
                            Text("Plans de voyage IA")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                            
                            Spacer()
                            
                            Button(showTravelPlans ? "Masquer" : "Voir tout") {
                                withAnimation { showTravelPlans.toggle() }
                            }
                            .font(.system(size: 12))
                            .foregroundColor(ThemeColors.primary(colorScheme))
                        }
                        
                        if showTravelPlans {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(viewModel.travelPlans) { plan in
                                        TravelPlanCard(plan: plan) {
                                            promptText = plan.videoPrompt
                                            showTravelPlans = false
                                        }
                                    }
                                }
                            }
                        } else {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(viewModel.travelPlans.prefix(3)) { plan in
                                        Button(action: { promptText = plan.videoPrompt }) {
                                            Text(String(plan.title.prefix(25)) + (plan.title.count > 25 ? "..." : ""))
                                                .font(.system(size: 11))
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(Color.green.opacity(0.1))
                                                .foregroundColor(.green)
                                                .cornerRadius(16)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Prompt input field
                    HStack {
                        Image(systemName: "pencil")
                            .foregroundColor(ThemeColors.primary(colorScheme))
                        
                        TextField("Décrivez votre vidéo de voyage...", text: $promptText, axis: .vertical)
                            .focused($isTextFieldFocused)
                            .lineLimit(1...3)
                            .disabled(!viewModel.isServiceAvailable || viewModel.state == .loading)
                        
                        if !promptText.isEmpty {
                            Button(action: { promptText = "" }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.gray)
                            }
                        }
                    }
                    .padding(12)
                    .background(ThemeColors.surface(colorScheme))
                    .cornerRadius(16)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(isTextFieldFocused ? ThemeColors.primary(colorScheme) : Color.gray.opacity(0.3), lineWidth: 1)
                    )
                    
                    // Image Upload Section
                    imageUploadSection
                    
                    // Music Selection Section
                    musicSelectionSection
                    
                    // Suggestions
                    if !viewModel.suggestions.isEmpty && promptText.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Suggestions:")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(viewModel.suggestions.prefix(5), id: \.self) { suggestion in
                                        Button(action: { promptText = suggestion }) {
                                            Text(String(suggestion.prefix(40)) + (suggestion.count > 40 ? "..." : ""))
                                                .font(.system(size: 11))
                                                .lineLimit(1)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(ThemeColors.primary(colorScheme).opacity(0.1))
                                                .foregroundColor(ThemeColors.primary(colorScheme))
                                                .cornerRadius(16)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // State display
                    stateDisplay
                    
                    // Generate button
                    if viewModel.state == .idle || isErrorState {
                        Button(action: {
                            isTextFieldFocused = false
                            Task {
                                await viewModel.generateVideo(prompt: promptText)
                            }
                        }) {
                            HStack {
                                Image(systemName: "sparkles")
                                Text("Générer la vidéo")
                                    .fontWeight(.bold)
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(
                                promptText.isEmpty || !viewModel.isServiceAvailable
                                    ? Color.gray.opacity(0.3)
                                    : ThemeColors.primary(colorScheme)
                            )
                            .cornerRadius(12)
                        }
                        .disabled(promptText.isEmpty || !viewModel.isServiceAvailable)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            } else {
                // Collapsed hint
                Text("Appuyez pour créer des vidéos de voyage avec l'IA")
                    .font(.system(size: 12))
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)
                    .onTapGesture {
                        withAnimation { isExpanded = true }
                    }
            }
        }
        .background(ThemeColors.surface(colorScheme))
        .cornerRadius(20)
        .shadow(color: Color.black.opacity(0.1), radius: 8, y: 4)
        .padding(.horizontal, 16)
    }
    
    // MARK: - Image Upload Section
    
    private var imageUploadSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "photo.on.rectangle.angled")
                    .foregroundColor(ThemeColors.primary(colorScheme))
                Text("Photos (\(viewModel.selectedImages.count)/20)")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                
                Spacer()
                
                // Photos Picker button
                PhotosPicker(
                    selection: $selectedPhotoItems,
                    maxSelectionCount: 20 - viewModel.selectedImages.count,
                    matching: .images
                ) {
                    HStack(spacing: 4) {
                        if viewModel.isUploadingImage {
                            ProgressView()
                                .scaleEffect(0.7)
                            Text("Envoi...")
                                .font(.system(size: 12))
                        } else {
                            Image(systemName: "photo.badge.plus")
                                .font(.system(size: 14))
                            Text("Galerie")
                                .font(.system(size: 12))
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(ThemeColors.primary(colorScheme).opacity(0.1))
                    .foregroundColor(ThemeColors.primary(colorScheme))
                    .cornerRadius(20)
                }
                .disabled(viewModel.isUploadingImage || viewModel.selectedImages.count >= 20)
                .onChange(of: selectedPhotoItems) { items in
                    Task {
                        for item in items {
                            if let data = try? await item.loadTransferable(type: Data.self) {
                                let fileName = "image_\(Date().timeIntervalSince1970).jpg"
                                await viewModel.uploadImage(data: data, fileName: fileName)
                            }
                        }
                        selectedPhotoItems.removeAll()
                    }
                }
            }
            
            // Upload error
            if let error = viewModel.uploadError {
                HStack {
                    Text(error)
                        .font(.system(size: 11))
                        .foregroundColor(.red)
                    Spacer()
                    Button(action: { viewModel.clearUploadError() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 14))
                            .foregroundColor(.red)
                    }
                }
                .padding(8)
                .background(Color.red.opacity(0.1))
                .cornerRadius(8)
            }
            
            if !viewModel.selectedImages.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(viewModel.selectedImages, id: \.self) { imageUrl in
                            ZStack(alignment: .topTrailing) {
                                AsyncImage(url: URL(string: imageUrl)) { image in
                                    image
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Color.gray.opacity(0.3)
                                }
                                .frame(width: 60, height: 60)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                
                                Button(action: { viewModel.removeImage(imageUrl) }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.system(size: 16))
                                        .foregroundColor(.white)
                                        .background(Circle().fill(Color.black.opacity(0.5)))
                                }
                                .offset(x: 4, y: -4)
                            }
                        }
                    }
                }
            }
            
            // Optional URL input
            Button(action: { showUrlInput.toggle() }) {
                Text(showUrlInput ? "Masquer URL" : "Ou ajouter via URL")
                    .font(.system(size: 11))
                    .foregroundColor(ThemeColors.primary(colorScheme))
            }
            
            if showUrlInput {
                HStack(spacing: 8) {
                    TextField("https://...", text: $imageUrlInput)
                        .font(.system(size: 12))
                        .textFieldStyle(.roundedBorder)
                        .disabled(viewModel.selectedImages.count >= 20)
                    
                    Button(action: {
                        if imageUrlInput.hasPrefix("http") {
                            viewModel.addImage(imageUrlInput)
                            imageUrlInput = ""
                        }
                    }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 24))
                            .foregroundColor(
                                imageUrlInput.hasPrefix("http") && viewModel.selectedImages.count < 20
                                    ? ThemeColors.primary(colorScheme)
                                    : Color.gray
                            )
                    }
                    .disabled(!imageUrlInput.hasPrefix("http") || viewModel.selectedImages.count >= 20)
                }
            }
        }
    }
    
    // MARK: - Music Selection Section
    
    private var musicSelectionSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button(action: { withAnimation { showMusicSelector.toggle() } }) {
                HStack {
                    Image(systemName: "music.note")
                        .foregroundColor(ThemeColors.primary(colorScheme))
                    Text("Musique de fond")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                    
                    Spacer()
                    
                    if let track = viewModel.selectedMusicTrack {
                        Button(action: { viewModel.selectMusicTrack(nil) }) {
                            HStack(spacing: 4) {
                                Text(track.name)
                                    .font(.system(size: 11))
                                Image(systemName: "xmark.circle.fill")
                                    .font(.system(size: 12))
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(ThemeColors.primary(colorScheme).opacity(0.1))
                            .foregroundColor(ThemeColors.primary(colorScheme))
                            .cornerRadius(12)
                        }
                    } else {
                        Image(systemName: showMusicSelector ? "chevron.up" : "chevron.down")
                            .font(.system(size: 12))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    }
                }
            }
            .buttonStyle(.plain)
            
            if showMusicSelector && !viewModel.musicTracks.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(viewModel.musicTracks) { track in
                            MusicTrackCard(
                                track: track,
                                isSelected: viewModel.selectedMusicTrack?.id == track.id,
                                onSelect: {
                                    viewModel.selectMusicTrack(
                                        viewModel.selectedMusicTrack?.id == track.id ? nil : track
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - State Display
    
    @ViewBuilder
    private var stateDisplay: some View {
        switch viewModel.state {
        case .loading:
            VStack(spacing: 8) {
                ProgressView()
                Text("Démarrage de la génération...")
                    .font(.system(size: 13))
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
            }
            .frame(maxWidth: .infinity)
            .padding()
            
        case .generating(_, let progress, let enhancedPrompt):
            VStack(spacing: 8) {
                ProgressView(value: Double(progress) / 100)
                    .tint(ThemeColors.primary(colorScheme))
                
                Text("Génération en cours \(progress)%")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(ThemeColors.primary(colorScheme))
                
                if !enhancedPrompt.isEmpty {
                    Text("\"\(String(enhancedPrompt.prefix(80)))...\"")
                        .font(.system(size: 11))
                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                }
                
                Button("Annuler") {
                    viewModel.cancelGeneration()
                }
                .font(.system(size: 14))
                .foregroundColor(ThemeColors.primary(colorScheme))
            }
            .frame(maxWidth: .infinity)
            .padding()
            
        case .completed(let videoUrl, _):
            VStack(spacing: 8) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 32))
                    .foregroundColor(.green)
                
                Text("Vidéo générée avec succès!")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.green)
                
                HStack(spacing: 12) {
                    Button(action: {
                        if let url = URL(string: videoUrl) {
                            UIApplication.shared.open(url)
                        }
                        onVideoGenerated?(videoUrl)
                    }) {
                        HStack {
                            Image(systemName: "play.fill")
                            Text("Voir")
                        }
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(ThemeColors.primary(colorScheme))
                        .cornerRadius(12)
                    }
                    
                    Button(action: {
                        viewModel.resetState()
                        viewModel.clearImages()
                        viewModel.selectMusicTrack(nil)
                        promptText = ""
                    }) {
                        Text("Nouveau")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(ThemeColors.primary(colorScheme))
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(ThemeColors.primary(colorScheme), lineWidth: 1)
                            )
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.green.opacity(0.1))
            .cornerRadius(12)
            
        case .error(let message):
            VStack(spacing: 8) {
                Image(systemName: "exclamationmark.circle.fill")
                    .font(.system(size: 24))
                    .foregroundColor(.red)
                
                Text(message)
                    .font(.system(size: 13))
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                
                Button("Réessayer") {
                    viewModel.resetState()
                }
                .font(.system(size: 14))
                .foregroundColor(ThemeColors.primary(colorScheme))
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.red.opacity(0.1))
            .cornerRadius(12)
            
        case .idle, .unavailable:
            EmptyView()
        }
    }
    
    private var isErrorState: Bool {
        if case .error = viewModel.state {
            return true
        }
        return false
    }
}

// MARK: - Music Track Card

struct MusicTrackCard: View {
    let track: MusicTrack
    let isSelected: Bool
    let onSelect: () -> Void
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        Button(action: onSelect) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    Image(systemName: "music.note")
                        .font(.system(size: 12))
                    Text(track.name)
                        .font(.system(size: 12, weight: .medium))
                        .lineLimit(1)
                }
                .foregroundColor(isSelected ? ThemeColors.primary(colorScheme) : ThemeColors.primaryText(colorScheme))
                
                HStack {
                    Text(track.genre)
                        .font(.system(size: 10))
                    Spacer()
                    Text(track.duration)
                        .font(.system(size: 10))
                }
                .foregroundColor(ThemeColors.secondaryText(colorScheme))
            }
            .padding(12)
            .frame(width: 140)
            .background(isSelected ? ThemeColors.primary(colorScheme).opacity(0.1) : ThemeColors.surface(colorScheme))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? ThemeColors.primary(colorScheme) : Color.clear, lineWidth: 2)
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Travel Plan Card

struct TravelPlanCard: View {
    let plan: TravelPlanSuggestion
    let onSelect: () -> Void
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(plan.title)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(ThemeColors.primaryText(colorScheme))
                .lineLimit(1)
            
            Text(plan.description)
                .font(.system(size: 11))
                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                .lineLimit(2)
            
            HStack(spacing: 4) {
                Image(systemName: "clock")
                    .font(.system(size: 10))
                Text(plan.duration)
                    .font(.system(size: 10))
            }
            .foregroundColor(ThemeColors.primary(colorScheme))
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 4) {
                    ForEach(plan.destinations.prefix(3), id: \.self) { dest in
                        Text(dest)
                            .font(.system(size: 9))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(ThemeColors.surface(colorScheme))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            .cornerRadius(4)
                    }
                }
            }
            
            Button(action: onSelect) {
                Text("Utiliser ce plan")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(ThemeColors.primary(colorScheme))
                    .cornerRadius(8)
            }
        }
        .padding(12)
        .frame(width: 200)
        .background(ThemeColors.secondaryContainer(colorScheme))
        .cornerRadius(16)
    }
}

// MARK: - API Service Extensions

extension APIService {
    func getAiVideoStatus() async throws -> AiVideoStatusResponse {
        let request = DefaultRequest(method: "GET", path: "/ai-video/status")
        return try await self.request(request, decodeTo: AiVideoStatusResponse.self)
    }
    
    func generateAiTravelVideo(prompt: String) async throws -> AiVideoGenerateResponse {
        let request = DefaultRequest(
            method: "POST",
            path: "/ai-video/generate",
            body: ["prompt": prompt]
        )
        return try await self.request(request, decodeTo: AiVideoGenerateResponse.self)
    }
    
    func generateAiTravelVideoWithMedia(prompt: String, images: [String], musicTrackId: String?) async throws -> AiVideoGenerateResponse {
        var body: [String: Any] = ["prompt": prompt, "images": images]
        if let musicId = musicTrackId {
            body["musicTrackId"] = musicId
        }
        let request = DefaultRequest(
            method: "POST",
            path: "/ai-video/generate-with-media",
            body: body
        )
        return try await self.request(request, decodeTo: AiVideoGenerateResponse.self)
    }
    
    func checkAiVideoStatus(predictionId: String) async throws -> AiVideoCheckStatusResponse {
        let request = DefaultRequest(method: "GET", path: "/ai-video/status/\(predictionId)")
        return try await self.request(request, decodeTo: AiVideoCheckStatusResponse.self)
    }
    
    func cancelAiVideo(predictionId: String) async throws {
        let request = DefaultRequest(method: "POST", path: "/ai-video/cancel/\(predictionId)")
        _ = try await self.request(request, decodeTo: GenericApiResponse.self)
    }
    
    func getMusicTracks() async throws -> MusicTracksResponse {
        let request = DefaultRequest(method: "GET", path: "/ai-video/music-tracks")
        return try await self.request(request, decodeTo: MusicTracksResponse.self)
    }
    
    func getTravelPlans() async throws -> TravelPlansResponse {
        let request = DefaultRequest(method: "GET", path: "/ai-video/travel-plans")
        return try await self.request(request, decodeTo: TravelPlansResponse.self)
    }
    
    func uploadVideoImage(imageData: Data, fileName: String) async throws -> ImageUploadResponse {
        let boundary = UUID().uuidString
        var body = Data()
        
        // Add image data
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"image\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n".data(using: .utf8)!)
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        
        let request = MultipartRequest(
            method: "POST",
            path: "/ai-video/upload-image",
            body: body,
            boundary: boundary
        )
        return try await self.request(request, decodeTo: ImageUploadResponse.self)
    }
}

// MARK: - Response Models

struct AiVideoStatusResponse: Codable {
    let available: Bool
    let suggestions: [String]
    let message: String?
}

struct AiVideoGenerateResponse: Codable {
    let success: Bool
    let message: String?
    let data: AiVideoGenerateData?
}

struct AiVideoGenerateData: Codable {
    let predictionId: String
    let status: String
    let originalPrompt: String
    let enhancedPrompt: String
    let estimatedTime: String?
}

struct AiVideoCheckStatusResponse: Codable {
    let success: Bool
    let data: AiVideoStatusData?
}

struct AiVideoStatusData: Codable {
    let predictionId: String
    let status: String
    let videoUrl: String?
    let progress: Int?
    let error: String?
    let isComplete: Bool
    let isFailed: Bool
}

struct GenericApiResponse: Codable {
    let success: Bool
    let message: String?
}

struct MusicTracksResponse: Codable {
    let success: Bool
    let tracks: [MusicTrack]
}

struct TravelPlansResponse: Codable {
    let success: Bool
    let plans: [TravelPlanSuggestion]
}

struct ImageUploadResponse: Codable {
    let success: Bool
    let message: String?
    let data: ImageUploadData?
}

struct ImageUploadData: Codable {
    let url: String
    let originalName: String
    let size: Int?
}

/// Multipart Request for file uploads
struct MultipartRequest: APIRequest {
    let method: String
    let path: String
    let body: Data
    let boundary: String
    
    var headers: [String: String]? {
        ["Content-Type": "multipart/form-data; boundary=\(boundary)"]
    }
    
    func encode() throws -> Data? {
        return body
    }
}

#Preview {
    AiTravelVideoGenerator()
}
