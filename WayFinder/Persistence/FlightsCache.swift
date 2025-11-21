import Foundation

struct CachedFlights: Codable {
    let destinations: [FlightDestination]
    let updatedAt: TimeInterval
    let source: String?
}

final class FlightsCache {
    static let shared = FlightsCache()
    private init() {}
    
    private let cacheKey = "com.wayfinder.flights_cache"
    private let updatedAtKey = "com.wayfinder.flights_cache_updated_at"
    private let sourceKey = "com.wayfinder.flights_cache_source"
    
    func store(_ destinations: [FlightDestination], source: String? = nil) {
        guard !destinations.isEmpty else { return }
        
        if let encoded = try? JSONEncoder().encode(destinations) {
            UserDefaults.standard.set(encoded, forKey: cacheKey)
            UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: updatedAtKey)
            if let source = source {
                UserDefaults.standard.set(source, forKey: sourceKey)
            }
        }
    }
    
    func read() -> CachedFlights? {
        guard let data = UserDefaults.standard.data(forKey: cacheKey),
              let destinations = try? JSONDecoder().decode([FlightDestination].self, from: data) else {
            return nil
        }
        
        let updatedAt = UserDefaults.standard.double(forKey: updatedAtKey)
        let source = UserDefaults.standard.string(forKey: sourceKey)
        
        return CachedFlights(destinations: destinations, updatedAt: updatedAt, source: source)
    }
    
    func clear() {
        UserDefaults.standard.removeObject(forKey: cacheKey)
        UserDefaults.standard.removeObject(forKey: updatedAtKey)
        UserDefaults.standard.removeObject(forKey: sourceKey)
    }
}

