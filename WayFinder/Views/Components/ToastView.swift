//
//  ToastView.swift
//  WayFinder
//
//  Toast overlay component that appears on top of content without interrupting it
//

import SwiftUI

struct ToastView: View {
    let message: String
    let type: ToastType
    @Binding var isPresented: Bool
    
    enum ToastType {
        case success
        case error
        case info
        
        var backgroundColor: Color {
            switch self {
            case .success:
                return Color.green
            case .error:
                return Color.red
            case .info:
                return Color.blue
            }
        }
        
        var icon: String {
            switch self {
            case .success:
                return "checkmark.circle.fill"
            case .error:
                return "exclamationmark.circle.fill"
            case .info:
                return "info.circle.fill"
            }
        }
    }
    
    var body: some View {
        VStack {
            Spacer()
            
            if isPresented {
                HStack(spacing: 12) {
                    Image(systemName: type.icon)
                        .foregroundColor(.white)
                        .font(.system(size: 20))
                    
                    Text(message)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.leading)
                    
                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
                .background(type.backgroundColor)
                .cornerRadius(12)
                .shadow(color: Color.black.opacity(0.2), radius: 8, x: 0, y: 4)
                .padding(.horizontal, 20)
                .padding(.bottom, 50)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(1000)
            }
        }
        .animation(.spring(response: 0.4, dampingFraction: 0.8), value: isPresented)
    }
}

// View modifier to easily add toast functionality
struct ToastModifier: ViewModifier {
    @Binding var toastMessage: String?
    @Binding var toastType: ToastView.ToastType
    @State private var isPresented = false
    
    func body(content: Content) -> some View {
        ZStack {
            content
            
            if let message = toastMessage {
                ToastView(
                    message: message,
                    type: toastType,
                    isPresented: $isPresented
                )
                .onAppear {
                    isPresented = true
                    // Auto-dismiss after 3 seconds
                    DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                        withAnimation {
                            isPresented = false
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                            toastMessage = nil
                        }
                    }
                }
            }
        }
    }
}

extension View {
    func toast(
        message: Binding<String?>,
        type: Binding<ToastView.ToastType> = .constant(.info)
    ) -> some View {
        modifier(ToastModifier(toastMessage: message, toastType: type))
    }
}

