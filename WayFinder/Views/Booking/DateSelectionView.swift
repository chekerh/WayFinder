//
//  DateSelectionView.swift
//  WayFinder
//
//  Created by sarrachmek on 4/11/2025.
//

import SwiftUI

struct DateSelectionView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.locale) private var locale
    
    @State private var selectedDate = Date()
    @State private var selectedTimeSlot: String?
    
    private let timeSlots = ["07:00", "07:45", "08:00", "08:45", "09:30"]
    
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.90, green: 0.95, blue: 1.0),
                    Color(red: 0.86, green: 0.93, blue: 1.0)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    headerCard
                    
                    calendarCard
                    
                    VStack(alignment: .leading, spacing: 12) {
                        Text("date_select_time")
                            .font(.headline)
                            .foregroundColor(Color(red: 0.12, green: 0.20, blue: 0.38))
                        
                        VStack(spacing: 12) {
                            ForEach(timeSlots, id: \.self) { slot in
                                timeSlotButton(for: slot)
                            }
                        }
                    }
                    .padding(.horizontal, 4)
                    
                    Button {
                        // Placeholder pour l'action de réservation
                    } label: {
                        Text("date_confirm_button")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(
                                LinearGradient(
                                    colors: [
                                        Color(red: 1.0, green: 0.77, blue: 0.19),
                                        Color(red: 1.0, green: 0.66, blue: 0.10)
                                    ],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .shadow(color: Color.black.opacity(0.12), radius: 12, x: 0, y: 6)
                    }
                    .padding(.top, 8)
                    
                    if let selectedTimeSlot {
                        Text("date_selection_summary \(formattedDate) \(selectedTimeSlot)")
                            .font(.subheadline)
                            .foregroundColor(Color(red: 0.28, green: 0.34, blue: 0.43))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 32)
                .padding(.bottom, 40)
            }
        }
    }
    
    private var headerCard: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(Color.white.opacity(colorScheme == .dark ? 0.12 : 0.95))
                .overlay(
                    RoundedRectangle(cornerRadius: 32, style: .continuous)
                        .stroke(Color.white.opacity(0.35), lineWidth: 1)
                )
                .shadow(color: Color.black.opacity(0.08), radius: 16, x: 0, y: 10)
            
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("date_reservation_title")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(Color(red: 0.11, green: 0.20, blue: 0.42))
                        
                        Text("date_reservation_subtitle")
                            .font(.subheadline)
                            .foregroundColor(Color(red: 0.28, green: 0.34, blue: 0.43))
                            .lineSpacing(4)
                    }
                    
                    Spacer()
                    
                    Image("calandrier")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 92, height: 92)
                        .shadow(color: Color.black.opacity(0.08), radius: 8, x: 0, y: 4)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 28)
        }
        .frame(height: 160)
    }
    
    private var calendarCard: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(Color.white)
                .shadow(color: Color.black.opacity(0.06), radius: 14, x: 0, y: 8)
            
            VStack(spacing: 16) {
                DatePicker(
                    "date_picker_label",
                    selection: $selectedDate,
                    displayedComponents: [.date]
                )
                .labelsHidden()
                .datePickerStyle(.graphical)
                .tint(Color(red: 0.25, green: 0.51, blue: 0.95))
            }
            .padding(20)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 360)
    }
    
    private func timeSlotButton(for slot: String) -> some View {
        let isSelected = selectedTimeSlot == slot
        
        return Button {
            withAnimation(.easeInOut(duration: 0.2)) {
                selectedTimeSlot = slot
            }
        } label: {
            HStack {
                Text(slot)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(isSelected ? .white : Color(red: 0.12, green: 0.20, blue: 0.38))
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.white)
                }
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(timeSlotBackgroundStyle(isSelected: isSelected))
                    .shadow(color: Color.black.opacity(isSelected ? 0.16 : 0.06), radius: isSelected ? 14 : 8, x: 0, y: isSelected ? 12 : 6)
            )
        }
        .buttonStyle(.plain)
    }
    
    private func timeSlotBackgroundStyle(isSelected: Bool) -> AnyShapeStyle {
        if isSelected {
            return AnyShapeStyle(
                LinearGradient(
                    colors: [
                        Color(red: 0.27, green: 0.57, blue: 0.99),
                        Color(red: 0.08, green: 0.39, blue: 0.91)
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
        } else {
            return AnyShapeStyle(Color.white)
        }
    }
    
    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.dateFormat = DateFormatter.dateFormat(fromTemplate: "MMMM d, yyyy", options: 0, locale: locale)
        return formatter.string(from: selectedDate)
    }
}

struct DateSelectionView_Previews: PreviewProvider {
    static var previews: some View {
        DateSelectionView()
    }
}

