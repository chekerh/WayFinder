//
//  surveyscreen.swift
//  WayFinder
//
//  Created by sarrachmek on 7/11/2025.
//

import Foundation
import SwiftUI

struct SurveyScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var selectedOptions: [String: String] = [:] // Pour stocker les réponses
    @State private var isSubmitting = false
    @State private var submissionError: String?
    @State private var navigateToHome = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                NavigationLink(destination: HomeScreen()
                    .navigationBarBackButtonHidden(true), isActive: $navigateToHome) {
                        EmptyView()
                    }
                    .hidden()
                
                surveyContent
            }
            .background(ThemeColors.background(colorScheme).ignoresSafeArea())
        }
        .alert(String(localized: "survey_submit_error_title"), isPresented: errorAlertBinding) {
            Button(String(localized: "generic_ok")) {
                submissionError = nil
            }
        } message: {
            Text(submissionError ?? "")
        }
    }
    
    private var surveyContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                
                // Titre
                Text("survey_title")
                    .font(.title)
                    .padding(.bottom, 8)
                
                // Texte d'introduction
                Text("survey_intro")
                    .font(.body)
                    .foregroundColor(ThemeColors.secondaryText(colorScheme))
                    .padding(.bottom, 16)
                
                // Questions et réponses
                QuestionCard(question: "survey_question_trip",
                             options: ["Vacances à la plage 🏖️", "Aventure en montagne 🏔️", "Voyage culturel 🏛️", "Road trip 🚗"],
                             selectedOption: $selectedOptions["vacationType"])
                
                QuestionCard(question: "survey_question_feelings",
                             options: ["Détendu et zen 😌", "Énergisé et curieux 🌍", "Excité et aventureux 🏄‍♂️"],
                             selectedOption: $selectedOptions["vacationFeeling"])
                
                QuestionCard(question: "survey_question_teleport",
                             options: ["Îles tropicales 🏝️", "Capitale culturelle 🏙️", "Montagnes enneigées 🏔️"],
                             selectedOption: $selectedOptions["teleportDestination"])
                
                QuestionCard(question: "survey_question_transport",
                             options: ["Avion ✈️", "Train 🚄", "Bateau ⛴️", "Voiture 🚗"],
                             selectedOption: $selectedOptions["transportMode"])
                
                QuestionCard(question: "survey_question_hobby",
                             options: ["Photographie 📸", "Randonnée 🥾", "Cuisine locale 🍽️", "Sports extrêmes 🧗"],
                             selectedOption: $selectedOptions["hobby"])
                
                QuestionCard(question: "survey_question_timetravel",
                             options: ["À l'époque des dinosaures 🦖", "Dans le futur 🌌", "À l'ère des grandes civilisations anciennes 🏛️"],
                             selectedOption: $selectedOptions["timeTravelDestination"])
                
                QuestionCard(question: "survey_question_budget",
                             options: ["Moins de 500 TND 💸", "500–1000 TND 💰", "Plus de 1000 TND 💎"],
                             selectedOption: $selectedOptions["budget"])
                
                QuestionCard(question: "survey_question_activities",
                             options: ["Explorer des musées 🎨", "Découvrir de nouveaux restaurants 🍽️", "Faire du sport 🏅", "Se détendre au spa 💆‍♂️"],
                             selectedOption: $selectedOptions["activities"])
                
                QuestionCard(question: "survey_question_daytime",
                             options: ["Le matin 🌅", "Le soir 🌙"],
                             selectedOption: $selectedOptions["morningOrNight"])
                
                QuestionCard(question: "survey_question_animal",
                             options: ["Lion 🦁", "Aigle 🦅", "Dauphin 🐬", "Koala 🐨"],
                             selectedOption: $selectedOptions["spiritualAnimal"])
                
                QuestionCard(question: "survey_question_epoch",
                             options: ["Les années 60 🎉", "L’ère victorienne 👑", "Le futur du XXIIe siècle 🚀"],
                             selectedOption: $selectedOptions["historicalEra"])
                
                // Bouton de soumission
                Button(action: {
                    Task { await submitSurvey() }
                }) {
                    if isSubmitting {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .frame(maxWidth: .infinity)
                            .padding()
                    } else {
                        Text("survey_submit")
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .padding()
                    }
                }
                .background(ThemeColors.accent())
                .foregroundColor(.white)
                .cornerRadius(8)
                .disabled(isSubmitting)
                .padding(.top, 24)
            }
            .padding(16)
            .padding(.bottom, 40)
        }
    }
}

struct QuestionCard: View {
    var question: String
    var options: [String]
    @Binding var selectedOption: String?
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(LocalizedStringKey(question))
                .font(.body)
            
            ForEach(options, id: \.self) { option in
                HStack {
                    RadioButton(isSelected: option == selectedOption, action: {
                        selectedOption = option
                    })
                    Text(option)
                        .font(.body)
                }
                .padding(.vertical, 4)
            }
        }
    }
}

struct RadioButton: View {
    var isSelected: Bool
    var action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Circle()
                .strokeBorder(Color.gray, lineWidth: 1)
                .frame(width: 20, height: 20)
                .background(Circle().fill(isSelected ? Color.blue : Color.clear))
                .overlay(
                    Circle()
                        .fill(isSelected ? Color.white : Color.clear)
                        .frame(width: 12, height: 12)
                )
        }
        .buttonStyle(PlainButtonStyle()) // Suppression de la style de bouton par défaut
    }
}

private extension SurveyScreen {
    var errorAlertBinding: Binding<Bool> {
        Binding(get: { submissionError != nil }, set: { presented in
            if !presented {
                submissionError = nil
            }
        })
    }

    func submitSurvey() async {
        guard !isSubmitting else { return }
        isSubmitting = true
        submissionError = nil
        
        let request = SurveyPreferenceRequest(
            vacationType: selectedOptions["vacationType"],
            vacationFeeling: selectedOptions["vacationFeeling"],
            teleportDestination: selectedOptions["teleportDestination"],
            transportMode: selectedOptions["transportMode"],
            hobby: selectedOptions["hobby"],
            timeTravelDestination: selectedOptions["timeTravelDestination"],
            budget: selectedOptions["budget"],
            activities: selectedOptions["activities"],
            morningOrNight: selectedOptions["morningOrNight"],
            spiritualAnimal: selectedOptions["spiritualAnimal"],
            historicalEra: selectedOptions["historicalEra"]
        )
        
        do {
            let response = try await SurveyService.shared.submitPreferences(request)
            PreferenceStorage.savePreferenceId(response.id)
            navigateToHome = true
        } catch {
            submissionError = error.localizedDescription
        }
        
        isSubmitting = false
    }
}

struct SurveyScreen_Previews: PreviewProvider {
    static var previews: some View {
        SurveyScreen()
    }
}
