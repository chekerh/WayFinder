import SwiftUI

struct ChatView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ChatViewModel()
    @State private var messageText: String = ""
    @State private var showModelSelector: Bool = false
    @State private var errorMessage: String?
    @FocusState private var isInputFocused: Bool
    var onBackToHome: (() -> Void)? = nil
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                headerView
                
                // Messages list
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            if viewModel.messages.isEmpty {
                                welcomeMessage
                            }
                            
                            ForEach(viewModel.messages) { message in
                                ChatMessageBubble(message: message)
                                    .id(message.id)
                            }
                            
                            if viewModel.isLoading {
                                typingIndicator
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 16)
                    }
                    .onChange(of: viewModel.messages.count) { _, _ in
                        if let lastMessage = viewModel.messages.last {
                            withAnimation {
                                proxy.scrollTo(lastMessage.id, anchor: .bottom)
                            }
                        }
                    }
                    .onChange(of: viewModel.isLoading) { _, isLoading in
                        if isLoading {
                            // Scroll to bottom when loading starts
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                                if let lastMessage = viewModel.messages.last {
                                    withAnimation {
                                        proxy.scrollTo(lastMessage.id, anchor: .bottom)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Message input
                messageInputView
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showModelSelector) {
            ModelSelectorView(
                availableModels: viewModel.availableModels,
                selectedModel: viewModel.selectedModel,
                onModelSelected: { model in
                    viewModel.switchModel(model)
                    showModelSelector = false
                },
                onDismiss: { showModelSelector = false }
            )
        }
        .onChange(of: viewModel.uiState) { _, newState in
            if case .error(let message) = newState {
                errorMessage = message
                viewModel.clearError()
            }
        }
        .alert("Error", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK") {
                errorMessage = nil
            }
        } message: {
            if let errorMessage = errorMessage {
                Text(errorMessage)
            }
        }
    }
    
    private var headerView: some View {
        VStack(spacing: 4) {
            HStack {
                // Bouton de retour
                Button(action: {
                    if let onBackToHome = onBackToHome {
                        onBackToHome()
                    } else {
                        dismiss()
                    }
                }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(ThemeColors.primaryText(colorScheme))
                        .frame(width: 44, height: 44)
                }
                
                Text("AI Travel Assistant")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                
                Spacer()
                
                Button(action: { showModelSelector = true }) {
                    Image(systemName: "gearshape.fill")
                        .font(.system(size: 20))
                        .foregroundColor(ThemeColors.accent())
                }
            }
            
            if let selectedModel = viewModel.selectedModel {
                Text(selectedModel.displayName)
                    .font(.system(size: 12))
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            } else {
                Text("Select Model")
                    .font(.system(size: 12))
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(ThemeColors.surface(colorScheme))
    }
    
    private var welcomeMessage: some View {
        VStack(spacing: 12) {
            Text("👋 Welcome!")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            
            Text("I'm your AI travel assistant. Ask me about flights, destinations, or travel recommendations based on your preferences!")
                .font(.system(size: 16))
                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                .multilineTextAlignment(.center)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(ThemeColors.accent().opacity(0.1))
        )
    }
    
    private var messageInputView: some View {
        HStack(spacing: 12) {
            TextField("Type your message...", text: $messageText, axis: .vertical)
                .textFieldStyle(.plain)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 28)
                        .fill(ThemeColors.surface(colorScheme))
                )
                .focused($isInputFocused)
                .disabled(viewModel.isLoading)
                .onSubmit {
                    sendMessage()
                }
            
            Button(action: sendMessage) {
                Image(systemName: "paperplane.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(
                        messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isLoading
                            ? ThemeColors.secondaryText(colorScheme).opacity(0.3)
                            : ThemeColors.accent()
                    )
                    .frame(width: 44, height: 44)
                    .background(
                        Circle()
                            .fill(
                                messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isLoading
                                    ? Color.clear
                                    : ThemeColors.accent().opacity(0.1)
                            )
                    )
            }
            .disabled(messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isLoading)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(ThemeColors.surface(colorScheme))
    }
    
    private var typingIndicator: some View {
        HStack(spacing: 8) {
            ForEach(0..<3) { index in
                Circle()
                    .fill(ThemeColors.accent())
                    .frame(width: 8, height: 8)
                    .scaleEffect(1.0)
                    .animation(
                        Animation.easeInOut(duration: 0.6)
                            .repeatForever()
                            .delay(Double(index) * 0.2),
                        value: viewModel.isLoading
                    )
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(ThemeColors.surface(colorScheme))
        )
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    
    private func sendMessage() {
        let trimmedMessage = messageText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedMessage.isEmpty else { return }
        
        viewModel.sendMessage(trimmedMessage)
        messageText = ""
        isInputFocused = false
    }
}

struct ChatMessageBubble: View {
    @Environment(\.colorScheme) private var colorScheme
    let message: ChatMessageUi
    
    var body: some View {
        HStack {
            if message.isFromUser {
                Spacer(minLength: 60)
            }
            
            VStack(alignment: message.isFromUser ? .trailing : .leading, spacing: 8) {
                Text(message.text)
                    .font(.system(size: 16))
                    .foregroundColor(message.isFromUser ? .white : ThemeColors.primaryText(colorScheme))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: 16)
                            .fill(
                                message.isFromUser
                                    ? ThemeColors.accent()
                                    : ThemeColors.surface(colorScheme)
                            )
                    )
                
                // Show model used for AI messages
                if !message.isFromUser, let modelUsed = message.modelUsed {
                    Text("via \(modelUsed)")
                        .font(.system(size: 10))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                
                // Flight packs
                if let flightPacks = message.flightPacks {
                    ForEach(flightPacks.indices, id: \.self) { index in
                        FlightPackCard(pack: flightPacks[index])
                    }
                }
            }
            .frame(maxWidth: 280, alignment: message.isFromUser ? .trailing : .leading)
            
            if !message.isFromUser {
                Spacer(minLength: 60)
            }
        }
    }
}

struct FlightPackCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let pack: FlightPack
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(pack.title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                
                if let details = pack.details {
                    Text(details)
                        .font(.system(size: 14))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
            }
            
            Spacer()
            
            Text(pack.price)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(ThemeColors.accent())
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(ThemeColors.surface(colorScheme))
        )
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 4, x: 0, y: 2)
    }
}

struct ModelSelectorView: View {
    @Environment(\.colorScheme) private var colorScheme
    let availableModels: [AvailableModel]
    let selectedModel: ChatModel?
    let onModelSelected: (ChatModel) -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Text("Select AI Model")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    .padding(.top, 20)
                
                List {
                    ForEach(availableModels, id: \.id) { model in
                        Button(action: {
                            if let chatModel = ChatModel(rawValue: model.id) {
                                onModelSelected(chatModel)
                            }
                        }) {
                            HStack {
                                if let chatModel = ChatModel(rawValue: model.id),
                                   chatModel == selectedModel {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(ThemeColors.accent())
                                } else {
                                    Image(systemName: "circle")
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                }
                                
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(model.name)
                                        .font(.system(size: 16))
                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                    
                                    if !model.available {
                                        Text("Not available")
                                            .font(.system(size: 12))
                                            .foregroundColor(.red)
                                    }
                                }
                                
                                Spacer()
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .disabled(!model.available)
                    }
                }
                .listStyle(.plain)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") {
                        onDismiss()
                    }
                    .foregroundColor(ThemeColors.accent())
                }
            }
        }
    }
}

struct ChatView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            ChatView()
        }
    }
}

