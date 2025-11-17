import Foundation

@MainActor
final class CountryService {
    static let shared = CountryService()
    private init() {}
    
    func fetchCountries(regionId: String) async throws -> [Country] {
        let builder = DefaultRequest(
            method: "GET",
            path: "regions/\(regionId)/countries"
        )
        return try await APIService.shared.request(builder, decodeTo: [Country].self)
    }
    
    func fetchCountryDetail(id: String) async throws -> CountryDetail {
        let builder = DefaultRequest(
            method: "GET",
            path: "countries/\(id)"
        )
        return try await APIService.shared.request(builder, decodeTo: CountryDetail.self)
    }
}


