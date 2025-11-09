import Foundation

struct NestError: Decodable {
    let statusCode: Int
    let message: Message
    let error: String?

    enum Message: Decodable {
        case single(String)
        case multiple([String])

        init(from decoder: Decoder) throws {
            let container = try decoder.singleValueContainer()
            if let string = try? container.decode(String.self) {
                self = .single(string)
            } else if let array = try? container.decode([String].self) {
                self = .multiple(array)
            } else {
                self = .single("Erreur inconnue.")
            }
        }

        var text: String {
            switch self {
            case .single(let string):
                return string
            case .multiple(let array):
                return array.joined(separator: "\n")
            }
        }
    }
}

