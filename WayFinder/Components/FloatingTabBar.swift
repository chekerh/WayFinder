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

        case .activity: return "house.fill"

        case .favorites: return "heart"

        case .explore: return "bubble.left.and.bubble.right"

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

    

    // Custom background with a center notch

    struct WavyTabBarBackground: Shape {

        let cornerRadius: CGFloat

        let notchRadius: CGFloat

        

        func path(in rect: CGRect) -> Path {

            var p = Path()

            let topY = rect.minY

            let midX = rect.midX

            

            // Start at bottom-left

            p.move(to: CGPoint(x: rect.minX, y: rect.maxY))

            // Up left edge with top-left rounded corner

            p.addLine(to: CGPoint(x: rect.minX, y: topY + cornerRadius))

            p.addQuadCurve(

                to: CGPoint(x: rect.minX + cornerRadius, y: topY),

                control: CGPoint(x: rect.minX, y: topY)

            )

            

            // Move along the top edge to the notch start

            let notchStartX = midX - notchRadius

            p.addLine(to: CGPoint(x: notchStartX, y: topY))

            // Concave downward arc for the notch

            p.addArc(

                center: CGPoint(x: midX, y: topY + notchRadius),

                radius: notchRadius,

                startAngle: .degrees(200),

                endAngle: .degrees(-20),

                clockwise: true

            )

            

            // Continue to top-right with rounded corner

            p.addLine(to: CGPoint(x: rect.maxX - cornerRadius, y: topY))

            p.addQuadCurve(

                to: CGPoint(x: rect.maxX, y: topY + cornerRadius),

                control: CGPoint(x: rect.maxX, y: topY)

            )

            

            // Down right edge and close along bottom

            p.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))

            p.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))

            p.closeSubpath()

            return p

        }

    }

    

    private let bubbleColor = LinearGradient(

        colors: [Color(red: 0.25, green: 0.65, blue: 0.98), Color(red: 0.07, green: 0.35, blue: 0.82)],

        startPoint: .topLeading,

        endPoint: .bottomTrailing

    )

    

    var body: some View {

        ZStack(alignment: .top) {

            WavyTabBarBackground(cornerRadius: 24, notchRadius: 16)

                .fill(ThemeColors.surface(colorScheme))

                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.06), radius: 12, x: 0, y: -2)

                .ignoresSafeArea(edges: .bottom)

            

            HStack(spacing: 0) {

                ForEach(FloatingTab.allCases) { tab in

                    tabButton(for: tab)

                }

            }

            .frame(maxWidth: .infinity)

            .padding(.top, 4)

            .padding(.bottom, 8)

            

            // Removed center floating button

        }

        .frame(height: 64)

    }

    

    private func tabButton(for tab: FloatingTab) -> some View {

        let isSelected = selection == tab

        return Button {

            withAnimation(.spring(response: 0.5, dampingFraction: 0.6, blendDuration: 0.3)) {

                selection = tab

                jumpToggle.toggle()

            }

        } label: {

            ZStack {

                if isSelected {

                    Circle()

                        .fill(bubbleColor)

                        .frame(width: 40, height: 40)

                        .offset(y: jumpToggle ? -4 : -8)

                        .matchedGeometryEffect(id: "bubble", in: bubbleNamespace)

                        .shadow(color: Color(red: 0.07, green: 0.35, blue: 0.82).opacity(0.45), radius: 12, x: 0, y: 8)

                }

                Image(systemName: tab.iconName)

                    .font(.system(size: 18, weight: .semibold))

                    .foregroundColor(isSelected ? .white : ThemeColors.accent())

                    .frame(width: 28, height: 28)

                    .offset(y: isSelected ? (jumpToggle ? -4 : -8) : 0)

            }

            .frame(width: 40, height: 40)

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
