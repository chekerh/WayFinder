import SwiftUI

struct NotificationView: View {
    @StateObject private var viewModel = NotificationViewModel()
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.errorMessage {
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
            } else if viewModel.notifications.isEmpty {
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
            } else {
                VStack(spacing: 0) {
                    // Header
                    HStack {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Notifications")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            
                            if viewModel.unreadCount > 0 {
                                Text("\(viewModel.unreadCount) non lue\(viewModel.unreadCount > 1 ? "s" : "")")
                                    .font(.subheadline)
                                    .foregroundStyle(ThemeColors.accent())
                            } else {
                                Text("Toutes lues")
                                    .font(.subheadline)
                                    .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            }
                        }
                        
                        Spacer()
                        
                        if viewModel.unreadCount > 0 {
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
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 20)
                    .padding(.bottom, 24)
                    
                    // Liste des notifications
                    ScrollView {
                        VStack(spacing: 12) {
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
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 100)
                    }
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadNotifications()
            await viewModel.loadUnreadCount()
        }
        .refreshable {
            await viewModel.loadNotifications()
            await viewModel.loadUnreadCount()
        }
    }
}

struct NotificationCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let notification: Notification
    let onRead: () -> Void
    let onDelete: () -> Void
    
    var body: some View {
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
        .onTapGesture {
            if !notification.isRead {
                onRead()
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive, action: onDelete) {
                Label("Supprimer", systemImage: "trash")
            }
        }
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

