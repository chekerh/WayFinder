import SwiftUI

enum ThemeColors {
    static func background(_ scheme: ColorScheme) -> Color {
        scheme == .dark
        ? Color(.systemBackground)
        : Color(red: 0.90, green: 0.95, blue: 1.0)
    }

    static func surface(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(.secondarySystemBackground) : Color.white
    }

    static func elevatedSurface(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(.tertiarySystemBackground) : Color.white
    }

    static func primaryText(_ scheme: ColorScheme) -> Color {
        Color(.label)
    }

    static func secondaryText(_ scheme: ColorScheme) -> Color {
        Color(.secondaryLabel)
    }

    static func border(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? Color.white.opacity(0.25) : Color.gray.opacity(0.3)
    }

    static func accent() -> Color {
        Color(red: 0.18, green: 0.55, blue: 0.99) // WayFinderBlue = 0xFF1976D2
    }
    
    // Matching Android MaterialTheme primaryContainer
    static func primaryContainer(_ scheme: ColorScheme) -> Color {
        scheme == .dark
        ? Color(red: 0.05, green: 0.18, blue: 0.38) // WayFinderBlueDark = 0xFF0D47A1
        : Color(red: 0.26, green: 0.65, blue: 0.96) // WayFinderBlueLight = 0xFF42A5F5
    }
    
    // Matching Android MaterialTheme onPrimaryContainer
    static func onPrimaryContainer(_ scheme: ColorScheme) -> Color {
        scheme == .dark
        ? Color(red: 0.56, green: 0.79, blue: 0.98) // WayFinderBlueDark80 = 0xFF90CAF9
        : Color(red: 0.05, green: 0.18, blue: 0.38) // WayFinderBlueDark = 0xFF0D47A1
    }

    static func accentGradient(_ scheme: ColorScheme) -> LinearGradient {
        let start = scheme == .dark ? Color(red: 0.12, green: 0.32, blue: 0.60) : Color(red: 0.18, green: 0.55, blue: 0.99)
        let end = scheme == .dark ? Color(red: 0.05, green: 0.18, blue: 0.38) : Color(red: 0.34, green: 0.74, blue: 0.99)
        return LinearGradient(colors: [start, end], startPoint: .leading, endPoint: .trailing)
    }
}

extension Color {
    static let transparent = Color.clear
}

