import SwiftUI

struct LanguageSettingsView: View {
    @EnvironmentObject private var languageManager: LanguageManager
    
    var body: some View {
        Form {
            Section(header: Text("settings_language_section")) {
                Picker("settings_language_section", selection: $languageManager.selectedLanguage) {
                    ForEach(AppLanguage.allCases) { language in
                        Text(language.localizedNameKey).tag(language)
                    }
                }
                .pickerStyle(.inline)
            }
        }
        .navigationTitle(Text("settings_language_title"))
    }
}

struct LanguageSettingsView_Previews: PreviewProvider {
    static var previews: some View {
        LanguageSettingsView()
            .environmentObject(LanguageManager())
    }
}

