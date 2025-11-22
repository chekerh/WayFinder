import Foundation

// Helper function pour accéder à FavoriteService.shared depuis un contexte non isolé
nonisolated private func getFavoriteServiceShared() -> FavoriteService {
    // MainActor.assumeIsolated permet d'accéder à .shared depuis un contexte non isolé
    return MainActor.assumeIsolated {
        FavoriteService.shared
    }
}

@MainActor
final class FavoriteViewModel: ObservableObject {
    @Published var favorites: [Favorite] = []
    @Published var favoriteCount: Int = 0
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: FavoriteService
    
    nonisolated init(service: FavoriteService? = nil) {
        // Accéder à .shared depuis un contexte non isolé
        if let service = service {
            self.service = service
        } else {
            // Utiliser une fonction helper nonisolated pour accéder à .shared
            self.service = getFavoriteServiceShared()
        }
    }
    
    func loadFavorites(itemType: FavoriteItemType? = nil) async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [FavoriteViewModel] Loading favorites")
        do {
            favorites = try await service.getFavorites(itemType: itemType)
            print("✅ [FavoriteViewModel] Loaded \(favorites.count) favorites")
        } catch {
            print("❌ [FavoriteViewModel] Error loading favorites: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func loadFavoriteCount(itemType: FavoriteItemType? = nil) async {
        print("🔄 [FavoriteViewModel] Loading favorite count")
        do {
            favoriteCount = try await service.getFavoriteCount(itemType: itemType)
            print("✅ [FavoriteViewModel] Favorite count: \(favoriteCount)")
        } catch {
            print("❌ [FavoriteViewModel] Error loading count: \(error.localizedDescription)")
        }
    }
    
    func addFavorite(
        itemType: FavoriteItemType,
        itemId: String,
        itemData: [String: String]? = nil
    ) async {
        print("🔄 [FavoriteViewModel] Adding favorite")
        do {
            let favorite = try await service.addFavorite(
                itemType: itemType,
                itemId: itemId,
                itemData: itemData
            )
            favorites.append(favorite)
            await loadFavoriteCount()
            print("✅ [FavoriteViewModel] Favorite added")
        } catch {
            print("❌ [FavoriteViewModel] Error adding favorite: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func removeFavorite(itemType: FavoriteItemType, itemId: String) async {
        print("🔄 [FavoriteViewModel] Removing favorite")
        do {
            try await service.removeFavorite(itemType: itemType, itemId: itemId)
            favorites.removeAll { $0.itemType == itemType && $0.itemId == itemId }
            await loadFavoriteCount()
            print("✅ [FavoriteViewModel] Favorite removed")
        } catch {
            print("❌ [FavoriteViewModel] Error removing favorite: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func checkFavorite(itemType: FavoriteItemType, itemId: String) async -> Bool {
        do {
            return try await service.checkFavorite(itemType: itemType, itemId: itemId)
        } catch {
            return false
        }
    }
}

