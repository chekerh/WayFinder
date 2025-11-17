import SwiftUI

struct CountryListView: View {
    let region: RecommendationRegion
    @StateObject private var viewModel = CountryListViewModel()
    @Environment(\.colorScheme) private var colorScheme
    
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
            } else {
                List(viewModel.countries) { country in
                    NavigationLink(
                        destination: CountryDetailView(countryId: country.id)
                    ) {
                        CountryRow(country: country)
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle(region.title)
        .task {
            await viewModel.load(regionId: region.id)
        }
    }
}

private struct CountryRow: View {
    let country: Country
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        HStack(spacing: 12) {
            if let urlString = country.thumbnailImageUrl,
               let url = URL(string: urlString) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    ThemeColors.surface(colorScheme)
                }
                .frame(width: 64, height: 64)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(country.name)
                    .font(.headline)
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                Text(country.summary)
                    .font(.subheadline)
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    .lineLimit(2)
            }
            
            Spacer()
        }
        .padding(.vertical, 4)
    }
}


