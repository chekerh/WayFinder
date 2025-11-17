import SwiftUI

struct CountryDetailView: View {
    @StateObject private var viewModel: CountryDetailViewModel
    @Environment(\.colorScheme) private var colorScheme
    
    init(countryId: String) {
        _viewModel = StateObject(wrappedValue: CountryDetailViewModel(countryId: countryId))
    }
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            if viewModel.isLoading {
                ProgressView()
            } else if let error = viewModel.errorMessage {
                Text(error)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .padding()
            } else if let detail = viewModel.detail {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                            if let urlString = detail.heroImageUrl,
                               let url = URL(string: urlString) {
                                AsyncImage(url: url) { phase in
                                    switch phase {
                                    case .empty:
                                        Rectangle()
                                            .fill(ThemeColors.surface(colorScheme))
                                            .overlay {
                                                ProgressView()
                                            }
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 300)
                                    case .success(let image):
                                        image
                                            .resizable()
                                            .scaledToFill()
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 300)
                                    case .failure(_):
                                        Rectangle()
                                            .fill(ThemeColors.surface(colorScheme))
                                            .overlay {
                                                Image(systemName: "photo")
                                                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                            }
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 300)
                                    @unknown default:
                                        Rectangle()
                                            .fill(ThemeColors.surface(colorScheme))
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 300)
                                    }
                                }
                                .clipped()
                                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                            }
                            
                            Text(detail.name)
                                .font(.largeTitle.bold())
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            Text(detail.summary)
                                .font(.headline)
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            
                            if !detail.description.isEmpty {
                                Text(detail.description)
                                    .font(.body)
                                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            }
                            
                            infoRow(label: "Meilleure période", value: detail.bestSeason)
                            infoRow(label: "Monnaie", value: detail.currency)
                            infoRow(label: "Langue", value: detail.language)
                            infoRow(label: "Fuseau horaire", value: detail.timezone)
                            
                            if let images = detail.extraImages, !images.isEmpty {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Galerie")
                                        .font(.headline)
                                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                                    
                                    ScrollView(.horizontal, showsIndicators: false) {
                                        HStack(spacing: 12) {
                                            ForEach(images, id: \.self) { urlString in
                                                if let url = URL(string: urlString) {
                                                    AsyncImage(url: url) { phase in
                                                        switch phase {
                                                        case .empty:
                                                            Rectangle()
                                                                .fill(ThemeColors.surface(colorScheme))
                                                                .overlay {
                                                                    ProgressView()
                                                                        .scaleEffect(0.5)
                                                                }
                                                                .frame(width: 160, height: 120)
                                                        case .success(let image):
                                                            image
                                                                .resizable()
                                                                .scaledToFill()
                                                                .frame(width: 160, height: 120)
                                                        case .failure(_):
                                                            Rectangle()
                                                                .fill(ThemeColors.surface(colorScheme))
                                                                .overlay {
                                                                    Image(systemName: "photo")
                                                                        .foregroundColor(ThemeColors.secondaryText(colorScheme))
                                                                }
                                                                .frame(width: 160, height: 120)
                                                        @unknown default:
                                                            Rectangle()
                                                                .fill(ThemeColors.surface(colorScheme))
                                                                .frame(width: 160, height: 120)
                                                        }
                                                    }
                                                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                                    .clipped()
                                                }
                                            }
                                        }
                                        .padding(.vertical, 4)
                                    }
                                }
                            }
                            
                            // Bouton Réserver
                            NavigationLink(destination: BookingOffersView(destination: detail.name)) {
                                HStack(spacing: 8) {
                                    Image(systemName: "airplane")
                                        .font(.subheadline)
                                    Text("Réserver")
                                        .font(.subheadline.bold())
                                }
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(ThemeColors.accent())
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }
                            .padding(.top, 8)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 8)
                    }
                    .scrollIndicators(.hidden)
                    .safeAreaPadding(.top, 8)
                    .safeAreaPadding(.bottom, 20)
                } else {
                    Text("Aucune donnée")
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
        }
        .navigationTitle("Détails")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.load()
        }
    }
    
    @ViewBuilder
    private func infoRow(label: String, value: String?) -> some View {
        if let value, !value.isEmpty {
            HStack {
                Text(label)
                    .font(.subheadline)
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                Spacer()
                Text(value)
                    .font(.subheadline)
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
            }
        }
    }
}


