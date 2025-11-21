import SwiftUI

/// Composant réutilisable pour afficher l'image de profil avec cache et persistance
struct ProfileImageView: View {
    let imageUrl: String?
    let size: CGFloat
    let placeholder: Image?
    
    @State private var loadedImageUrl: String?
    @State private var imageLoadError: Bool = false
    
    init(imageUrl: String?, size: CGFloat = 40, placeholder: Image? = nil) {
        self.imageUrl = imageUrl
        self.size = size
        self.placeholder = placeholder
    }
    
    var body: some View {
        Group {
            if let imageUrl = imageUrl, !imageUrl.isEmpty, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .onAppear {
                                loadedImageUrl = imageUrl
                                imageLoadError = false
                            }
                    case .failure(_):
                        // Placeholder en cas d'erreur
                        defaultPlaceholder
                            .onAppear {
                                imageLoadError = true
                            }
                    case .empty:
                        // Pendant le chargement
                        Circle()
                            .fill(Color.gray.opacity(0.2))
                            .overlay(
                                ProgressView()
                                    .scaleEffect(0.7)
                            )
                    @unknown default:
                        defaultPlaceholder
                    }
                }
                .id(imageUrl) // Forcer le rechargement si l'URL change
                .transition(.opacity)
            } else {
                defaultPlaceholder
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .onChange(of: imageUrl) { oldValue, newValue in
            // Réinitialiser l'état quand l'URL change
            if oldValue != newValue {
                loadedImageUrl = newValue
                imageLoadError = false
            }
        }
    }
    
    private var defaultPlaceholder: some View {
        Circle()
            .fill(Color.gray.opacity(0.3))
            .overlay(
                (placeholder ?? Image(systemName: "person.fill"))
                    .foregroundColor(.gray.opacity(0.6))
                    .font(.system(size: size * 0.45))
            )
    }
}

// MARK: - Preview
struct ProfileImageView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            ProfileImageView(imageUrl: nil, size: 40)
            ProfileImageView(imageUrl: "https://i.ibb.co/example.jpg", size: 80)
        }
        .padding()
    }
}

