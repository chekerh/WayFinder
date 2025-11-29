import SwiftUI

struct FavoritesView: View {
    @ObservedObject var viewModel: FavoriteViewModel
    @Environment(\.colorScheme) private var colorScheme
    
    init(viewModel: FavoriteViewModel? = nil) {
        // Si un viewModel est fourni, on l'utilise, sinon on en crée un nouveau
        if let viewModel = viewModel {
            self._viewModel = ObservedObject(wrappedValue: viewModel)
        } else {
            self._viewModel = ObservedObject(wrappedValue: FavoriteViewModel())
        }
    }
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.errorMessage {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 48))
                        .foregroundColor(.orange)
                    Text("Erreur")
                        .font(.headline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text(error)
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    Button(action: {
                        Task {
                            await viewModel.loadFavorites()
                        }
                    }) {
                        Text("Réessayer")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(ThemeColors.accent())
                            .clipShape(Capsule())
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.favorites.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "heart.slash")
                        .font(.system(size: 48))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    Text("favorites_title")
                        .font(.headline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("favorites_empty")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(spacing: 0) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("favorites_title")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        Text(String(format: String(localized: "favorites_count"), viewModel.favorites.count))
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.top, 20)
                    .padding(.bottom, 24)
                    
                    // Liste des favoris
                    ScrollView {
                        VStack(spacing: 16) {
                            ForEach(viewModel.favorites) { favorite in
                                FavoriteCard(
                                    favorite: favorite,
                                    onRemove: {
                                        Task {
                                            await viewModel.removeFavorite(
                                                itemType: favorite.itemType,
                                                itemId: favorite.itemId
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 74)
                    }
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadFavorites()
            await viewModel.loadFavoriteCount()
        }
        .refreshable {
            await viewModel.loadFavorites()
            await viewModel.loadFavoriteCount()
        }
    }
}

struct FavoriteCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let favorite: Favorite
    let onRemove: () -> Void
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Background Image
            Group {
                if let imageUrl = favorite.itemData.imageUrl, !imageUrl.isEmpty, let url = URL(string: imageUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                        case .failure, .empty:
                            Rectangle()
                                .fill(LinearGradient(
                                    colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ))
                        @unknown default:
                            Rectangle()
                                .fill(LinearGradient(
                                    colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ))
                        }
                    }
                } else {
                    Rectangle()
                        .fill(LinearGradient(
                            colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ))
                }
            }
            .frame(height: 200)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            
            // Gradient Overlay
            LinearGradient(
                colors: [Color.clear, Color.black.opacity(0.7)],
                startPoint: .center,
                endPoint: .bottom
            )
            .frame(height: 200)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            
            // Content
            VStack(alignment: .leading, spacing: 2) {
                Text(favorite.itemData.name ?? favorite.itemId)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                
                if let city = favorite.itemData.city, let country = favorite.itemData.country {
                    Text("\(city), \(country)")
                        .font(.body)
                        .foregroundColor(.white.opacity(0.9))
                }
                
                if let price = favorite.itemData.price, let currency = favorite.itemData.currency {
                    Text(formatPrice(price, currency: currency))
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.top, 4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            
            // Favorite Button (top right)
            VStack {
                HStack {
                    Spacer()
                    Button(action: onRemove) {
                        ZStack {
                            Circle()
                                .fill(Color.white.opacity(0.3))
                                .frame(width: 40, height: 40)
                            
                            Image(systemName: "heart.fill")
                                .foregroundColor(Color(red: 1.0, green: 0.09, blue: 0.267))
                                .font(.system(size: 20))
                        }
                    }
                    .buttonStyle(.plain)
                }
                Spacer()
            }
            .padding(12)
        }
        .frame(height: 200)
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.12), radius: 12, x: 0, y: 8)
    }
    
    private func formatPrice(_ price: Double?, currency: String?) -> String {
        guard let price = price, let currency = currency else {
            return ""
        }
        // Si le prix est un entier, on l'affiche sans décimales
        if price.truncatingRemainder(dividingBy: 1) == 0 {
            return "\(Int(price)) \(currency)"
        } else {
            return "\(String(format: "%.2f", price)) \(currency)"
        }
    }
    
    private func iconForType(_ type: FavoriteItemType) -> String {
        switch type {
        case .flight: return "airplane"
        case .destination: return "map"
        case .activity: return "figure.walk"
        case .hotel: return "bed.double"
        }
    }
}

struct FavoritesView_Previews: PreviewProvider {
    static var previews: some View {
        FavoritesView()
    }
}

