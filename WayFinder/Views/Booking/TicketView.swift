//
//  TicketView.swift
//  WayFinder
//
//  Created by sarrachmek on 16/11/2025.
//

import SwiftUI

struct TicketView: View {
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        ZStack {
            ThemeColors.background(colorScheme)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                header
                ticketCard
                buttonsRow
                geminiFooter
                bottomActions
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 12)
        }
        .navigationBarHidden(true)
    }
    
    private var header: some View {
        HStack {
            Text("Votre Ticket")
                .font(.system(size: 26, weight: .bold))
                .foregroundStyle(ThemeColors.primaryText(colorScheme))
            Spacer()
            Image(systemName: "person.badge.key.fill")
                .font(.system(size: 26, weight: .semibold))
                .foregroundColor(ThemeColors.accent())
        }
    }
    
    private var ticketCard: some View {
        VStack(spacing: 0) {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.blue)
                .frame(height: 56)
                .overlay(
                    Text("000-234-XXX-09A")
                        .font(.headline.weight(.bold))
                        .foregroundColor(.white)
                )
            
            VStack(spacing: 16) {
                HStack(alignment: .center) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("From")
                            .foregroundColor(.white.opacity(0.8))
                        Text("New York")
                            .foregroundColor(.white)
                        Text("JFK")
                            .foregroundColor(.white)
                            .font(.headline.weight(.bold))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    
                    Image(systemName: "airplane")
                        .font(.system(size: 34, weight: .bold))
                        .foregroundColor(.white)
                    
                    VStack(alignment: .trailing, spacing: 6) {
                        Text("To")
                            .foregroundColor(.white.opacity(0.8))
                        Text("London")
                            .foregroundColor(.white)
                        Text("LHR")
                            .foregroundColor(.white)
                            .font(.headline.weight(.bold))
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)
                }
                
                dottedLine
                
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Aug 19,\n2025\n09:00 AM")
                            .foregroundColor(.white)
                            .font(.footnote)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("Aug 19,\n2025\n04:00 PM")
                            .foregroundColor(.white)
                            .font(.footnote)
                            .multilineTextAlignment(.trailing)
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)
                }
                
                dottedLine
                
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        captionRow(title: "Passenger", value: "Andrew")
                    }
                    Spacer()
                    VStack(alignment: .leading, spacing: 2) {
                        captionRow(title: "Flight", value: "A 0897")
                    }
                    Spacer()
                    VStack(alignment: .leading, spacing: 2) {
                        captionRow(title: "Seat", value: "17 A")
                    }
                    Spacer()
                    VStack(alignment: .leading, spacing: 2) {
                        captionRow(title: "Gate/Terminal", value: "12/2A")
                    }
                }
                .foregroundColor(.white)
                .font(.footnote)
            }
            .padding(16)
            .background(
                LinearGradient(gradient: Gradient(colors: [Color.gray.opacity(0.25), Color.gray.opacity(0.2)]),
                               startPoint: .topLeading, endPoint: .bottomTrailing)
            )
            
            Rectangle()
                .fill(ThemeColors.surface(colorScheme))
                .frame(height: 160)
                .overlay(barcode)
        }
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(ThemeColors.surface(colorScheme), lineWidth: 1))
    }
    
    private var dottedLine: some View {
        Rectangle()
            .fill(Color.blue.opacity(0.6))
            .frame(height: 2)
            .overlay(
                HStack(spacing: 6) {
                    ForEach(0..<24, id: \.self) { _ in
                        Capsule().fill(Color.white.opacity(0.9)).frame(width: 8, height: 2)
                    }
                }
            )
    }
    
    private func captionRow(title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).opacity(0.8)
            Text(value).font(.headline.weight(.semibold))
        }
    }
    
    private var barcode: some View {
        VStack(spacing: 16) {
            // Simulated barcode with black/white bars
            HStack(spacing: 2) {
                ForEach(0..<60, id: \.self) { i in
                    Rectangle()
                        .fill(i % 2 == 0 ? Color.black : Color.clear)
                        .frame(width: 3, height: 120)
                }
            }
            Text("0 0 2 2 2 4 4 4 4 4 1 1 1")
                .font(.caption)
                .foregroundColor(.black.opacity(0.7))
                .tracking(3)
        }
        .padding(.horizontal, 26)
    }
    
    private var buttonsRow: some View {
        HStack(spacing: 16) {
            Button(action: {}) {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.down.doc.fill")
                    Text("Save PDF")
                        .font(.headline)
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            }
            .background(ThemeColors.accent())
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            
            Button(action: {}) {
                Text("Modifier")
                    .font(.headline)
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .background(Color.yellow)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }
    
    private var geminiFooter: some View {
        Image("gemini")
            .resizable()
            .scaledToFit()
            .frame(height: 36)
            .opacity(0.9)
            .padding(.vertical, 8)
    }
    
    private var bottomActions: some View {
        HStack(spacing: 16) {
            Button(action: {}) {
                Text("Decline")
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .background(Color.pink.opacity(0.3))
            .clipShape(Capsule())
            
            Button(action: {}) {
                HStack {
                    Text("Buy $1,200.00")
                    Image(systemName: "chevron.right")
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            }
            .background(ThemeColors.accent())
            .clipShape(Capsule())
        }
    }
}

struct TicketView_Previews: PreviewProvider {
    static var previews: some View {
        TicketView()
    }
}


