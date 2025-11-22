//
//  AccommodationSelectionView.swift
//  WayFinder
//
//  Created by sarrachmek on 8/11/2025.
//

import SwiftUI
import AVFoundation

struct AccommodationSelectionView: View {
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var languageManager: LanguageManager
    
    @State private var selectedOption: String?
    private let options: [String] = [
        "accommodation_option_tent",
        "accommodation_option_glamping",
        "accommodation_option_hotel",
        "accommodation_option_hostel"
    ]
    
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.94, green: 0.97, blue: 1.0),
                    Color(red: 0.88, green: 0.94, blue: 1.0)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 32) {
                    Text("accommodation_title")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(Color(red: 0.13, green: 0.24, blue: 0.44))
                        .padding(.top, 36)
                    
                    LoopingVideoView(resourceName: "plan", fileExtension: "mp4")
                        .frame(width: 200, height: 200)
                        .clipShape(Circle())
                        .shadow(color: Color.black.opacity(0.18), radius: 16, x: 0, y: 10)
                    
                    VStack(spacing: 18) {
                        ForEach(options, id: \.self) { key in
                            optionRow(for: key)
                        }
                    }
                    
                    Button {
                        // TODO: handle validation action
                    } label: {
                        Text("accommodation_confirm_button")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(
                                LinearGradient(
                                    colors: [
                                        Color(red: 0.27, green: 0.52, blue: 0.97),
                                        Color(red: 0.11, green: 0.42, blue: 0.92)
                                    ],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                            .shadow(color: Color.black.opacity(0.12), radius: 14, x: 0, y: 8)
                    }
                    .padding(.vertical, 4)
                }
                .padding(.horizontal, 24)
                .padding(.top, 24)
                .padding(.bottom, 24)
            }
        }
        .environmentObject(languageManager)
        .environment(\.locale, languageManager.locale)
        .environment(\.layoutDirection, languageManager.layoutDirection)
    }
    
    private func optionRow(for key: String) -> some View {
        let isSelected = selectedOption == key
        
        return Button {
            withAnimation(.easeInOut(duration: 0.25)) {
                selectedOption = key
            }
        } label: {
            HStack {
                Text(LocalizedStringKey(key))
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(isSelected ? .white : Color(red: 0.16, green: 0.27, blue: 0.44))
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.title3)
                        .foregroundColor(.white)
                        .transition(.scale)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 18)
            .background(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(optionStyle(isSelected: isSelected))
                    .shadow(color: Color.black.opacity(isSelected ? 0.16 : 0.08),
                            radius: isSelected ? 14 : 10,
                            x: 0,
                            y: isSelected ? 10 : 6)
            )
        }
        .buttonStyle(.plain)
    }
    
    private func optionStyle(isSelected: Bool) -> AnyShapeStyle {
        if isSelected {
            return AnyShapeStyle(
                LinearGradient(
                    colors: [
                        Color(red: 0.25, green: 0.60, blue: 0.99),
                        Color(red: 0.08, green: 0.40, blue: 0.91)
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
        } else {
            return AnyShapeStyle(Color.white.opacity(colorScheme == .dark ? 0.18 : 1.0))
        }
    }
}

// MARK: - Looping video helper

struct LoopingVideoView: UIViewRepresentable {
    let resourceName: String
    let fileExtension: String
    
    func makeUIView(context: Context) -> LoopingPlayerView {
        LoopingPlayerView(resourceName: resourceName, fileExtension: fileExtension)
    }
    
    func updateUIView(_ uiView: LoopingPlayerView, context: Context) {}
}

final class LoopingPlayerView: UIView {
    private let playerLayer = AVPlayerLayer()
    private let queuePlayer = AVQueuePlayer()
    private var playerLooper: AVPlayerLooper?
    
    init(resourceName: String, fileExtension: String) {
        super.init(frame: .zero)
        backgroundColor = .clear
        isUserInteractionEnabled = false
        
        if let url = Bundle.main.url(forResource: resourceName, withExtension: fileExtension) {
            let asset = AVURLAsset(url: url)
            let item = AVPlayerItem(asset: asset)
            playerLooper = AVPlayerLooper(player: queuePlayer, templateItem: item)
            queuePlayer.play()
        }
        
        playerLayer.player = queuePlayer
        playerLayer.videoGravity = .resizeAspectFill
        layer.addSublayer(playerLayer)
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        playerLayer.frame = bounds
    }
    
    deinit {
        queuePlayer.pause()
        playerLooper = nil
    }
}

struct AccommodationSelectionView_Previews: PreviewProvider {
    static var previews: some View {
        AccommodationSelectionView()
            .environmentObject(LanguageManager())
    }
}
