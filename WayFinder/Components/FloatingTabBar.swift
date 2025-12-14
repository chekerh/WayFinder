import SwiftUI



enum FloatingTab: String, CaseIterable, Identifiable {

    case activity

    case mapMemories

    case outfit

    case alerts

    case explore

    

    var id: String { rawValue }

    

    var iconName: String {

        switch self {

        case .activity: return "house.fill"

        case .mapMemories: return "map.fill"

        case .outfit: return "camera.fill"

        case .alerts: return "airplane"

        case .explore: return "bubble.left.and.bubble.right"

        }

    }

    

    var labelKey: LocalizedStringKey {

        switch self {

        case .activity: return "tab_activity"

        case .mapMemories: return "map_memories_title"

        case .outfit: return "tab_outfit"

        case .alerts: return "tab_bookings"

        case .explore: return "tab_explore"

        }

    }

}



struct FloatingTabBar: View {

    @Environment(\.colorScheme) private var colorScheme

    @Binding var selection: FloatingTab

    @Namespace private var bubbleNamespace

    @State private var bubbleScale: CGFloat = 1.0
    @State private var bubbleOffset: CGFloat = -8  // Normal position
    @State private var isBouncing: Bool = false
    @State private var previousSelection: FloatingTab?

    

    // Simple rounded rectangle background (no notch)

    struct WavyTabBarBackground: Shape {

        let cornerRadius: CGFloat

        

        func path(in rect: CGRect) -> Path {

            var p = Path()

            let topY = rect.minY

            

            // Start at bottom-left

            p.move(to: CGPoint(x: rect.minX, y: rect.maxY))

            // Up left edge with top-left rounded corner

            p.addLine(to: CGPoint(x: rect.minX, y: topY + cornerRadius))

            p.addQuadCurve(

                to: CGPoint(x: rect.minX + cornerRadius, y: topY),

                control: CGPoint(x: rect.minX, y: topY)

            )

            

            // Move along the top edge (straight line, no notch)

            p.addLine(to: CGPoint(x: rect.maxX - cornerRadius, y: topY))

            

            // Top-right rounded corner

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
    
    // Smooth bouncing transition animation - combines horizontal movement with vertical bounce (400ms)
    private let bounceTransitionAnimation = Animation.spring(
        response: 0.4,
        dampingFraction: 0.7,
        blendDuration: 0.2
    )
    
    // Smooth water drop movement animation - horizontal transition (350ms)
    private let waterDropAnimation = Animation.spring(
        response: 0.35,
        dampingFraction: 0.75,
        blendDuration: 0.2
    )
    
    // Pronounced bounce animation when landing (bubble landing effect)
    private let landingBounce = Animation.spring(
        response: 0.3,
        dampingFraction: 0.5,
        blendDuration: 0.15
    )
    
    // Gentle settle bounce after landing
    private let settleBounce = Animation.spring(
        response: 0.25,
        dampingFraction: 0.65,
        blendDuration: 0.1
    )

    

    var body: some View {

        ZStack(alignment: .top) {

            // Rectangle blanc arrondi simple (sans notch)
            WavyTabBarBackground(cornerRadius: 24)
                .fill(ThemeColors.surface(colorScheme))
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.06), radius: 12, x: 0, y: -2)
                .ignoresSafeArea(edges: .bottom)

            

            // HStack with proper alignment to ensure all icons are perfectly level
            HStack(alignment: .center, spacing: 0) {

                ForEach(FloatingTab.allCases) { tab in

                    tabButton(for: tab)

                }

            }
            .frame(maxWidth: .infinity)
            .frame(height: 64, alignment: .center)
            .padding(.top, 4)
            .padding(.bottom, 8)

        }

        .frame(height: 64)
        .onChange(of: selection) { _, newSelection in
            // Only trigger bounce animation if selection actually changed
            guard previousSelection != newSelection else { return }
            previousSelection = newSelection
            
            // Bouncing bubble animation: jump up during transition, then bounce on landing
            isBouncing = true
            
            // Phase 1: Jump up while moving horizontally (bubble bounces up during transition)
            // The matchedGeometryEffect handles horizontal movement, we add vertical bounce
            withAnimation(bounceTransitionAnimation) {
                bubbleScale = 1.1  // Slightly grow during jump
                bubbleOffset = -16  // Jump up above the tab bar
            }
            
            // Phase 2: Land on icon with impact bounce
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                withAnimation(landingBounce) {
                    bubbleScale = 1.2  // Scale up on impact (15% larger)
                    bubbleOffset = -2  // Impact position
                }
            }
            
            // Phase 3: Rebound upward (bubble bounces back)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                withAnimation(settleBounce) {
                    bubbleScale = 0.95  // Slightly compress
                    bubbleOffset = -14  // Bounce up
                }
            }
            
            // Phase 4: Settle bounce (final settling)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                withAnimation(settleBounce) {
                    bubbleScale = 1.05  // Slight overshoot
                    bubbleOffset = -6  // Settle down
                }
            }
            
            // Phase 5: Final position (fully settled)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.65) {
                withAnimation(settleBounce) {
                    bubbleScale = 1.0  // Normal size
                    bubbleOffset = -8  // Final resting position
                    isBouncing = false
                }
            }
        }
        .onAppear {
            // Initialize previous selection
            previousSelection = selection
        }

    }

    

    private func tabButton(for tab: FloatingTab) -> some View {

        let isSelected = selection == tab

        return Button {

            // Trigger jumping bubble animation
            // The bubble will start from below and jump up to the new icon
            selection = tab

        } label: {

            ZStack(alignment: .center) {

                // Animated blue bubble with water drop effect
                if isSelected {

                    Circle()

                        .fill(bubbleColor)

                        .frame(width: 40, height: 40)

                        .scaleEffect(bubbleScale)

                        .offset(y: bubbleOffset)

                        .matchedGeometryEffect(id: "bubble", in: bubbleNamespace)

                        .shadow(color: Color(red: 0.07, green: 0.35, blue: 0.82).opacity(isBouncing ? 0.7 : 0.5), radius: isBouncing ? 20 : 14, x: 0, y: isBouncing ? 10 : 6)
                        
                        // Add inner glow effect for water drop appearance
                        .overlay(
                            Circle()
                                .fill(
                                    RadialGradient(
                                        colors: [
                                            Color.white.opacity(0.4),
                                            Color.white.opacity(0.1),
                                            Color.clear
                                        ],
                                        center: UnitPoint(x: 0.3, y: 0.3),
                                        startRadius: 3,
                                        endRadius: 18
                                    )
                                )
                                .frame(width: 40, height: 40)
                                .blendMode(.overlay)
                        )
                        
                        // Add outer glow for water drop effect
                        .overlay(
                            Circle()
                                .stroke(
                                    LinearGradient(
                                        colors: [
                                            Color.white.opacity(0.3),
                                            Color.clear
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ),
                                    lineWidth: 1.5
                                )
                                .frame(width: 40, height: 40)
                        )

                }

                Image(systemName: tab.iconName)

                    .font(.system(size: 18, weight: .semibold))

                    .foregroundColor(isSelected ? .white : ThemeColors.accent())

                    .frame(width: 28, height: 28)

                    .offset(y: isSelected ? bubbleOffset : 0)
                    
                    // Add scale animation to icon that follows bubble bounce (synchronized)
                    .scaleEffect(isSelected ? min(bubbleScale * 0.88, 1.0) : 1.0)
                    
                    // Add smooth transition for icon color
                    .animation(waterDropAnimation, value: isSelected)

            }
            .frame(width: 40, height: 40)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())

        }

        .buttonStyle(.plain)

    }

}



struct FloatingTabBar_Previews: PreviewProvider {

    static var previews: some View {

        FloatingTabBar(selection: .constant(.explore))

            .previewLayout(.sizeThatFits)

            .padding()

    }

}
