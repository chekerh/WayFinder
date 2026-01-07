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
        // D'abord essayer de parser le format standard "WF-XXX-001"
        if let (city, country) = getDestinationName(from: code) {
            return "\(city), \(country)"
        }
        
        // Si ça ne fonctionne pas, essayer de détecter un code d'aéroport dans d'autres formats
        // Par exemple: "dest_2", "7", "WF-BCN", etc.
        
        // Vérifier si c'est un code d'aéroport de 3 lettres (ex: "BCN", "CDG")
        let upperCode = code.uppercased()
        if upperCode.count == 3, let (city, country) = getDestinationName(from: "WF-\(upperCode)-001") {
            return "\(city), \(country)"
        }
        
        // Si la chaîne contient un code d'aéroport (ex: dans "dest_BCN" ou similaire)
        for airportCode in ["BCN", "CDG", "ORY", "LHR", "LGW", "JFK", "LGA", "LAX", "DXB", "FCO", "MAD", "AMS", "FRA", "MUC", "IST", "CAI", "TUN", "NRT", "HND", "ICN", "PEK", "PKX", "PVG", "SIN", "BKK", "SYD", "MEL"] {
            if upperCode.contains(airportCode), let (city, country) = getDestinationName(from: "WF-\(airportCode)-001") {
                return "\(city), \(country)"
            }
        }
        
        // Si aucun pattern ne correspond, retourner une version plus lisible du code
        // Remplacer "dest_" par rien et essayer de formater
        if code.lowercased().hasPrefix("dest_") {
            let cleaned = String(code.dropFirst(5))
            // Essayer de trouver un code d'aéroport dans le reste
            for airportCode in ["BCN", "CDG", "ORY", "LHR", "LGW", "JFK", "LGA", "LAX", "DXB", "FCO", "MAD", "AMS", "FRA", "MUC", "IST", "CAI", "TUN", "NRT", "HND", "ICN", "PEK", "PKX", "PVG", "SIN", "BKK", "SYD", "MEL"] {
                if cleaned.uppercased().contains(airportCode), let (city, country) = getDestinationName(from: "WF-\(airportCode)-001") {
                    return "\(city), \(country)"
                }
            }
        }
        
        // Fallback: retourner le code tel quel
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

