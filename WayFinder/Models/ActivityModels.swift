//
//  ActivityModels.swift
//  WayFinder
//
//  Models for activities from OpenTripMap API
//

import Foundation

// MARK: - OpenTripMap Place Model

struct OpenTripMapPlace: Codable, Identifiable {
    let xid: String
    let name: String
    let kinds: String?
    let rate: Int?
    let wikidata: String?
    let osm: String?
    let point: OpenTripMapPoint?
    let preview: OpenTripMapPreview?
    let wikipedia_extracts: OpenTripMapWikiExtract?
    
    var id: String { xid }
    
    enum CodingKeys: String, CodingKey {
        case xid, name, kinds, rate, wikidata, osm, point, preview
        case wikipedia_extracts
    }
}

struct OpenTripMapPoint: Codable {
    let lon: Double
    let lat: Double
}

struct OpenTripMapPreview: Codable {
    let source: String?
    let width: Int?
    let height: Int?
}

struct OpenTripMapWikiExtract: Codable {
    let title: String?
    let text: String?
    let html: String?
}

// MARK: - Activities API Response

struct ActivitiesResponse: Codable {
    let data: [OpenTripMapPlace]
    let meta: ActivitiesMeta?
}

struct ActivitiesMeta: Codable {
    let source: String?
    let total: Int?
}

// MARK: - Activity Feed Models (from backend)

struct ActivityFeedItem: Codable, Identifiable {
    let id: String
    let name: String
    let description: String?
    let imageUrl: String?
    let category: String?
    let rating: Double?
    let price: Double?
    let currency: String?
    let location: String?
    let latitude: Double?
    let longitude: Double?
    let duration: String?
    let provider: String?
    
    enum CodingKeys: String, CodingKey {
        case id, name, description, imageUrl, category
        case rating, price, currency, location
        case latitude, longitude, duration, provider
    }
}

struct ActivityFeedResponse: Codable {
    let feed: [ActivityFeedItem]
    let nearbyPlaces: [OpenTripMapPlace]?
    let categories: [String]?
    let meta: [String: String]?
    
    enum CodingKeys: String, CodingKey {
        case feed
        case nearbyPlaces = "nearby_places"
        case categories, meta
    }
}

// MARK: - Activity Category

enum ActivityCategory: String, CaseIterable {
    case cultural = "cultural"
    case natural = "natural"
    case architecture = "architecture"
    case historic = "historic"
    case religion = "religion"
    case sport = "sport"
    case amusement = "amusement"
    case industrial = "industrial"
    
    var displayName: String {
        switch self {
        case .cultural: return String(localized: "Culturel")
        case .natural: return String(localized: "Nature")
        case .architecture: return String(localized: "Architecture")
        case .historic: return String(localized: "Historique")
        case .religion: return String(localized: "Religieux")
        case .sport: return String(localized: "Sport")
        case .amusement: return String(localized: "Divertissement")
        case .industrial: return String(localized: "Industriel")
        }
    }
    
    var icon: String {
        switch self {
        case .cultural: return "theatermasks.fill"
        case .natural: return "leaf.fill"
        case .architecture: return "building.columns.fill"
        case .historic: return "scroll.fill"
        case .religion: return "star.fill"
        case .sport: return "sportscourt.fill"
        case .amusement: return "party.popper.fill"
        case .industrial: return "gearshape.fill"
        }
    }
}

