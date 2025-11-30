import SwiftUI

struct OutfitSelectionView: View {
    @StateObject private var bookingViewModel = BookingViewModel()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                HStack {
                    Button(action: {
                        dismiss()
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(ThemeColors.primaryText(colorScheme))
                    }
                    .buttonStyle(.plain)
                    
                    Text("outfit_check_my_outfit")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                    
                    Spacer()
                    
                    Button(action: {
                        dismiss()
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24, weight: .medium))
                            .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 8)
                
                // Subtitle
                HStack {
                    Text("outfit_select_booking")
                        .font(.subheadline)
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 16)
                
                // Content
                if bookingViewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = bookingViewModel.errorMessage {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 48))
                            .foregroundColor(.orange)
                        Text("outfit_error")
                            .font(.headline)
                            .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        Text(error)
                            .font(.subheadline)
                            .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button(action: {
                            Task {
                                await bookingViewModel.loadHistory()
                            }
                        }) {
                            Text("outfit_retry")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 12)
                                .background(ThemeColors.accent())
                                .clipShape(Capsule())
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    let confirmedBookings = bookingViewModel.bookings.filter { $0.status == .confirmed }
                    
                    if confirmedBookings.isEmpty {
                        VStack(spacing: 16) {
                            Image(systemName: "camera.fill")
                                .font(.system(size: 64))
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                            Text("outfit_no_confirmed_bookings")
                                .font(.headline)
                                .foregroundStyle(ThemeColors.primaryText(colorScheme))
                            Text("outfit_no_confirmed_bookings_message")
                                .font(.subheadline)
                                .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 32)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        ScrollView(showsIndicators: false) {
                            LazyVStack(alignment: .leading, spacing: 12) {
                                ForEach(confirmedBookings) { booking in
                                    OutfitBookingCard(booking: booking)
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.bottom, 80)
                        }
                    }
                }
            }
        }
        .navigationBarHidden(true)
        .onAppear {
            Task {
                await bookingViewModel.loadHistory()
            }
        }
    }
}

struct OutfitBookingCard: View {
    let booking: Booking
    @Environment(\.colorScheme) private var colorScheme
    @State private var navigateToUpload = false
    
    private var destination: String {
        DestinationHelper.getFullDestinationName(from: booking.destination)
    }
    
    private var departureDate: String {
        if let dateString = booking.departureDate {
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd"
            if let date = formatter.date(from: dateString) {
                formatter.dateFormat = "dd MMM yyyy"
                return formatter.string(from: date)
            }
            // Try ISO8601 format if yyyy-MM-dd fails
            let isoFormatter = ISO8601DateFormatter()
            isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            if let date = isoFormatter.date(from: dateString) {
                formatter.dateFormat = "dd MMM yyyy"
                return formatter.string(from: date)
            }
        }
        return String(localized: "outfit_date_not_specified")
    }
    
    var body: some View {
        NavigationLink(destination: OutfitUploadView(bookingId: booking.id)) {
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(destination)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(ThemeColors.primaryText(colorScheme))
                        .lineLimit(1)
                    
                    Text("\(String(localized: "flight_departure")): \(departureDate)")
                        .font(.system(size: 14))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                    
                    Text("\(String(localized: "confirmation_number_label")): \(booking.confirmationNumber)")
                        .font(.system(size: 12))
                        .foregroundStyle(ThemeColors.secondaryText(colorScheme))
                }
                
                Spacer()
                
                Image(systemName: "camera.fill")
                    .font(.system(size: 32))
                    .foregroundColor(ThemeColors.accent())
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(ThemeColors.surface(colorScheme))
            )
            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.05), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}

