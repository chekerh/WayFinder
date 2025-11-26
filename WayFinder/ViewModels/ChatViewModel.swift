import Foundation
import SwiftUI

enum ChatUiState: Equatable {
    case idle
    case loading
    case success(String)
    case error(String)
}

struct ChatMessageUi: Identifiable {
    let id = UUID()
    let text: String
    let isFromUser: Bool
    let modelUsed: String?
    let flightPacks: [FlightPack]?
}

@MainActor
class ChatViewModel: ObservableObject {
    @Published var messages: [ChatMessageUi] = []
    @Published var availableModels: [AvailableModel] = []
    @Published var selectedModel: ChatModel?
    @Published var isLoading: Bool = false
    @Published var uiState: ChatUiState = .idle
    
    private let chatService = ChatService.shared
    
    init() {
        Task {
            loadAvailableModels()
            loadHistory()
        }
    }
    
    func sendMessage(_ message: String) {
        guard !message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, !isLoading else { return }
        
        Task {
            isLoading = true
            
            // Add user message to UI immediately
            let userMessage = ChatMessageUi(
                text: message,
                isFromUser: true,
                modelUsed: nil,
                flightPacks: nil
            )
            messages.append(userMessage)
            
            do {
                // Send to API
                let response = try await chatService.sendMessage(message, model: selectedModel)
                
                // Add AI response to UI
                let aiMessage = ChatMessageUi(
                    text: response.message,
                    isFromUser: false,
                    modelUsed: response.modelUsed,
                    flightPacks: response.flightPacks
                )
                messages.append(aiMessage)
                
                uiState = .success("Message sent")
            } catch {
                // Remove user message on error
                if !messages.isEmpty {
                    messages.removeLast()
                }
                uiState = .error(error.localizedDescription)
            }
            
            isLoading = false
        }
    }
    
    func switchModel(_ model: ChatModel) {
        Task {
            do {
                let response = try await chatService.switchModel(model)
                if response.success {
                    selectedModel = model
                    uiState = .success("Model switched to \(response.model)")
                }
            } catch {
                uiState = .error(error.localizedDescription)
            }
        }
    }
    
    func loadHistory() {
        Task {
            do {
                let history = try await chatService.getHistory(limit: 50)
                messages = history.map { item in
                    ChatMessageUi(
                        text: item.message,
                        isFromUser: item.role == "user",
                        modelUsed: item.role == "assistant" ? item.modelUsed : nil,
                        flightPacks: item.flightPacks
                    )
                }
            } catch {
                // Silently fail - history is optional
                print("Failed to load chat history: \(error.localizedDescription)")
            }
        }
    }
    
    func clearHistory() {
        Task {
            do {
                _ = try await chatService.clearHistory()
                messages = []
                uiState = .success("History cleared")
            } catch {
                uiState = .error(error.localizedDescription)
            }
        }
    }
    
    private func loadAvailableModels() {
        Task {
            do {
                let response = try await chatService.getAvailableModels()
                availableModels = response.models
                
                // Set default model to first available one
                if let firstAvailable = response.models.first(where: { $0.available }),
                   let model = ChatModel(rawValue: firstAvailable.id) {
                    selectedModel = model
                } else {
                    // Fallback to huggingface
                    selectedModel = .huggingface
                }
            } catch {
                // Set default models if API fails
                availableModels = [
                    AvailableModel(id: "huggingface", name: "Hugging Face (Free)", available: true),
                    AvailableModel(id: "openai_gpt4o_mini", name: "OpenAI GPT-4o Mini", available: true),
                    AvailableModel(id: "openai_gpt4o", name: "OpenAI GPT-4o", available: true)
                ]
                selectedModel = .huggingface
            }
        }
    }
    
    func clearError() {
        uiState = .idle
    }
}

