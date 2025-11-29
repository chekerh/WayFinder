import SwiftUI

struct ChatView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ChatViewModel()
    @State private var messageText: String = ""
    @State private var showModelSelector: Bool = false
    @State private var errorMessage: String?
    @FocusState private var isInputFocused: Bool
    @State private var selectedDestination: FlightDestination? = nil
    @State private var showFlightDetail = false
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
                                ChatMessageBubble(
                                    message: message,
                                    onFlightPackClick: { pack in
                                        handleFlightPackClick(pack: pack)
                                    }
                                )
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
        .navigationDestination(isPresented: $showFlightDetail) {
            if let destination = selectedDestination {
                FlightDetailScreen(
                    destinationId: destination.id,
                    destination: destination
                )
            }
        }
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
    
    private func handleFlightPackClick(pack: FlightPack) {
        let destination = pack.toFlightDestination()
        selectedDestination = destination
        showFlightDetail = true
    }
}

// Extension pour convertir FlightPack en FlightDestination (comme Android)
extension FlightPack {
    func toFlightDestination() -> FlightDestination {
        let priceValue = packPriceValue(price: price)
        let currency = packCurrency(price: price) ?? "USD"
        let generatedId = "\(origin)-\(destination)-\(airline ?? "")-\(price)".hash.description
        let imageUrl = getDestinationImageUrl(destination: destination, city: destination)
        
        return FlightDestination(
            id: generatedId,
            name: destination.isEmpty ? title : destination,
            city: destination,
            country: "",
            imageUrl: imageUrl,
            price: priceValue,
            currency: currency,
            description: details ?? "Flight from \(origin) to \(destination) with \(airline ?? "various airlines").",
            departureDate: nil,
            arrivalDate: nil,
            airline: airline
        )
    }
    
    // Fonction pour obtenir l'URL de l'image basée sur la destination
    private func getDestinationImageUrl(destination: String, city: String) -> String? {
        let destLower = destination.lowercased()
        let cityLower = city.lowercased()
        let titleLower = title.lowercased()
        
        // Mapping des destinations populaires vers des images Unsplash
        if destLower.contains("cdg") || cityLower.contains("paris") || titleLower.contains("paris") {
            return "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800&q=80" // Paris
        } else if destLower.contains("fco") || cityLower.contains("rome") || titleLower.contains("rome") {
            return "https://images.unsplash.com/photo-1529260830199-42c24126f198?w=800&q=80" // Rome
        } else if destLower.contains("mad") || cityLower.contains("madrid") || titleLower.contains("madrid") {
            return "https://images.unsplash.com/photo-1539037116277-4db20889f2d2?w=800&q=80" // Madrid
        } else if destLower.contains("bcn") || cityLower.contains("barcelona") || titleLower.contains("barcelona") {
            return "https://images.unsplash.com/photo-1539037116277-4db20889f2d2?w=800&q=80" // Barcelona
        } else if destLower.contains("lhr") || cityLower.contains("london") || titleLower.contains("london") {
            return "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&q=80" // London
        } else if destLower.contains("jfk") || cityLower.contains("new york") || titleLower.contains("new york") {
            return "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&q=80" // New York
        } else if destLower.contains("dxb") || cityLower.contains("dubai") || titleLower.contains("dubai") {
            return "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800&q=80" // Dubai
        } else if destLower.contains("ber") || cityLower.contains("berlin") || titleLower.contains("berlin") {
            return "https://images.unsplash.com/photo-1587330979470-1a0b5b0b5b5b?w=800&q=80" // Berlin
        } else if destLower.contains("ams") || cityLower.contains("amsterdam") || titleLower.contains("amsterdam") {
            return "https://images.unsplash.com/photo-1534351590666-13e3e96b5017?w=800&q=80" // Amsterdam
        } else if destLower.contains("vie") || cityLower.contains("vienna") || titleLower.contains("vienna") {
            return "https://images.unsplash.com/photo-1516550893923-42d28e5677af?w=800&q=80" // Vienna
        } else if destLower.contains("ath") || cityLower.contains("athens") || titleLower.contains("athens") {
            return "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=800&q=80" // Athens
        } else if destLower.contains("ist") || cityLower.contains("istanbul") || titleLower.contains("istanbul") {
            return "https://images.unsplash.com/photo-1524231757912-21f4fe3a7200?w=800&q=80" // Istanbul
        } else if destLower.contains("cai") || cityLower.contains("cairo") || titleLower.contains("cairo") {
            return "https://images.unsplash.com/photo-1539650116574-75c0c6d73ab6?w=800&q=80" // Cairo
        } else if destLower.contains("doh") || cityLower.contains("doha") || titleLower.contains("doha") {
            return "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&q=80" // Doha
        } else {
            // Image par défaut pour les destinations non mappées
            return "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&q=80" // Travel default
        }
    }
    
    private func packPriceValue(price: String) -> Double? {
        guard !price.isEmpty else { return nil }
        let digits = price.filter { $0.isNumber || $0 == "." }
        if let value = Double(digits) {
            return Double(Int(value))
        }
        return nil
    }
    
    private func packCurrency(price: String) -> String? {
        guard !price.isEmpty else { return nil }
        let parts = price.trimmingCharacters(in: .whitespacesAndNewlines).components(separatedBy: " ")
        return parts.last?.filter { $0.isLetter }
    }
}

struct ChatMessageBubble: View {
    @Environment(\.colorScheme) private var colorScheme
    let message: ChatMessageUi
    let onFlightPackClick: (FlightPack) -> Void
    
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
                        FlightPackCard(
                            pack: flightPacks[index],
                            onFlightPackClick: onFlightPackClick
                        )
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
    let onFlightPackClick: (FlightPack) -> Void
    
    private var destinationImageUrl: String? {
        let destLower = pack.destination.lowercased()
        let cityLower = pack.destination.lowercased()
        let titleLower = pack.title.lowercased()
        
        // Mapping des destinations populaires vers des images Unsplash
        if destLower.contains("cdg") || cityLower.contains("paris") || titleLower.contains("paris") {
            return "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800&q=80" // Paris
        } else if destLower.contains("fco") || cityLower.contains("rome") || titleLower.contains("rome") {
            return "https://images.unsplash.com/photo-1529260830199-42c24126f198?w=800&q=80" // Rome
        } else if destLower.contains("mad") || cityLower.contains("madrid") || titleLower.contains("madrid") {
            return "https://images.unsplash.com/photo-1539037116277-4db20889f2d2?w=800&q=80" // Madrid
        } else if destLower.contains("bcn") || cityLower.contains("barcelona") || titleLower.contains("barcelona") {
            return "https://images.unsplash.com/photo-1539037116277-4db20889f2d2?w=800&q=80" // Barcelona
        } else if destLower.contains("lhr") || cityLower.contains("london") || titleLower.contains("london") {
            return "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&q=80" // London
        } else if destLower.contains("jfk") || cityLower.contains("new york") || titleLower.contains("new york") {
            return "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&q=80" // New York
        } else if destLower.contains("dxb") || cityLower.contains("dubai") || titleLower.contains("dubai") {
            return "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800&q=80" // Dubai
        } else if destLower.contains("ber") || cityLower.contains("berlin") || titleLower.contains("berlin") {
            return "https://images.unsplash.com/photo-1587330979470-1a0b5b0b5b5b?w=800&q=80" // Berlin
        } else if destLower.contains("ams") || cityLower.contains("amsterdam") || titleLower.contains("amsterdam") {
            return "https://images.unsplash.com/photo-1534351590666-13e3e96b5017?w=800&q=80" // Amsterdam
        } else if destLower.contains("vie") || cityLower.contains("vienna") || titleLower.contains("vienna") {
            return "https://images.unsplash.com/photo-1516550893923-42d28e5677af?w=800&q=80" // Vienna
        } else if destLower.contains("ath") || cityLower.contains("athens") || titleLower.contains("athens") {
            return "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=800&q=80" // Athens
        } else if destLower.contains("ist") || cityLower.contains("istanbul") || titleLower.contains("istanbul") {
            return "https://images.unsplash.com/photo-1524231757912-21f4fe3a7200?w=800&q=80" // Istanbul
        } else if destLower.contains("cai") || cityLower.contains("cairo") || titleLower.contains("cairo") {
            return "https://images.unsplash.com/photo-1539650116574-75c0c6d73ab6?w=800&q=80" // Cairo
        } else if destLower.contains("doh") || cityLower.contains("doha") || titleLower.contains("doha") {
            return "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&q=80" // Doha
        } else {
            // Image par défaut pour les destinations non mappées
            return "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&q=80" // Travel default
        }
    }
    
    var body: some View {
        Button(action: {
            onFlightPackClick(pack)
        }) {
            HStack(spacing: 12) {
                // Image de la destination
                if let imageUrl = destinationImageUrl, let url = URL(string: imageUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            RoundedRectangle(cornerRadius: 12)
                                .fill(ThemeColors.surface(colorScheme))
                                .frame(width: 80, height: 80)
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                                .frame(width: 80, height: 80)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        case .failure:
                            RoundedRectangle(cornerRadius: 12)
                                .fill(ThemeColors.surface(colorScheme))
                                .frame(width: 80, height: 80)
                                .overlay(
                                    Image(systemName: "photo")
                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                )
                        @unknown default:
                            RoundedRectangle(cornerRadius: 12)
                                .fill(ThemeColors.surface(colorScheme))
                                .frame(width: 80, height: 80)
                        }
                    }
                } else {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(ThemeColors.surface(colorScheme))
                        .frame(width: 80, height: 80)
                        .overlay(
                            Image(systemName: "photo")
                                .foregroundColor(ThemeColors.secondaryText(colorScheme))
                        )
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(pack.title)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    if let details = pack.details {
                        Text(details)
                            .font(.system(size: 12))
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            .lineLimit(2)
                    }
                    
                    Text("\(pack.origin) → \(pack.destination)")
                        .font(.system(size: 14))
                        .foregroundColor(ThemeColors.accent())
                    
                    if let airline = pack.airline {
                        Text(airline)
                            .font(.system(size: 12))
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text(pack.price)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(ThemeColors.accent())
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(ThemeColors.surface(colorScheme))
            )
            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
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

