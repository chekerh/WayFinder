import SwiftUI

struct ReviewsSection: View {
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = ReviewViewModel()
    let itemType: String
    let itemId: String
    @State private var showReviewDialog = false
    @State private var selectedReview: Review?
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Avis")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(ThemeColors.primaryText(colorScheme))
                Spacer()
                Button("Ajouter un avis") {
                    selectedReview = nil
                    showReviewDialog = true
                }
                .font(.subheadline)
                .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
            }
            
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .frame(height: 100)
            } else if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .foregroundColor(.red)
                    .padding(.vertical, 16)
            } else {
                if let stats = viewModel.reviewStats {
                    ReviewStatsDisplay(stats: stats)
                }
                
                if viewModel.reviews.isEmpty {
                    Text("Aucun avis pour le moment. Soyez le premier à laisser un avis!")
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        .padding(.vertical, 16)
                } else {
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(viewModel.reviews) { review in
                                ReviewCard(review: review)
                            }
                        }
                    }
                    .frame(height: 400)
                }
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 4)
        .task {
            await loadReviews()
        }
        .sheet(isPresented: $showReviewDialog) {
            ReviewDialog(
                itemType: itemType,
                itemId: itemId,
                existingReview: selectedReview,
                onDismiss: { showReviewDialog = false },
                onSubmit: { rating, comment in
                    Task {
                        guard let itemTypeEnum = ReviewItemType(rawValue: itemType) else {
                            print("Invalid item type: \(itemType)")
                            showReviewDialog = false
                            return
                        }
                        
                        await viewModel.createReview(
                            itemType: itemTypeEnum,
                            itemId: itemId,
                            rating: rating,
                            comment: comment
                        )
                        await loadReviews()
                        showReviewDialog = false
                    }
                }
            )
        }
    }
    
    private func loadReviews() async {
        // Convert String to ReviewItemType
        guard let itemTypeEnum = ReviewItemType(rawValue: itemType) else {
            print("Invalid item type: \(itemType)")
            return
        }
        await viewModel.loadReviews(itemType: itemTypeEnum, itemId: itemId)
        await viewModel.loadReviewStats(itemType: itemTypeEnum, itemId: itemId)
    }
}

struct ReviewStatsDisplay: View {
    let stats: ReviewStats
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(String(format: "%.1f", stats.averageRating))
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(Color(red: 0.098, green: 0.463, blue: 0.824))
                
                StarRating(rating: Int(stats.averageRating.rounded()))
                    .frame(height: 20)
                
                Spacer()
                
                Text("\(stats.totalReviews) avis")
                    .font(.caption)
                    .foregroundStyle(ThemeColors.secondaryText(.light))
            }
            
            // Rating distribution
            if let distribution = stats.ratingDistribution {
                VStack(spacing: 4) {
                    ForEach((1...5).reversed(), id: \.self) { stars in
                        HStack {
                            Text("\(stars)")
                                .font(.caption)
                                .frame(width: 16)
                            
                            Image(systemName: "star.fill")
                                .foregroundColor(Color(red: 1.0, green: 0.843, blue: 0.0))
                                .font(.system(size: 12))
                            
                            GeometryReader { geometry in
                                ZStack(alignment: .leading) {
                                    Rectangle()
                                        .fill(Color.gray.opacity(0.2))
                                        .frame(height: 8)
                                    
                                    Rectangle()
                                        .fill(Color(red: 1.0, green: 0.843, blue: 0.0))
                                        .frame(
                                            width: stats.totalReviews > 0 
                                                ? geometry.size.width * (Double(distribution[stars] ?? 0) / Double(stats.totalReviews))
                                                : 0,
                                            height: 8
                                        )
                                }
                            }
                            .frame(height: 8)
                            
                            Text("\(distribution[stars] ?? 0)")
                                .font(.caption)
                                .foregroundStyle(ThemeColors.secondaryText(.light))
                                .frame(width: 24)
                        }
                    }
                }
            }
        }
    }
}

struct ReviewCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let review: Review
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Circle()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 32, height: 32)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Utilisateur")
                        .font(.system(size: 14, weight: .bold))
                    
                    StarRating(rating: review.rating)
                        .frame(height: 12)
                }
                
                Spacer()
            }
            
            if let comment = review.comment, !comment.isEmpty {
                Text(comment)
                    .font(.system(size: 14))
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            }
            
            if let createdAt = review.createdAt {
                Text(formatTimestamp(createdAt))
                    .font(.caption)
                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.white)
        )
        .shadow(color: Color.black.opacity(0.02), radius: 2, x: 0, y: 2)
    }
    
    private func formatTimestamp(_ date: Date) -> String {
        let displayFormatter = DateFormatter()
        displayFormatter.dateFormat = "dd MMM yyyy"
        return displayFormatter.string(from: date)
    }
}

struct StarRating: View {
    let rating: Int
    var body: some View {
        HStack(spacing: 2) {
            ForEach(0..<5) { index in
                Image(systemName: index < rating ? "star.fill" : "star")
                    .foregroundColor(index < rating ? Color(red: 1.0, green: 0.843, blue: 0.0) : Color.gray.opacity(0.3))
                    .font(.system(size: 12))
            }
        }
    }
}

struct ReviewDialog: View {
    let itemType: String
    let itemId: String
    let existingReview: Review?
    let onDismiss: () -> Void
    let onSubmit: (Int, String?) -> Void
    
    @State private var rating: Int
    @State private var comment: String
    
    init(itemType: String, itemId: String, existingReview: Review?, onDismiss: @escaping () -> Void, onSubmit: @escaping (Int, String?) -> Void) {
        self.itemType = itemType
        self.itemId = itemId
        self.existingReview = existingReview
        self.onDismiss = onDismiss
        self.onSubmit = onSubmit
        _rating = State(initialValue: existingReview?.rating ?? 0)
        _comment = State(initialValue: existingReview?.comment ?? "")
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Text("Note")
                    .font(.headline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                HStack(spacing: 16) {
                    ForEach(1...5, id: \.self) { star in
                        Button {
                            rating = star
                        } label: {
                            Image(systemName: star <= rating ? "star.fill" : "star")
                                .font(.system(size: 32))
                                .foregroundColor(star <= rating ? Color(red: 1.0, green: 0.843, blue: 0.0) : Color.gray)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                
                TextField("Commentaire (optionnel)", text: $comment, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(4...10)
                
                Spacer()
                
                Button {
                    onSubmit(rating, comment.isEmpty ? nil : comment)
                } label: {
                    Text(existingReview != nil ? "Modifier" : "Publier")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(rating > 0 ? Color(red: 0.098, green: 0.463, blue: 0.824) : Color.gray)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(rating == 0)
            }
            .padding()
            .navigationTitle(existingReview != nil ? "Modifier votre avis" : "Ajouter un avis")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") {
                        onDismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}
