import SwiftUI

struct NotificationView: View {
    @StateObject private var viewModel = NotificationViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @State private var navigateToPostId: String? = nil
    @State private var showDeleteAllConfirmation = false
    
    var body: some View {
        Group {
            if viewModel.isLoading {
                ZStack {
                    ThemeColors.background(colorScheme)
                        .ignoresSafeArea()
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            } else if let error = viewModel.errorMessage {
                ZStack {
                    ThemeColors.background(colorScheme)
                        .ignoresSafeArea()
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 48))
                            .foregroundColor(.orange)
                        Text("Erreur")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        Text(error)
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button(action: {
                            Task {
                                await viewModel.loadNotifications()
                            }
                        }) {
                            Text("Réessayer")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 12)
                                .background(ThemeColors.accent())
                                .clipShape(Capsule())
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            } else if viewModel.notifications.isEmpty {
                ZStack {
                    ThemeColors.background(colorScheme)
                        .ignoresSafeArea()
                    VStack(spacing: 16) {
                        Image(systemName: "bell.slash")
                            .font(.system(size: 48))
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        Text("Aucune notification")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        Text("Vous n'avez pas encore de notifications")
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            } else {
                ScrollView(showsIndicators: true) {
                    VStack(spacing: 0) {
                        // Header
                        HStack {
                            Text("Notifications")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            Spacer()
                            
                            Button(action: {
                                Task {
                                    await viewModel.markAllAsRead()
                                }
                            }) {
                                Text("Tout marquer comme lu")
                                    .font(.caption)
                                    .foregroundColor(ThemeColors.accent())
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 20)
                        .padding(.bottom, 16)
                        
                        // Delete All Notifications Button
                        Button(action: {
                            showDeleteAllConfirmation = true
                        }) {
                            HStack {
                                Image(systemName: "trash.fill")
                                    .font(.system(size: 14, weight: .semibold))
                                Text("Supprimer toutes les notifications")
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color.red)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 16)
                        
                        // Liste des notifications
                        LazyVStack(spacing: 12) {
                            ForEach(viewModel.notifications) { notification in
                                NotificationCard(
                                    notification: notification,
                                    onRead: {
                                        Task {
                                            await viewModel.markAsRead(notificationId: notification.id)
                                        }
                                    },
                                    onDelete: {
                                        Task {
                                            await viewModel.deleteNotification(notificationId: notification.id)
                                        }
                                    },
                                    onTap: {
                                        handleNotificationTap(notification: notification)
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 8)
                        .padding(.bottom, 100)
                    }
                }
                .background(ThemeColors.background(colorScheme))
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadNotifications()
        }
        .refreshable {
            await viewModel.loadNotifications()
        }
        .alert("Supprimer toutes les notifications", isPresented: $showDeleteAllConfirmation) {
            Button("Annuler", role: .cancel) {
                showDeleteAllConfirmation = false
            }
            Button("Confirmer", role: .destructive) {
                Task {
                    await viewModel.deleteAllNotifications()
                    showDeleteAllConfirmation = false
                }
            }
        } message: {
            Text("Êtes-vous sûr de vouloir supprimer toutes les notifications ? Cette action est irréversible.")
        }
    }
    
    private func handleNotificationTap(notification: Notification) {
        // Marquer comme lu
        Task {
            await viewModel.markAsRead(notificationId: notification.id)
        }
        
        // Naviguer selon le type de notification
        let type = notification.type.rawValue
        if type == "post_commented" || type == "post_liked" || type == "journey_commented" || type == "journey_liked" {
            if let postId = notification.data?.postId {
                // Envoyer une notification pour naviguer
                NotificationCenter.default.post(
                    name: NSNotification.Name("FCMNotificationTapped"),
                    object: nil,
                    userInfo: [
                        "type": type,
                        "postId": postId,
                        "commentId": notification.data?.commentId ?? ""
                    ]
                )
                // Fermer la vue des notifications
                dismiss()
            }
        }
    }
}

struct NotificationCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let notification: Notification
    let onRead: () -> Void
    let onDelete: () -> Void
    let onTap: () -> Void
    
    @State private var offset: CGFloat = 0
    
    var body: some View {
        ZStack(alignment: .trailing) {
            // Bouton de suppression (visible quand on swipe vers la gauche)
            HStack {
                Spacer()
                Button(action: {
                    withAnimation(.spring()) {
                        offset = 0
                    }
                    // Attendre un peu avant de supprimer pour voir l'animation
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        onDelete()
                    }
                }) {
                    Image(systemName: "trash")
                        .foregroundColor(.white)
                        .font(.system(size: 16, weight: .medium))
                        .frame(width: 60)
                        .frame(maxHeight: .infinity)
                        .background(
                            Color.red
                                .clipShape(Rectangle())
                        )
                }
                .buttonStyle(.plain)
            }
            .opacity(offset < -10 ? 1 : 0) // Visible seulement quand on swipe
            .allowsHitTesting(offset < -10) // Désactiver les interactions quand invisible
            
            // Contenu de la notification
            HStack(alignment: .top, spacing: 12) {
                // Icône selon le type
                Image(systemName: iconForType(notification.type))
                    .font(.system(size: 20))
                    .foregroundColor(notification.isRead ? ThemeColors.secondaryText(colorScheme) : ThemeColors.accent())
                    .frame(width: 32, height: 32)
                    .background(
                        Circle()
                            .fill((notification.isRead ? ThemeColors.secondaryText(colorScheme) : ThemeColors.accent()).opacity(0.1))
                    )
                
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(notification.title)
                            .font(.system(size: 16, weight: notification.isRead ? .regular : .semibold))
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        
                        Spacer()
                        
                        if !notification.isRead {
                            Circle()
                                .fill(ThemeColors.accent())
                                .frame(width: 8, height: 8)
                        }
                    }
                    
                    Text(notification.message)
                        .font(.system(size: 14))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                        .lineLimit(2)
                    
                    if let createdAt = notification.createdAt {
                        Text(formatDate(createdAt))
                            .font(.caption)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    }
                }
                
                Spacer()
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(notification.isRead ? ThemeColors.surface(colorScheme) : ThemeColors.surface(colorScheme).opacity(0.7))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(notification.isRead ? Color.clear : ThemeColors.accent().opacity(0.3), lineWidth: 1)
            )
            .contentShape(Rectangle())
            .offset(x: offset)
            .gesture(
                DragGesture()
                    .onChanged { value in
                        if value.translation.width < 0 {
                            // Swipe vers la gauche
                            offset = max(value.translation.width, -60)
                        } else if offset < 0 {
                            // Permettre de revenir en arrière
                            offset = min(0, offset + value.translation.width)
                        }
                    }
                    .onEnded { value in
                        if value.translation.width < -30 || offset < -30 {
                            // Ouvrir le bouton de suppression
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                offset = -60
                            }
                        } else {
                            // Fermer le bouton de suppression
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                offset = 0
                            }
                        }
                    }
            )
            .onTapGesture {
                // Appeler onTap pour gérer la navigation
                onTap()
            }
        }
        .clipped() // Empêcher le bouton de dépasser les bords
        .contentShape(Rectangle())
    }
    
    private func iconForType(_ type: NotificationType) -> String {
        switch type {
        case .bookingConfirmed, .bookingUpdated:
            return "checkmark.circle.fill"
        case .bookingCancelled:
            return "xmark.circle.fill"
        case .priceAlert:
            return "tag.fill"
        case .paymentSuccess:
            return "checkmark.seal.fill"
        case .paymentFailed:
            return "exclamationmark.triangle.fill"
        case .tripReminder:
            return "calendar"
        case .postLiked, .journeyLiked:
            return "heart.fill"
        case .postCommented, .journeyCommented:
            return "message.fill"
        case .general:
            return "bell.fill"
        }
    }
    
    private func formatDate(_ date: Date) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .abbreviated
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

struct NotificationView_Previews: PreviewProvider {
    static var previews: some View {
        NotificationView()
    }
}

