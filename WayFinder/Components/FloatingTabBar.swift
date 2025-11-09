import SwiftUI

enum FloatingTab: String, CaseIterable, Identifiable {
    case activity
    case favorites
    case explore
    case alerts
    case profile
    
    var id: String { rawValue }
    
    var iconName: String {
        switch self {
        case .activity: return "figure.strengthtraining.traditional"
        case .favorites: return "heart"
        case .explore: return "mappin.and.ellipse"
        case .alerts: return "bell"
        case .profile: return "person"
        }
    }
    
    var labelKey: LocalizedStringKey {
        switch self {
        case .activity: return "tab_activity"
        case .favorites: return "tab_favorites"
        case .explore: return "tab_explore"
        case .alerts: return "tab_alerts"
        case .profile: return "tab_profile"
        }
    }
}

struct FloatingTabBar: View {
    @Environment(\.colorScheme) private var colorScheme
    @Binding var selection: FloatingTab
    @Namespace private var bubbleNamespace
    @State private var jumpToggle = false
    
    private let bubbleColor = LinearGradient(
        colors: [Color(red: 0.25, green: 0.65, blue: 0.98), Color(red: 0.07, green: 0.35, blue: 0.82)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
    
    var body: some View {
        HStack(spacing: 0) {
            ForEach(FloatingTab.allCases) { tab in
                tabButton(for: tab)
            }
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 12)
        .background(
            Capsule()
                .fill(ThemeColors.surface(colorScheme))
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.4 : 0.08), radius: 16, x: 0, y: 8)
        )
        .padding(.horizontal, 24)
    }
    
    private func tabButton(for tab: FloatingTab) -> some View {
        let isSelected = selection == tab
        return Button {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.6, blendDuration: 0.3)) {
                selection = tab
                jumpToggle.toggle()
            }
        } label: {
            VStack(spacing: 6) {
                ZStack {
                    if isSelected {
                        Circle()
                            .fill(bubbleColor)
                            .frame(width: 58, height: 58)
                            .offset(y: jumpToggle ? -6 : -12)
                            .matchedGeometryEffect(id: "bubble", in: bubbleNamespace)
                            .shadow(color: Color(red: 0.07, green: 0.35, blue: 0.82).opacity(0.45), radius: 12, x: 0, y: 8)
                    }
                    Image(systemName: tab.iconName)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(isSelected ? .white : ThemeColors.accent())
                        .frame(width: 44, height: 44)
                }
                .frame(height: 44)
                .frame(maxWidth: .infinity)
                
                Text(tab.labelKey)
                    .font(.caption2)
                    .fontWeight(isSelected ? .semibold : .regular)
                    .foregroundColor(isSelected ? ThemeColors.accent() : ThemeColors.secondaryText(colorScheme))
                    .lineLimit(1)
                    .opacity(isSelected ? 1 : 0)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
        .contentShape(Rectangle())
    }
}

struct FloatingTabBar_Previews: PreviewProvider {
    static var previews: some View {
        FloatingTabBar(selection: .constant(.explore))
            .previewLayout(.sizeThatFits)
            .padding()
    }
}

