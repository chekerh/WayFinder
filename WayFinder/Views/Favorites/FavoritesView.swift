import SwiftUI

struct FavoritesView: View {
    @StateObject private var viewModel = FavoriteViewModel()
    @Environment(\.colorScheme) private var colorScheme
    
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
                    Text("Aucun favori")
                        .font(.headline)
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    Text("Vos favoris apparaîtront ici")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(spacing: 0) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Favoris")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        Text("\(viewModel.favorites.count) favori\(viewModel.favorites.count > 1 ? "s" : "")")
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
        HStack(spacing: 12) {
            // Icône selon le type
            Image(systemName: iconForType(favorite.itemType))
                .font(.system(size: 24))
                .foregroundColor(ThemeColors.accent())
                .frame(width: 50, height: 50)
                .background(
                    Circle()
                        .fill(ThemeColors.accent().opacity(0.1))
                )
            
            VStack(alignment: .leading, spacing: 4) {
                Text(favorite.itemData.name ?? favorite.itemId)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                
                if let city = favorite.itemData.city, let country = favorite.itemData.country {
                    Text("\(city), \(country)")
                        .font(.caption)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                
                if let price = favorite.itemData.price, let currency = favorite.itemData.currency {
                    Text("\(String(format: "%.2f", price)) \(currency)")
                        .font(.caption)
                        .foregroundStyle(ThemeColors.accent())
                }
            }
            
            Spacer()
            
            Button(action: onRemove) {
                Image(systemName: "heart.fill")
                    .foregroundColor(.red)
                    .font(.system(size: 20))
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
        )
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 8, x: 0, y: 4)
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

