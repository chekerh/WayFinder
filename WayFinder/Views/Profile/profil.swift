//
//  profil.swift
//  WayFinder
//
//  Created by sarrachmek on 7/11/2025.
//

import SwiftUI

struct ProfileView: View {
    private let actions: [ProfileAction] = [
        .init(icon: "pencil", titleKey: "profile_edit_name"),
        .init(icon: "checklist", titleKey: "profile_list_project"),
        .init(icon: "lock", titleKey: "profile_change_password"),
        .init(icon: "envelope", titleKey: "profile_change_email"),
        .init(icon: "gearshape", titleKey: "profile_settings"),
        .init(icon: "slider.horizontal.3", titleKey: "profile_preferences"),
        .init(icon: "arrow.right.square", titleKey: "profile_logout", isDestructive: true)
    ]
    
    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 24) {
                header
                    .padding(.horizontal, 24)
                    .padding(.top, 24)
                
                VStack(spacing: 12) {
                    ForEach(actions) { action in
                        ProfileRow(action: action)
                            .padding(.horizontal, 24)
                    }
                }
                .padding(.bottom, 32)
            }
        }
        .background(Color(red: 0.93, green: 0.97, blue: 0.95).ignoresSafeArea())
    }
    
    private var header: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(LinearGradient(
                    colors: [Color(red: 0.07, green: 0.35, blue: 0.28), Color(red: 0.12, green: 0.52, blue: 0.40)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ))
                .shadow(color: Color.black.opacity(0.12), radius: 20, x: 0, y: 10)
            
            VStack(spacing: 16) {
                ZStack(alignment: .bottomTrailing) {
                    Circle()
                        .fill(Color.white)
                        .overlay(
                            Image(systemName: "person.fill")
                                .resizable()
                                .scaledToFit()
                                .foregroundColor(Color(red: 0.12, green: 0.52, blue: 0.40))
                                .padding(24)
                        )
                        .frame(width: 120, height: 120)
                    
                    Circle()
                        .fill(Color(red: 0.98, green: 0.82, blue: 0.18))
                        .frame(width: 36, height: 36)
                        .overlay(Image(systemName: "pencil")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(.white))
                        .offset(x: 6, y: 6)
                }
                
                Text("profile_username")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(.white)
            }
            .padding(.vertical, 32)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct ProfileAction: Identifiable {
    let id = UUID()
    let icon: String
    let titleKey: String
    var isDestructive: Bool = false
}

private struct ProfileRow: View {
    let action: ProfileAction
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: action.icon)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(action.isDestructive ? Color(red: 0.82, green: 0.18, blue: 0.12) : Color(red: 0.12, green: 0.52, blue: 0.40))
                .frame(width: 32, height: 32)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(action.isDestructive ? Color(red: 0.98, green: 0.90, blue: 0.90) : Color.white)
                )
                .shadow(color: Color.black.opacity(0.08), radius: 4, x: 0, y: 3)
            
            Text(LocalizedStringKey(action.titleKey))
                .font(.system(size: 16, weight: action.isDestructive ? .semibold : .regular))
                .foregroundColor(action.isDestructive ? Color(red: 0.82, green: 0.18, blue: 0.12) : Color(red: 0.17, green: 0.17, blue: 0.19))
                .frame(maxWidth: .infinity, alignment: .leading)
            
            Image(systemName: "chevron.right")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Color.gray.opacity(0.5))
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color.white)
                .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 6)
        )
    }
}

struct ProfileView_Previews: PreviewProvider {
    static var previews: some View {
        ProfileView()
    }
}
