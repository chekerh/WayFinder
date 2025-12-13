//
//  AiTravelVideoGenerator.swift
//  WayFinder
//
//  AI Travel Video Generator component for creating travel videos from text prompts
//

import SwiftUI

/// UI State for AI Travel Video generation
enum AiVideoState: Equatable {
    case idle
    case loading
    case generating(predictionId: String, progress: Int, enhancedPrompt: String)
    case completed(videoUrl: String, prompt: String)
    case error(message: String)
    case unavailable
}

/// ViewModel for AI Travel Video generation
@MainActor
class AiTravelVideoViewModel: ObservableObject {
    @Published var state: AiVideoState = .idle
    @Published var suggestions: [String] = []
    @Published var isServiceAvailable = false
    
    private var pollingTask: Task<Void, Never>?
    
    init() {
        Task {
            await checkServiceStatus()
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
            let response = try await APIService.shared.generateAiTravelVideo(prompt: prompt)
            
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
                        
                        Text("Décrivez votre voyage de rêve")
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
                    
                    // Prompt input field
                    HStack {
                        Image(systemName: "pencil")
                            .foregroundColor(ThemeColors.primary(colorScheme))
                        
                        TextField("Ex: Coucher de soleil sur une plage tropicale...", text: $promptText, axis: .vertical)
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
                                            Text(suggestion.prefix(40) + (suggestion.count > 40 ? "..." : ""))
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
                    
                    // Generate button
                    if viewModel.state == .idle || viewModel.state == .error(message: "") || (viewModel.state == .error(message: "") == false && viewModel.state != .loading && viewModel.state != .generating(predictionId: "", progress: 0, enhancedPrompt: "") && viewModel.state != .completed(videoUrl: "", prompt: "")) {
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
    
    func checkAiVideoStatus(predictionId: String) async throws -> AiVideoCheckStatusResponse {
        let request = DefaultRequest(method: "GET", path: "/ai-video/status/\(predictionId)")
        return try await self.request(request, decodeTo: AiVideoCheckStatusResponse.self)
    }
    
    func cancelAiVideo(predictionId: String) async throws {
        let request = DefaultRequest(method: "POST", path: "/ai-video/cancel/\(predictionId)")
        _ = try await self.request(request, decodeTo: GenericApiResponse.self)
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

#Preview {
    AiTravelVideoGenerator()
}

