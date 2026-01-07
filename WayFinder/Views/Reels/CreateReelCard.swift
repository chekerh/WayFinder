//
//  CreateReelCard.swift
//  WayFinder
//
//  Create Reel Card - First item in reels feed for creating new reels
//

import SwiftUI

struct CreateReelCard: View {
    let onTap: () -> Void
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        Button(action: onTap) {
            ZStack {
                // Background with gradient
                RoundedRectangle(cornerRadius: 16)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color(red: 0.098, green: 0.463, blue: 0.824).opacity(0.1),
                                Color(red: 0.259, green: 0.647, blue: 0.980).opacity(0.1)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .strokeBorder(
                                Color(red: 0.098, green: 0.463, blue: 0.824),
                                style: StrokeStyle(lineWidth: 2, dash: [8, 4])
                            )
                    )
                
                // Content
                VStack(spacing: 12) {
                    // Plus icon in circle
                    ZStack {
                        Circle()
                            .fill(Color(red: 0.098, green: 0.463, blue: 0.824))
                            .frame(width: 56, height: 56)
                        
                        Image(systemName: "plus")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(.white)
                    }
                    
                    // Text
                    Text("Create Reel")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                }
            }
        }
        .buttonStyle(.plain)
        .frame(width: 160, height: 240)
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 4, x: 0, y: 2)
    }
}

#Preview {
    CreateReelCard(onTap: {})
        .padding()
}

