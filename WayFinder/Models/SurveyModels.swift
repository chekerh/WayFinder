import Foundation

struct SurveyPreferenceRequest: Encodable {
    let vacationType: String?
    let vacationFeeling: String?
    let teleportDestination: String?
    let transportMode: String?
    let hobby: String?
    let timeTravelDestination: String?
    let budget: String?
    let activities: String?
    let morningOrNight: String?
    let spiritualAnimal: String?
    let historicalEra: String?
}

struct SurveyPreferenceResponse: Decodable {
    let id: String
}
