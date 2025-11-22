import Foundation

@MainActor
final class ItineraryViewModel: ObservableObject {
    @Published var itineraries: [Itinerary] = []
    @Published var currentItinerary: Itinerary?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: ItineraryService
    
    nonisolated init(service: ItineraryService? = nil) {
        // Accéder à .shared depuis un contexte non isolé
        if let service = service {
            self.service = service
        } else {
            // Utiliser MainActor.assumeIsolated pour accéder à .shared (ItineraryService est Sendable)
            self.service = MainActor.assumeIsolated {
                ItineraryService.shared
            }
        }
    }
    
    func loadItineraries(includePublic: Bool = false) async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [ItineraryViewModel] Loading itineraries")
        do {
            itineraries = try await service.getAllItineraries(includePublic: includePublic)
            print("✅ [ItineraryViewModel] Loaded \(itineraries.count) itineraries")
        } catch {
            print("❌ [ItineraryViewModel] Error loading itineraries: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func loadItinerary(id: String) async {
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [ItineraryViewModel] Loading itinerary: \(id)")
        do {
            currentItinerary = try await service.getItinerary(id: id)
            print("✅ [ItineraryViewModel] Itinerary loaded")
        } catch {
            print("❌ [ItineraryViewModel] Error loading itinerary: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func createItinerary(
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        description: String? = nil,
        days: [DayPlanDto]? = nil,
        tags: [String]? = nil,
        isPublic: Bool? = nil,
        totalBudget: Double? = nil,
        currency: String? = nil
    ) async {
        print("🔄 [ItineraryViewModel] Creating itinerary")
        do {
            let itinerary = try await service.createItinerary(
                title: title,
                destination: destination,
                startDate: startDate,
                endDate: endDate,
                description: description,
                days: days,
                tags: tags,
                isPublic: isPublic,
                totalBudget: totalBudget,
                currency: currency
            )
            itineraries.append(itinerary)
            print("✅ [ItineraryViewModel] Itinerary created")
        } catch {
            print("❌ [ItineraryViewModel] Error creating itinerary: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    func deleteItinerary(id: String) async {
        print("🔄 [ItineraryViewModel] Deleting itinerary")
        do {
            try await service.deleteItinerary(id: id)
            itineraries.removeAll { $0.id == id }
            print("✅ [ItineraryViewModel] Itinerary deleted")
        } catch {
            print("❌ [ItineraryViewModel] Error deleting itinerary: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
}

