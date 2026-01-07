//
//  CreateReelSheet.swift
//  WayFinder
//
//  Create Reel Sheet - Modal for creating new reels with image and music
//

import SwiftUI
import PhotosUI
import AVFoundation

struct CreateReelSheet: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    
    let onReelCreated: () -> Void
    let onDismiss: () -> Void
    
    @StateObject private var journeyViewModel = JourneyViewModel()
    @State private var selectedPhoto: PhotosPickerItem? = nil
    @State private var selectedImage: UIImage? = nil
    @State private var selectedMusicTrack: MusicTrack? = nil
    @State private var selectedStartTime: TimeInterval = 0
    @State private var selectedEndTime: TimeInterval = 30
    @State private var musicTracks: [MusicTrack] = []
    @State private var isLoadingTracks = false
    @State private var isCreating = false
    @State private var errorMessage: String? = nil
    @State private var showError = false
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Image Selection Section
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Select Image")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                            
                            if let image = selectedImage {
                                // Preview selected image
                                ZStack(alignment: .topTrailing) {
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(Color.clear)
                                        .frame(height: 300)
                                        .overlay(
                                            Image(uiImage: image)
                                                .resizable()
                                                .aspectRatio(contentMode: .fill)
                                                .frame(height: 300)
                                                .clipped()
                                        )
                                        .clipShape(RoundedRectangle(cornerRadius: 16))
                                    
                                    Button(action: {
                                        selectedImage = nil
                                        selectedPhoto = nil
                                    }) {
                                        Image(systemName: "xmark.circle.fill")
                                            .font(.system(size: 24))
                                            .foregroundColor(.white)
                                            .background(Circle().fill(Color.black.opacity(0.5)))
                                    }
                                    .padding(8)
                                }
                                .frame(maxWidth: .infinity)
                                .frame(height: 300)
                            } else {
                                // Image picker button
                                PhotosPicker(
                                    selection: $selectedPhoto,
                                    matching: .images,
                                    photoLibrary: .shared()
                                ) {
                                    VStack(spacing: 12) {
                                        Image(systemName: "photo.on.rectangle")
                                            .font(.system(size: 48))
                                            .foregroundColor(ThemeColors.accent())
                                        
                                        Text("Tap to select image")
                                            .font(.system(size: 16, weight: .medium))
                                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    }
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 300)
                                    .background(ThemeColors.surface(colorScheme))
                                    .clipShape(RoundedRectangle(cornerRadius: 16))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 16)
                                            .stroke(Color.gray.opacity(0.3), lineWidth: 2)
                                    )
                                }
                            }
                        }
                        
                        // Music Selection Section
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Select Music (Optional)")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(ThemeColors.primaryText(colorScheme))
                            
                            if isLoadingTracks {
                                ProgressView()
                                    .frame(maxWidth: .infinity)
                                    .padding()
                            } else if musicTracks.isEmpty {
                                Text("No music tracks available")
                                    .font(.system(size: 14))
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                    .frame(maxWidth: .infinity)
                                    .padding()
                            } else {
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 12) {
                                        ForEach(musicTracks) { track in
                                            MusicTrackCard(
                                                track: track,
                                                isSelected: selectedMusicTrack?.id == track.id,
                                                onSelect: {
                                                    if selectedMusicTrack?.id == track.id {
                                                        selectedMusicTrack = nil
                                                    } else {
                                                        selectedMusicTrack = track
                                                        selectedStartTime = 0
                                                        selectedEndTime = 30
                                                    }
                                                },
                                                startTime: $selectedStartTime,
                                                endTime: $selectedEndTime
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if let selectedTrack = selectedMusicTrack {
                                HStack {
                                    Text("Selected: \(selectedTrack.name)")
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                                    
                                    Spacer()
                                    
                                    Button(action: {
                                        selectedMusicTrack = nil
                                    }) {
                                        Text("Clear")
                                            .font(.system(size: 14))
                                            .foregroundColor(.red)
                                    }
                                }
                                .padding()
                                .background(ThemeColors.surface(colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                        
                        // Create Button
                        Button(action: {
                            Task {
                                await createReel()
                            }
                        }) {
                            HStack {
                                if isCreating {
                                    ProgressView()
                                        .tint(.white)
                                } else {
                                    Image(systemName: "plus.circle.fill")
                                    Text("Create Reel")
                                        .fontWeight(.bold)
                                }
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(
                                selectedImage != nil && !isCreating
                                    ? ThemeColors.accent()
                                    : Color.gray.opacity(0.3)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(selectedImage == nil || isCreating)
                        .padding(.bottom, 32)
                    }
                    .padding()
                }
            }
            .navigationTitle("Create Reel")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        onDismiss()
                    }
                }
            }
            .onChange(of: selectedPhoto) { newPhoto in
                Task {
                    if let data = try? await newPhoto?.loadTransferable(type: Data.self),
                       let uiImage = UIImage(data: data) {
                        selectedImage = uiImage
                    }
                }
            }
            .task {
                await loadMusicTracks()
            }
            .alert("Error", isPresented: $showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(errorMessage ?? "Unknown error")
            }
        }
    }
    
    private func loadMusicTracks() async {
        isLoadingTracks = true
        defer { isLoadingTracks = false }
        
        do {
            let response = try await APIService.shared.getMusicTracks()
            if response.success {
                musicTracks = response.tracks
                print("✅ Loaded \(response.tracks.count) music tracks")
                for track in response.tracks {
                    print("  - \(track.name): \(track.previewUrl ?? "no URL")")
                }
            } else {
                print("❌ Music tracks response not successful")
            }
        } catch {
            print("❌ Failed to load music tracks: \(error.localizedDescription)")
        }
    }
    
    private func createReel() async {
        guard let image = selectedImage else { return }
        
        isCreating = true
        errorMessage = nil
        
        defer { isCreating = false }
        
        do {
            // Convert UIImage to Data
            guard let imageData = image.jpegData(compressionQuality: 0.8) else {
                errorMessage = "Failed to process image"
                showError = true
                return
            }
            
            // Create description with music track info if selected
            var description: String? = nil
            if let music = selectedMusicTrack {
                let timeRange = String(format: "%.1f-%.1fs", selectedStartTime, selectedEndTime)
                description = "Music: \(music.name) (\(timeRange))"
            }
            
            // Convert Data to UIImage
            guard let uiImage = UIImage(data: imageData) else {
                errorMessage = "Failed to convert image data"
                showError = true
                return
            }
            
            // Create journey (reel) using JourneyViewModel
            let _ = try await journeyViewModel.createJourney(
                images: [uiImage],
                bookingId: nil,
                destination: nil,
                description: description,
                tags: selectedMusicTrack != nil ? ["reel", "music"] : ["reel"],
                isPublic: true
            )
            
            // Success - call onReelCreated
            onReelCreated()
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}

@MainActor
class MusicPlayerManager: ObservableObject {
    @Published var isPlaying = false
    @Published var currentTime: TimeInterval = 0
    @Published var duration: TimeInterval = 0
    @Published var startTime: TimeInterval = 0
    @Published var endTime: TimeInterval = 30
    @Published var isLoading = false
    @Published var loadError: String?
    
    private var audioPlayer: AVAudioPlayer?
    private var timer: Timer?
    
    func loadTrack(url: URL) async {
        isLoading = true
        loadError = nil
        
        do {
            stop()
            
            print("📥 Downloading audio from: \(url.absoluteString)")
            // Download audio data for remote URLs
            let (data, response) = try await URLSession.shared.data(from: url)
            
            guard let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode) else {
                throw NSError(domain: "MusicPlayer", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid response from server"])
            }
            
            print("✅ Downloaded \(data.count) bytes")
            
            // Create player from data
            audioPlayer = try AVAudioPlayer(data: data)
            audioPlayer?.delegate = AudioPlayerDelegate { [weak self] in
                self?.handlePlaybackEnd()
            }
            
            // Prepare to play
            let prepared = audioPlayer?.prepareToPlay() ?? false
            
            if prepared {
                duration = audioPlayer?.duration ?? 0
                if endTime > duration {
                    endTime = duration
                }
                print("✅ Successfully loaded track, duration: \(duration)s")
            } else {
                throw NSError(domain: "MusicPlayer", code: -2, userInfo: [NSLocalizedDescriptionKey: "Failed to prepare audio player"])
            }
        } catch {
            print("❌ Failed to load track: \(error.localizedDescription)")
            loadError = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func play() {
        guard let player = audioPlayer else {
            print("⚠️ Audio player is nil")
            return
        }
        
        // Ensure player is prepared
        if !player.prepareToPlay() {
            print("⚠️ Failed to prepare audio player")
            return
        }
        
        // Set start time and play
        player.currentTime = startTime
        let success = player.play()
        
        if success {
            isPlaying = true
            startTimer()
            print("▶️ Playback started at \(startTime)s, duration: \(duration)s")
        } else {
            print("❌ Failed to start playback - play() returned false")
            loadError = "Failed to start playback"
        }
    }
    
    func pause() {
        audioPlayer?.pause()
        isPlaying = false
        stopTimer()
    }
    
    func stop() {
        audioPlayer?.stop()
        audioPlayer = nil
        isPlaying = false
        currentTime = 0
        stopTimer()
    }
    
    func seek(to time: TimeInterval) {
        audioPlayer?.currentTime = time
        currentTime = time
    }
    
    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self = self, let player = self.audioPlayer else { return }
            self.currentTime = player.currentTime
            if self.currentTime >= self.endTime {
                self.pause()
                self.currentTime = self.startTime
            }
        }
    }
    
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
    
    private func handlePlaybackEnd() {
        pause()
        currentTime = startTime
    }
}

class AudioPlayerDelegate: NSObject, AVAudioPlayerDelegate {
    let onFinished: () -> Void
    
    init(onFinished: @escaping () -> Void) {
        self.onFinished = onFinished
    }
    
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        onFinished()
    }
}

struct MusicTrackCard: View {
    let track: MusicTrack
    let isSelected: Bool
    let onSelect: () -> Void
    @Binding var startTime: TimeInterval
    @Binding var endTime: TimeInterval
    @StateObject private var player = MusicPlayerManager()
    @State private var isExpanded = false
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Main card content
            Button(action: onSelect) {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 8) {
                        Image(systemName: "music.note")
                            .font(.system(size: 16))
                            .foregroundColor(isSelected ? ThemeColors.accent() : ThemeColors.secondaryText(colorScheme))
                        
                        Text(track.name)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(isSelected ? ThemeColors.accent() : ThemeColors.primaryText(colorScheme))
                            .lineLimit(1)
                        
                        Spacer()
                        
                        // Expand/Collapse button
                        Button(action: {
                            withAnimation {
                                isExpanded.toggle()
                                if !isExpanded {
                                    player.stop()
                                }
                            }
                        }) {
                            Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                                .font(.system(size: 12))
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        }
                        .buttonStyle(.plain)
                    }
                    
                    HStack {
                        Text(track.genre)
                            .font(.system(size: 12))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        
                        Spacer()
                        
                        Text(track.duration)
                            .font(.system(size: 12))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    }
                }
                .padding(12)
            }
            .buttonStyle(.plain)
            .frame(width: 160)
            .background(
                isSelected
                    ? ThemeColors.accent().opacity(0.1)
                    : ThemeColors.surface(colorScheme)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? ThemeColors.accent() : Color.clear, lineWidth: 2)
            )
            
            // Expanded preview controls
            if isExpanded && track.previewUrl != nil {
                VStack(spacing: 8) {
                    // Loading or error state
                    if player.isLoading {
                        HStack {
                            ProgressView()
                                .scaleEffect(0.8)
                            Text("Loading...")
                                .font(.system(size: 12))
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        }
                    } else if let error = player.loadError {
                        Text("Error: \(error)")
                            .font(.system(size: 11))
                            .foregroundColor(.red)
                            .lineLimit(2)
                    }
                    
                    // Play/Pause button
                    Button(action: togglePlayback) {
                        HStack {
                            Image(systemName: player.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                                .font(.system(size: 24))
                            Text(player.isPlaying ? "Pause" : "Preview")
                                .font(.system(size: 12, weight: .medium))
                        }
                        .foregroundColor(ThemeColors.accent())
                    }
                    .buttonStyle(.plain)
                    .disabled(player.isLoading || player.duration == 0)
                    .onAppear {
                        if isExpanded, let previewUrl = track.previewUrl, let url = URL(string: previewUrl) {
                            Task {
                                await player.loadTrack(url: url)
                                player.startTime = startTime
                                player.endTime = endTime
                            }
                        }
                    }
                    .onChange(of: isExpanded) { expanded in
                        if expanded, let previewUrl = track.previewUrl, let url = URL(string: previewUrl) {
                            Task {
                                await player.loadTrack(url: url)
                                player.startTime = startTime
                                player.endTime = endTime
                            }
                        } else if !expanded {
                            player.stop()
                        }
                    }
                    
                    // Time slider - only show if duration is valid
                    if player.duration > 0 {
                        VStack(spacing: 4) {
                            Slider(
                                value: $player.currentTime,
                                in: 0...player.duration,
                                onEditingChanged: { editing in
                                    if !editing {
                                        player.seek(to: player.currentTime)
                                    }
                                }
                            )
                            .accentColor(ThemeColors.accent())
                            
                            HStack {
                                Text(formatTime(player.currentTime))
                                    .font(.system(size: 10))
                                Spacer()
                                Text(formatTime(player.duration))
                                    .font(.system(size: 10))
                            }
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        }
                        
                        // Time range selection
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Select Section (seconds)")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                            
                            VStack(spacing: 8) {
                                HStack {
                                    Text("Start:")
                                        .font(.system(size: 11))
                                    let startRange = 0.0...min(max(endTime - 1, 0), player.duration)
                                    if startRange.lowerBound <= startRange.upperBound {
                                        Slider(value: Binding(
                                            get: { startTime },
                                            set: { newValue in
                                                startTime = newValue
                                                player.startTime = newValue
                                            }
                                        ), in: startRange)
                                            .accentColor(ThemeColors.accent())
                                    }
                                    Text("\(Int(startTime))s")
                                        .font(.system(size: 11))
                                        .frame(width: 30)
                                }
                                
                                HStack {
                                    Text("End:")
                                        .font(.system(size: 11))
                                    let endRange = max(startTime + 1, 0)...min(player.duration, 60)
                                    if endRange.lowerBound <= endRange.upperBound {
                                        Slider(value: Binding(
                                            get: { endTime },
                                            set: { newValue in
                                                endTime = newValue
                                                player.endTime = newValue
                                            }
                                        ), in: endRange)
                                            .accentColor(ThemeColors.accent())
                                    }
                                    Text("\(Int(endTime))s")
                                        .font(.system(size: 11))
                                        .frame(width: 30)
                                }
                            }
                        }
                        .padding(.top, 4)
                    }
                }
                .padding(8)
                .background(ThemeColors.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .onDisappear {
            player.stop()
        }
    }
    
    private func togglePlayback() {
        if player.isPlaying {
            player.pause()
        } else {
            // If duration is set, player is loaded, just play
            if player.duration > 0 {
                player.play()
            } else if let previewUrl = track.previewUrl, let url = URL(string: previewUrl) {
                // Load and play asynchronously
                Task {
                    await player.loadTrack(url: url)
                    // Play after loading completes
                    if player.duration > 0 {
                        player.play()
                    }
                }
            }
        }
    }
    
    private func formatTime(_ time: TimeInterval) -> String {
        let minutes = Int(time) / 60
        let seconds = Int(time) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}

#Preview {
    CreateReelSheet(
        onReelCreated: {},
        onDismiss: {}
    )
}

