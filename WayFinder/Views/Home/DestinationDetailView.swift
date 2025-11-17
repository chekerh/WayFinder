//
//  DestinationDetailView.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

struct DestinationDetailView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    headerImage
                    detailCard
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 32)
            }
        }
        .navigationBarHidden(true)
    }

    // MARK: - Header
    private var headerImage: some View {
        ZStack(alignment: .topLeading) {
            Image("rome")
                .resizable()
                .scaledToFill()
                .frame(height: 320)
                .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
                .shadow(color: .black.opacity(0.25), radius: 18, x: 0, y: 10)

            HStack {
                CircleButton(systemName: "chevron.left") {
                    dismiss()
                }
                Spacer()
                HStack(spacing: 16) {
                    CircleButton(systemName: "square.and.arrow.up") {}
                    CircleButton(systemName: "map") {}
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)

            VStack {
                Spacer()
                VStack(spacing: 16) {
                    Circle()
                        .fill(Color.white.opacity(0.4))
                        .frame(width: 40, height: 40)
                    Circle()
                        .fill(Color.white.opacity(0.4))
                        .frame(width: 40, height: 40)
                    Circle()
                        .fill(Color.white.opacity(0.9))
                        .frame(width: 40, height: 40)
                }
                .padding(.trailing, 24)
                .padding(.bottom, 32)
                .frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
    }

    // MARK: - Detail card
    private var detailCard: some View {
        VStack(alignment: .leading, spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Word Trade Center")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))

                Text("Le Bahrain World Trade Center est un complexe de deux tours jumelles de 240 mètres situé à Manama, à Bahreïn, inauguré en 2008. Inspiré par les voiles arabes, c’est le premier bâtiment à intégrer de grandes éoliennes.")
                    .font(.subheadline)
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            }

            VStack(alignment: .leading, spacing: 12) {
                Text("Équipements disponibles")
                    .font(.headline)
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))

                HStack(spacing: 32) {
                    EquipmentItem(icon: "sun.max.fill", label: "ensoleillé")
                    EquipmentItem(icon: "fork.knife", label: "Resto")
                    EquipmentItem(icon: "wifi", label: "Wi-Fi\ngratuit")
                    EquipmentItem(icon: "cup.and.saucer.fill", label: "Café")
                    EquipmentItem(icon: "briefcase.fill", label: "Affaires")
                }
            }

            HStack(spacing: 16) {
                Button(action: {}) {
                    Text("Comparer les prix")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .background(
                    LinearGradient(
                        colors: [Color(red: 0.12, green: 0.46, blue: 0.98),
                                 Color(red: 0.19, green: 0.67, blue: 0.99)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(Capsule())

                Button(action: {}) {
                    Text("Réserver")
                        .font(.headline)
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .background(
                    LinearGradient(
                        colors: [Color(red: 1.0, green: 0.82, blue: 0.47),
                                 Color(red: 0.99, green: 0.69, blue: 0.26)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(Capsule())
            }
        }
        .padding(24)
        .background(
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(ThemeColors.surface(colorScheme))
        )
    }
}

// MARK: - Subviews
private struct CircleButton: View {
    let systemName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 44, height: 44)
                .background(Color.black.opacity(0.35))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
    }
}

private struct EquipmentItem: View {
    let icon: String
    let label: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 22))
                .foregroundColor(.gray)
            Text(label)
                .font(.caption)
                .multilineTextAlignment(.center)
                .foregroundColor(.gray)
        }
    }
}

struct DestinationDetailView_Previews: PreviewProvider {
    static var previews: some View {
        DestinationDetailView()
    }
}


