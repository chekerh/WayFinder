import SwiftUI

struct DestinationVideoCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let destination: DestinationWithVideoStatus
    let onGenerateClick: () -> Void
    let onVideoClick: () -> Void
    
    @State private var showErrorDialog = false
    
    var body: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 8) {
                Text(destination.destination)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                
                Text("\(destination.imageCount) \(String(localized: "destination_video_photos"))")
                    .font(.system(size: 13))
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                
                // Status indicator
                statusView
            }
            
            Spacer()
            
            // Action button
            actionButton
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(ThemeColors.surface(colorScheme))
        )
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 5, x: 0, y: 2)
        .alert(String(localized: "destination_video_error_details") + " - \(destination.destination)", isPresented: $showErrorDialog) {
            Button("Fermer") {
                showErrorDialog = false
            }
            Button(String(localized: "destination_video_retry")) {
                showErrorDialog = false
                onGenerateClick()
            }
        } message: {
            if let errorMessage = destination.errorMessage {
                VStack(alignment: .leading, spacing: 8) {
                    Text(String(localized: "destination_video_error_message"))
                    Text(errorMessage)
                }
            }
        }
    }
    
    @ViewBuilder
    private var statusView: some View {
        switch destination.videoStatus {
        case "ready":
            HStack(spacing: 4) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 14))
                    .foregroundColor(Color(red: 0.298, green: 0.686, blue: 0.314))
                Text(String(localized: "destination_video_ready"))
                    .font(.system(size: 12))
                    .foregroundColor(Color(red: 0.298, green: 0.686, blue: 0.314))
            }
        case "processing":
            HStack(spacing: 4) {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Color(red: 1.0, green: 0.596, blue: 0.0)))
                    .scaleEffect(0.7)
                Text(String(localized: "destination_video_generating"))
                    .font(.system(size: 12))
                    .foregroundColor(Color(red: 1.0, green: 0.596, blue: 0.0))
            }
        case "failed":
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 14))
                        .foregroundColor(Color(red: 0.91, green: 0.12, blue: 0.39))
                    Text(String(localized: "destination_video_failed"))
                        .font(.system(size: 12))
                        .foregroundColor(Color(red: 0.91, green: 0.12, blue: 0.39))
                }
                if let errorMessage = destination.errorMessage, !errorMessage.isEmpty {
                    Text(errorMessage.prefix(50) + (errorMessage.count > 50 ? "..." : ""))
                        .font(.system(size: 11))
                        .foregroundColor(Color(red: 0.91, green: 0.12, blue: 0.39))
                        .lineLimit(1)
                        .onTapGesture {
                            showErrorDialog = true
                        }
                }
            }
        default:
            Text(String(localized: "destination_video_not_generated"))
                .font(.system(size: 12))
                .foregroundColor(ThemeColors.secondaryText(colorScheme))
        }
    }
    
    @ViewBuilder
    private var actionButton: some View {
        switch destination.videoStatus {
        case "ready":
            Button(action: onVideoClick) {
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 24))
                    .foregroundColor(Color(red: 0.29, green: 0.56, blue: 0.89))
            }
        case "processing":
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
        default:
            Button(action: onGenerateClick) {
                HStack(spacing: 6) {
                    Image(systemName: "video.fill")
                        .font(.system(size: 14))
                    Text(String(localized: "destination_video_generate"))
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color(red: 0.29, green: 0.56, blue: 0.89))
                )
            }
        }
    }
}

