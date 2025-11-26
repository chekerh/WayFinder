import Foundation

struct DestinationHelper {
    /// Convertit un code de destination (ex: "WF-BCN-001") en nom de ville et pays
    static func getDestinationName(from code: String) -> (city: String, country: String)? {
        // Extraire le code d'aéroport du format "WF-XXX-001"
        let components = code.split(separator: "-")
        guard components.count >= 2 else { return nil }
        
        let airportCode = String(components[1]).uppercased()
        
        // Mapping des codes d'aéroport vers ville et pays
        switch airportCode {
        case "BCN":
            return ("Barcelone", "Espagne")
        case "CDG", "ORY":
            return ("Paris", "France")
        case "LHR", "LGW":
            return ("Londres", "Royaume-Uni")
        case "JFK", "LGA":
            return ("New York", "États-Unis")
        case "LAX":
            return ("Los Angeles", "États-Unis")
        case "DXB":
            return ("Dubaï", "Émirats arabes unis")
        case "FCO":
            return ("Rome", "Italie")
        case "MAD":
            return ("Madrid", "Espagne")
        case "AMS":
            return ("Amsterdam", "Pays-Bas")
        case "FRA":
            return ("Francfort", "Allemagne")
        case "MUC":
            return ("Munich", "Allemagne")
        case "IST":
            return ("Istanbul", "Turquie")
        case "CAI":
            return ("Le Caire", "Égypte")
        case "TUN":
            return ("Tunis", "Tunisie")
        case "NRT", "HND":
            return ("Tokyo", "Japon")
        case "ICN":
            return ("Séoul", "Corée du Sud")
        case "PEK", "PKX":
            return ("Pékin", "Chine")
        case "PVG":
            return ("Shanghai", "Chine")
        case "SIN":
            return ("Singapour", "Singapour")
        case "BKK":
            return ("Bangkok", "Thaïlande")
        case "SYD":
            return ("Sydney", "Australie")
        case "MEL":
            return ("Melbourne", "Australie")
        default:
            return nil
        }
    }
    
    /// Retourne le nom complet de la destination (ex: "Barcelone, Espagne")
    static func getFullDestinationName(from code: String) -> String {
        if let (city, country) = getDestinationName(from: code) {
            return "\(city), \(country)"
        }
        return code
    }
    
    /// Retourne uniquement le nom de la ville
    static func getCityName(from code: String) -> String {
        if let (city, _) = getDestinationName(from: code) {
            return city
        }
        return code
    }
    
    /// Retourne uniquement le nom du pays
    static func getCountryName(from code: String) -> String? {
        if let (_, country) = getDestinationName(from: code) {
            return country
        }
        return nil
    }
}

