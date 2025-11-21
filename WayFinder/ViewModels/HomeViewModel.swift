import Foundation

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var greetingName: String = "Javier"
    @Published var profileImageUrl: String?
    @Published var regions: [RecommendationRegion] = []
    @Published var highlights: [RecommendationHighlight] = []
    @Published var selectedRegionId: String?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    // Pays de la région sélectionnée chargés depuis l'API
    @Published var countries: [Country] = []
    @Published var isLoadingCountries = false
    @Published var countriesErrorMessage: String?
    
    private let recommendationService: RecommendationService
    private let countryService: CountryService
    private let preferenceStorage: PreferenceStorage.Type
    private let userService: UserService
    private let profileImageService = ProfileImageService.shared
    private var profileImageObserver: NSObjectProtocol?
    private var userDefaultsObserver: NSObjectProtocol?
    
    init(recommendationService: RecommendationService = .shared,
         countryService: CountryService = .shared,
         preferenceStorage: PreferenceStorage.Type = PreferenceStorage.self,
         userService: UserService = .shared) {
        self.recommendationService = recommendationService
        self.countryService = countryService
        self.preferenceStorage = preferenceStorage
        self.userService = userService
        
        // Observer le service d'image centralisé
        profileImageUrl = profileImageService.profileImageUrl
        
        // Observer les changements du service centralisé
        profileImageObserver = NotificationCenter.default.addObserver(
            forName: NSNotification.Name("UserProfileImageDidUpdate"),
            object: nil,
            queue: .main
        ) { [weak self] notification in
            Task { @MainActor in
                guard let self else { return }
                // Mettre à jour depuis le service centralisé
                let newUrl = ProfileImageService.shared.profileImageUrl ?? notification.userInfo?["profileImageUrl"] as? String
                if self.profileImageUrl != newUrl {
                    self.profileImageUrl = newUrl
                    print("🔄 [HomeViewModel] Profile image updated from service: \(newUrl ?? "nil")")
                }
            }
        }
        
        userDefaultsObserver = NotificationCenter.default.addObserver(
            forName: UserDefaults.didChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                guard let self else { return }
                if let storedName = UserStorage.fetchDisplayName() {
                    self.greetingName = storedName
                }
                let newImageUrl = UserStorage.fetchProfileImageUrl()
                if self.profileImageUrl != newImageUrl {
                    self.profileImageUrl = newImageUrl
                }
            }
        }
        
        // Observer aussi les changements spécifiques au profil
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("UserProfileDidUpdate"),
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                guard let self else { return }
                self.profileImageUrl = UserStorage.fetchProfileImageUrl()
                if let storedName = UserStorage.fetchDisplayName() {
                    self.greetingName = storedName
                }
            }
        }
        
        // Observer les changements d'image de profil spécifiquement (doublon - déjà géré ci-dessus)
        // Cette observation est déjà faite dans le profileImageObserver ci-dessus
        
        if let storedName = UserStorage.fetchDisplayName() {
            greetingName = storedName
        }
        
        // Charger l'image depuis le service centralisé
        profileImageService.loadPersistedImage()
        profileImageUrl = profileImageService.profileImageUrl
        if let imageUrl = profileImageUrl {
            print("✅ [HomeViewModel] Loaded persisted profile image: \(imageUrl)")
        }
    }
    
    deinit {
        if let observer = profileImageObserver {
            NotificationCenter.default.removeObserver(observer)
        }
        if let observer = userDefaultsObserver {
            NotificationCenter.default.removeObserver(observer)
        }
    }
    
    func load() async {
        if isLoading { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        do {
            let preferenceId = preferenceStorage.fetchPreferenceId()
            let response = try await recommendationService.fetchRecommendations(preferenceId: preferenceId)
            greetingName = response.user.firstName
            regions = response.regions
            highlights = response.highlights
            if selectedRegionId == nil {
                selectedRegionId = response.regions.first?.id
                // Charger les pays de la première région
                if let firstRegionId = response.regions.first?.id {
                    await loadCountries(for: firstRegionId)
                }
            } else if let selectedRegionId, !response.regions.contains(where: { $0.id == selectedRegionId }) {
                self.selectedRegionId = response.regions.first?.id
                if let firstRegionId = response.regions.first?.id {
                    await loadCountries(for: firstRegionId)
                }
            }
        } catch {
            errorMessage = error.localizedDescription
        }
        
        await loadUserProfileIfNeeded()
    }
    
    /// Recharge l'image de profil depuis UserStorage
    func reloadProfileImage() {
        // Recharger depuis le service centralisé
        profileImageService.loadPersistedImage()
        let serviceImageUrl = profileImageService.profileImageUrl
        let cachedImageUrl = serviceImageUrl ?? UserStorage.fetchProfileImageUrl()
        
        // Filtrer l'image de test si elle existe
        let filteredUrl = (cachedImageUrl?.contains("pravatar.cc") == true) ? nil : cachedImageUrl
        
        // Toujours mettre à jour si l'URL a changé
        if profileImageUrl != filteredUrl {
            profileImageUrl = filteredUrl
            print("🔄 [HomeViewModel] Reloaded profile image: \(filteredUrl ?? "nil")")
        }
    }
    
    func selectRegion(_ region: RecommendationRegion) {
        selectedRegionId = region.id
        // Charger les pays de la région sélectionnée depuis l'API
        Task {
            await loadCountries(for: region.id)
        }
    }
    
    func loadCountries(for regionId: String) async {
        guard !isLoadingCountries else { return }
        isLoadingCountries = true
        defer { isLoadingCountries = false }
        countriesErrorMessage = nil
        
        print("🔄 [HomeViewModel] Loading countries for region: \(regionId)")
        do {
            countries = try await countryService.fetchCountries(regionId: regionId)
            print("✅ [HomeViewModel] Loaded \(countries.count) countries for region \(regionId)")
        } catch {
            print("❌ [HomeViewModel] Error loading countries: \(error.localizedDescription)")
            countriesErrorMessage = error.localizedDescription
            countries = [] // Réinitialiser en cas d'erreur
        }
    }

    private func loadUserProfileIfNeeded() async {
        do {
            let profile = try await userService.fetchProfile()
            let newImageUrl = profile.resolvedProfileImageUrl
            
            // Sauvegarder dans UserStorage pour la persistance (avec email)
            UserStorage.saveProfile(profile)
            
            // Mettre à jour le service centralisé
            if let imageUrl = newImageUrl {
                profileImageService.updateProfileImage(imageUrl, email: profile.email)
            }
            
            // Toujours mettre à jour l'URL de l'image
            profileImageUrl = newImageUrl ?? profileImageService.profileImageUrl ?? UserStorage.fetchProfileImageUrl()
            
            print("✅ [HomeViewModel] Profile loaded - Image URL: \(profileImageUrl ?? "nil")")
            
            // Si aucun prénom depuis les recommandations, utiliser celui du profil
            if greetingName == "Javier" || greetingName.isEmpty {
                if let firstName = profile.firstName, !firstName.isEmpty {
                    greetingName = firstName
                } else if let username = profile.username, !username.isEmpty {
                    greetingName = username
                }
            }
        } catch {
            print("⚠️ [HomeViewModel] Unable to load profile image: \(error.localizedDescription)")
            // En cas d'erreur, utiliser l'URL en cache depuis UserStorage
            let cachedUrl = UserStorage.fetchProfileImageUrl()
            if profileImageUrl != cachedUrl {
                profileImageUrl = cachedUrl
                print("🔄 [HomeViewModel] Using cached profile image: \(cachedUrl ?? "nil")")
            }
        }
    }
}
