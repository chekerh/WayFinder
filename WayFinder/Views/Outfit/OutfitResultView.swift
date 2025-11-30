import SwiftUI

struct OutfitResultView: View {
    let outfitId: String
    @StateObject private var viewModel = OutfitWeatherViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    
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
                    
                    Text("outfit_result_title")
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
                
                // Content
                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = viewModel.errorMessage {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 48))
                            .foregroundColor(.orange)
                        Text("outfit_error")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        Text(error)
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button(action: {
                            Task {
                                await viewModel.loadOutfit(outfitId: outfitId)
                            }
                        }) {
                            Text("outfit_retry")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 12)
                                .background(ThemeColors.accent())
                                .clipShape(Capsule())
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let outfit = viewModel.outfit {
                    OutfitResultContent(outfit: outfit, colorScheme: colorScheme)
                } else {
                    Text("outfit_loading")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
        }
        .navigationBarHidden(true)
        .onAppear {
            Task {
                await viewModel.loadOutfit(outfitId: outfitId)
            }
        }
    }
}

struct OutfitResultContent: View {
    let outfit: Outfit
    let colorScheme: ColorScheme
    
    private var recommendation: OutfitRecommendation? {
        outfit.recommendation
    }
    
    private var weather: WeatherData? {
        outfit.weatherData
    }
    
    private var scoreColor: Color {
        guard let score = recommendation?.score else { return .gray }
        if score >= 80 {
            return Color(red: 0.298, green: 0.686, blue: 0.314) // Green
        } else if score >= 60 {
            return Color(red: 1.0, green: 0.596, blue: 0.0) // Orange
        } else {
            return Color(red: 0.956, green: 0.262, blue: 0.212) // Red
        }
    }
    
    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 24) {
                // Outfit Image - Enhanced Design
                AsyncImage(url: URL(string: outfit.imageUrl)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(height: 320)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .overlay(
                                RoundedRectangle(cornerRadius: 24)
                                    .stroke(
                                        LinearGradient(
                                            gradient: Gradient(colors: [
                                                Color.white.opacity(0.3),
                                                Color.white.opacity(0.1)
                                            ]),
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        ),
                                        lineWidth: 1
                                    )
                            )
                    case .failure, .empty:
                        RoundedRectangle(cornerRadius: 24)
                            .fill(
                                LinearGradient(
                                    gradient: Gradient(colors: [
                                        ThemeColors.surface(colorScheme),
                                        ThemeColors.surface(colorScheme).opacity(0.8)
                                    ]),
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(height: 320)
                            .overlay(
                                Image(systemName: "photo")
                                    .font(.system(size: 48, weight: .light))
                                    .foregroundColor(ThemeColors.secondaryText(colorScheme).opacity(0.5))
                            )
                    @unknown default:
                        EmptyView()
                    }
                }
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.15), radius: 20, x: 0, y: 10)
                .padding(.horizontal, 24)
                .padding(.top, 16)
                
                // Score Card - Enhanced Design
                VStack(spacing: 16) {
                    // Score Circle with Gradient
                    ZStack {
                        // Outer glow effect
                        Circle()
                            .fill(
                                RadialGradient(
                                    gradient: Gradient(colors: [
                                        scoreColor.opacity(0.3),
                                        scoreColor.opacity(0.1),
                                        Color.clear
                                    ]),
                                    center: .center,
                                    startRadius: 50,
                                    endRadius: 80
                                )
                            )
                            .frame(width: 160, height: 160)
                            .blur(radius: 10)
                        
                        // Main circle with gradient
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
                            .frame(width: 140, height: 140)
                            .shadow(color: scoreColor.opacity(0.4), radius: 20, x: 0, y: 10)
                        
                        VStack(spacing: 4) {
                            Text("\(recommendation?.score ?? 0)")
                                .font(.system(size: 56, weight: .bold, design: .rounded))
                                .foregroundColor(.white)
                            
                            Text("/100")
                                .font(.system(size: 18, weight: .semibold, design: .rounded))
                                .foregroundColor(.white.opacity(0.8))
                        }
                    }
                    
                    Text("outfit_adaptation_score")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        .tracking(0.5)
                    
                    // Status Icon with background
                    HStack(spacing: 8) {
                        Image(systemName: recommendation?.isSuitable == true ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                            .font(.system(size: 24, weight: .semibold))
                            .foregroundColor(.white)
                        
                        Text(recommendation?.isSuitable == true ? "Adapté" : "À améliorer")
                            .font(.system(size: 16, weight: .semibold, design: .rounded))
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(
                        Capsule()
                            .fill(recommendation?.isSuitable == true ? Color(red: 0.298, green: 0.686, blue: 0.314) : Color(red: 1.0, green: 0.596, blue: 0.0))
                    )
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 32)
                .padding(.horizontal, 24)
                .background(
                    RoundedRectangle(cornerRadius: 24)
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    scoreColor.opacity(0.15),
                                    scoreColor.opacity(0.05)
                                ]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 24)
                                .stroke(scoreColor.opacity(0.2), lineWidth: 1)
                        )
                )
                .shadow(color: scoreColor.opacity(0.2), radius: 15, x: 0, y: 8)
                .padding(.horizontal, 24)
                
                // Weather Info - Enhanced Design
                if let weather = weather {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack(spacing: 8) {
                            Image(systemName: "cloud.sun.fill")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundStyle(
                                    LinearGradient(
                                        gradient: Gradient(colors: [Color.blue, Color.cyan]),
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                            
                            Text("outfit_destination_weather")
                                .font(.system(size: 20, weight: .bold, design: .rounded))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        }
                        
                        Divider()
                            .background(ThemeColors.secondaryText(colorScheme).opacity(0.2))
                        
                        VStack(spacing: 14) {
                            HStack {
                                HStack(spacing: 8) {
                                    Image(systemName: "thermometer")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(Color.orange)
                                    
                                    Text("outfit_temperature")
                                        .font(.system(size: 15, weight: .medium, design: .rounded))
                                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                }
                                Spacer()
                                Text("\(weather.temperature)°C")
                                    .font(.system(size: 20, weight: .bold, design: .rounded))
                                    .foregroundStyle(
                                        LinearGradient(
                                            gradient: Gradient(colors: [Color.orange, Color.red]),
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                            }
                            
                            HStack {
                                HStack(spacing: 8) {
                                    Image(systemName: "cloud.fill")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(Color.blue)
                                    
                                    Text("outfit_condition")
                                        .font(.system(size: 15, weight: .medium, design: .rounded))
                                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                }
                                Spacer()
                                Text(weather.condition.capitalized)
                                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(24)
                    .background(
                        RoundedRectangle(cornerRadius: 20)
                            .fill(ThemeColors.surface(colorScheme))
                            .overlay(
                                RoundedRectangle(cornerRadius: 20)
                                    .stroke(
                                        LinearGradient(
                                            gradient: Gradient(colors: [Color.blue.opacity(0.3), Color.cyan.opacity(0.2)]),
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        ),
                                        lineWidth: 1
                                    )
                            )
                    )
                    .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.08), radius: 12, x: 0, y: 6)
                    .padding(.horizontal, 24)
                }
                
                // Feedback - Enhanced Design
                if let recommendation = recommendation {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack(spacing: 8) {
                            Image(systemName: "message.fill")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundStyle(
                                    LinearGradient(
                                        gradient: Gradient(colors: [Color.purple, Color.pink]),
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                            
                            Text("outfit_feedback")
                                .font(.system(size: 20, weight: .bold, design: .rounded))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        }
                        
                        Divider()
                            .background(ThemeColors.secondaryText(colorScheme).opacity(0.2))
                        
                        Text(recommendation.feedback)
                            .font(.system(size: 17, weight: .regular, design: .rounded))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            .lineSpacing(6)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(24)
                    .background(
                        RoundedRectangle(cornerRadius: 20)
                            .fill(ThemeColors.surface(colorScheme))
                            .overlay(
                                RoundedRectangle(cornerRadius: 20)
                                    .stroke(
                                        LinearGradient(
                                            gradient: Gradient(colors: [Color.purple.opacity(0.2), Color.pink.opacity(0.1)]),
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        ),
                                        lineWidth: 1
                                    )
                            )
                    )
                    .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.3 : 0.08), radius: 12, x: 0, y: 6)
                    .padding(.horizontal, 24)
                    
                    // Suggestions - Enhanced Design
                    if !recommendation.suggestions.isEmpty {
                        VStack(alignment: .leading, spacing: 16) {
                            HStack(spacing: 8) {
                                Image(systemName: "lightbulb.fill")
                                    .font(.system(size: 20, weight: .semibold))
                                    .foregroundStyle(
                                        LinearGradient(
                                            gradient: Gradient(colors: [Color.yellow, Color.orange]),
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                
                                Text("outfit_suggestions")
                                    .font(.system(size: 20, weight: .bold, design: .rounded))
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                            
                            Divider()
                                .background(ThemeColors.secondaryText(colorScheme).opacity(0.2))
                            
                            VStack(alignment: .leading, spacing: 12) {
                                ForEach(Array(recommendation.suggestions.enumerated()), id: \.element) { index, suggestion in
                                    HStack(alignment: .top, spacing: 12) {
                                        ZStack {
                                            Circle()
                                                .fill(
                                                    LinearGradient(
                                                        gradient: Gradient(colors: [
                                                            ThemeColors.accent(),
                                                            ThemeColors.accent().opacity(0.7)
                                                        ]),
                                                        startPoint: .topLeading,
                                                        endPoint: .bottomTrailing
                                                    )
                                                )
                                                .frame(width: 24, height: 24)
                                            
                                            Text("\(index + 1)")
                                                .font(.system(size: 12, weight: .bold, design: .rounded))
                                                .foregroundColor(.white)
                                        }
                                        
                                        Text(suggestion)
                                            .font(.system(size: 16, weight: .medium, design: .rounded))
                                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .lineSpacing(4)
                                    }
                                    .padding(.vertical, 4)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(24)
                        .background(
                            RoundedRectangle(cornerRadius: 20)
                                .fill(
                                    LinearGradient(
                                        gradient: Gradient(colors: [
                                            ThemeColors.accent().opacity(0.15),
                                            ThemeColors.accent().opacity(0.08)
                                        ]),
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 20)
                                        .stroke(ThemeColors.accent().opacity(0.3), lineWidth: 1)
                                )
                        )
                        .shadow(color: ThemeColors.accent().opacity(0.2), radius: 12, x: 0, y: 6)
                        .padding(.horizontal, 24)
                    }
                }
                
                Spacer()
                    .frame(height: 32)
            }
        }
    }
}

