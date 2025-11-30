import Foundation

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var greetingName: String = "" // Start empty, will be loaded from profile
    @Published var profileImageUrl: String?
    @Published var regions: [RecommendationRegion] = []
    @Published var highlights: [RecommendationHighlight] = []
    @Published var selectedRegionId: String?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var onboardingCompleted: Bool = true
    @Published var onboardingSkipped: Bool = false
    @Published var showOnboardingAlert: Bool = false
    
    // Pays de la région sélectionnée chargés depuis l'API
    @Published var countries: [Country] = []
    @Published var isLoadingCountries = false
    @Published var countriesErrorMessage: String?
    
    private let recommendationService: RecommendationService
    private let countryService: CountryService
    private let preferenceStorage: PreferenceStorage.Type
    nonisolated(unsafe) private var userService: UserService!
    private let profileImageService: ProfileImageService
    private var profileImageObserver: NSObjectProtocol?
    private var userDefaultsObserver: NSObjectProtocol?
    
    nonisolated init(recommendationService: RecommendationService? = nil,
         countryService: CountryService? = nil,
         preferenceStorage: PreferenceStorage.Type = PreferenceStorage.self,
         userService: UserService? = nil,
         profileImageService: ProfileImageService? = nil) {
        // Assigner recommendationService
        if let recommendationService = recommendationService {
            self.recommendationService = recommendationService
        } else {
            // Utiliser MainActor.assumeIsolated pour accéder à .shared (RecommendationService est Sendable)
            self.recommendationService = MainActor.assumeIsolated {
                RecommendationService.shared
            }
        }
        
        // Assigner countryService
        if let countryService = countryService {
            self.countryService = countryService
        } else {
            // Utiliser MainActor.assumeIsolated pour accéder à .shared (CountryService est Sendable)
            self.countryService = MainActor.assumeIsolated {
                CountryService.shared
            }
        }
        
        self.preferenceStorage = preferenceStorage
        
        // Assigner userService (UserService n'est pas @MainActor)
        // Utiliser nonisolated(unsafe) pour contourner l'isolation MainActor
        if let userService = userService {
            nonisolated(unsafe) let captured = userService
            self.userService = captured
        } else {
            nonisolated(unsafe) let captured = UserService.shared
            self.userService = captured
        }
        
        // Assigner profileImageService (ProfileImageService est @MainActor)
        let finalProfileImageService: ProfileImageService
        if let profileImageService = profileImageService {
            finalProfileImageService = profileImageService
        } else {
            // Utiliser MainActor.assumeIsolated pour accéder à .shared (ProfileImageService est Sendable)
            finalProfileImageService = MainActor.assumeIsolated {
                ProfileImageService.shared
            }
        }
        self.profileImageService = finalProfileImageService
        
        // Initialiser les propriétés MainActor dans un Task
        Task { @MainActor in
            
            // Charger le nom depuis UserStorage immédiatement
            if let storedName = UserStorage.fetchDisplayName(), !storedName.isEmpty {
                self.greetingName = storedName
                print("✅ [HomeViewModel] Loaded greeting name from storage: \(storedName)")
            } else {
                print("⚠️ [HomeViewModel] No stored name found, will load from profile")
                // Load profile immediately to get the user's name
                await loadUserProfileIfNeeded()
            }
            
            // Charger l'image depuis le service centralisé
            finalProfileImageService.loadPersistedImage()
            self.profileImageUrl = finalProfileImageService.profileImageUrl
            if let imageUrl = self.profileImageUrl {
                print("✅ [HomeViewModel] Loaded persisted profile image: \(imageUrl)")
            }
            
            // Observer les changements du service centralisé
            self.profileImageObserver = NotificationCenter.default.addObserver(
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
            
            self.userDefaultsObserver = NotificationCenter.default.addObserver(
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
        
        // Load profile first to get the user's name
        await loadUserProfileIfNeeded()
        
        do {
            let preferenceId = preferenceStorage.fetchPreferenceId()
            let response = try await recommendationService.fetchRecommendations(preferenceId: preferenceId)
            // Only use firstName from recommendations if we don't have a name yet
            if greetingName.isEmpty, !response.user.firstName.isEmpty {
                greetingName = response.user.firstName
                // Save it to UserStorage
                UserStorage.saveProfile(displayName: response.user.firstName, profileImageUrl: nil, email: nil)
                print("✅ [HomeViewModel] Updated greeting name from recommendations: \(response.user.firstName)")
            }
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

    func loadUserProfileIfNeeded() async {
        do {
            let profile = try await userService.fetchProfile()
            let newImageUrl = profile.resolvedProfileImageUrl
            
            // Déterminer le meilleur nom d'affichage à sauvegarder
            let displayNameToSave: String? = {
                if let firstName = profile.firstName, !firstName.isEmpty {
                    return firstName
                } else if let username = profile.username, !username.isEmpty {
                    return username
                } else {
                    return profile.displayNameValue.isEmpty ? nil : profile.displayNameValue
                }
            }()
            
            // Sauvegarder dans UserStorage pour la persistance (avec email et le meilleur nom)
            if let displayName = displayNameToSave {
                UserStorage.saveProfile(displayName: displayName, profileImageUrl: newImageUrl, email: profile.email)
            } else {
                UserStorage.saveProfile(profile)
            }
            
            // Mettre à jour le service centralisé
            if let imageUrl = newImageUrl {
                profileImageService.updateProfileImage(imageUrl, email: profile.email)
            }
            
            // Toujours mettre à jour l'URL de l'image
            profileImageUrl = newImageUrl ?? profileImageService.profileImageUrl ?? UserStorage.fetchProfileImageUrl()
            
            // Mettre à jour l'état d'onboarding
            onboardingCompleted = profile.onboardingCompleted ?? true
            onboardingSkipped = profile.onboardingSkipped ?? false
            
            // Afficher l'alerte si l'onboarding n'est pas complété (comme Android)
            // Android affiche le badge si onboardingSkipped == true, mais on affiche le popup si onboardingCompleted == false
            if !onboardingCompleted {
                // Attendre un peu pour que l'écran soit complètement chargé avant d'afficher le popup
                try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 secondes
                showOnboardingAlert = true
                print("📢 [HomeViewModel] Showing onboarding alert (onboardingCompleted: \(onboardingCompleted))")
            }
            
            print("✅ [HomeViewModel] Profile loaded - Image URL: \(profileImageUrl ?? "nil"), Onboarding completed: \(onboardingCompleted), Skipped: \(onboardingSkipped)")
            
            // Mettre à jour le nom d'accueil depuis le profil si pas déjà défini
            if greetingName.isEmpty {
                if let firstName = profile.firstName, !firstName.isEmpty {
                    greetingName = firstName
                    // Sauvegarder dans UserStorage pour la prochaine fois
                    UserStorage.saveProfile(displayName: firstName, profileImageUrl: nil, email: profile.email)
                    print("✅ [HomeViewModel] Updated greeting name from profile firstName: \(firstName)")
                } else if let username = profile.username, !username.isEmpty {
                    greetingName = username
                    // Sauvegarder dans UserStorage pour la prochaine fois
                    UserStorage.saveProfile(displayName: username, profileImageUrl: nil, email: profile.email)
                    print("✅ [HomeViewModel] Updated greeting name from profile username: \(username)")
                } else {
                    let displayName = profile.displayNameValue
                    // displayNameValue always returns a value (at minimum "Utilisateur"), so check if it's meaningful
                    if !displayName.isEmpty && displayName != "Utilisateur" {
                        greetingName = displayName
                        // Sauvegarder dans UserStorage pour la prochaine fois
                        UserStorage.saveProfile(displayName: displayName, profileImageUrl: nil, email: profile.email)
                        print("✅ [HomeViewModel] Updated greeting name from profile displayName: \(displayName)")
                    }
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
