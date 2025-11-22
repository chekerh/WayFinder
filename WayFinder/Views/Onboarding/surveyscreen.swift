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
    @State private var selectedOptions: [String: String] = [:] // Stocke les valeurs envoyées au backend
    @State private var isSubmitting = false
    @State private var submissionError: String?
    @State private var navigateToHome = false
    
    private let tripOptions: [SurveyOption] = [
        .init(value: "beach", labelKey: "survey_option_trip_beach"),
        .init(value: "mountain_adventure", labelKey: "survey_option_trip_mountain"),
        .init(value: "cultural", labelKey: "survey_option_trip_cultural"),
        .init(value: "roadtrip", labelKey: "survey_option_trip_roadtrip")
    ]
    private let feelingsOptions: [SurveyOption] = [
        .init(value: "relaxed", labelKey: "survey_option_feelings_relaxed"),
        .init(value: "curious", labelKey: "survey_option_feelings_curious"),
        .init(value: "excited", labelKey: "survey_option_feelings_excited")
    ]
    private let teleportOptions: [SurveyOption] = [
        .init(value: "tropical_islands", labelKey: "survey_option_teleport_tropical"),
        .init(value: "cultural_capital", labelKey: "survey_option_teleport_capital"),
        .init(value: "snowy_mountains", labelKey: "survey_option_teleport_mountains")
    ]
    private let transportOptions: [SurveyOption] = [
        .init(value: "plane", labelKey: "survey_option_transport_plane"),
        .init(value: "train", labelKey: "survey_option_transport_train"),
        .init(value: "boat", labelKey: "survey_option_transport_boat"),
        .init(value: "car", labelKey: "survey_option_transport_car")
    ]
    private let hobbyOptions: [SurveyOption] = [
        .init(value: "photography", labelKey: "survey_option_hobby_photo"),
        .init(value: "hiking", labelKey: "survey_option_hobby_hiking"),
        .init(value: "cooking", labelKey: "survey_option_hobby_cooking"),
        .init(value: "extreme_sports", labelKey: "survey_option_hobby_extreme")
    ]
    private let timeTravelOptions: [SurveyOption] = [
        .init(value: "dinosaurs", labelKey: "survey_option_timetravel_dinosaurs"),
        .init(value: "future", labelKey: "survey_option_timetravel_future"),
        .init(value: "ancient_civilizations", labelKey: "survey_option_timetravel_civilizations")
    ]
    private let budgetOptions: [SurveyOption] = [
        .init(value: "under_500", labelKey: "survey_option_budget_under_500"),
        .init(value: "between_500_1000", labelKey: "survey_option_budget_500_1000"),
        .init(value: "over_1000", labelKey: "survey_option_budget_over_1000")
    ]
    private let activitiesOptions: [SurveyOption] = [
        .init(value: "museums", labelKey: "survey_option_activities_museums"),
        .init(value: "food", labelKey: "survey_option_activities_food"),
        .init(value: "sports", labelKey: "survey_option_activities_sports"),
        .init(value: "spa", labelKey: "survey_option_activities_spa")
    ]
    private let daytimeOptions: [SurveyOption] = [
        .init(value: "morning", labelKey: "survey_option_daytime_morning"),
        .init(value: "evening", labelKey: "survey_option_daytime_evening")
    ]
    private let animalOptions: [SurveyOption] = [
        .init(value: "lion", labelKey: "survey_option_animal_lion"),
        .init(value: "eagle", labelKey: "survey_option_animal_eagle"),
        .init(value: "dolphin", labelKey: "survey_option_animal_dolphin"),
        .init(value: "koala", labelKey: "survey_option_animal_koala")
    ]
    private let epochOptions: [SurveyOption] = [
        .init(value: "sixties", labelKey: "survey_option_epoch_sixties"),
        .init(value: "victorian", labelKey: "survey_option_epoch_victorian"),
        .init(value: "future", labelKey: "survey_option_epoch_future")
    ]
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                NavigationLink(value: "home") {
                    EmptyView()
                }
                .hidden()
                .navigationDestination(isPresented: $navigateToHome) {
                    HomeScreen()
                        .navigationBarBackButtonHidden(true)
                }
                
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
                QuestionCard(questionKey: "survey_question_trip",
                             options: tripOptions,
                             selectedValue: $selectedOptions["vacationType"])
                
                QuestionCard(questionKey: "survey_question_feelings",
                             options: feelingsOptions,
                             selectedValue: $selectedOptions["vacationFeeling"])
                
                QuestionCard(questionKey: "survey_question_teleport",
                             options: teleportOptions,
                             selectedValue: $selectedOptions["teleportDestination"])
                
                QuestionCard(questionKey: "survey_question_transport",
                             options: transportOptions,
                             selectedValue: $selectedOptions["transportMode"])
                
                QuestionCard(questionKey: "survey_question_hobby",
                             options: hobbyOptions,
                             selectedValue: $selectedOptions["hobby"])
                
                QuestionCard(questionKey: "survey_question_timetravel",
                             options: timeTravelOptions,
                             selectedValue: $selectedOptions["timeTravelDestination"])
                
                QuestionCard(questionKey: "survey_question_budget",
                             options: budgetOptions,
                             selectedValue: $selectedOptions["budget"])
                
                QuestionCard(questionKey: "survey_question_activities",
                             options: activitiesOptions,
                             selectedValue: $selectedOptions["activities"])
                
                QuestionCard(questionKey: "survey_question_daytime",
                             options: daytimeOptions,
                             selectedValue: $selectedOptions["morningOrNight"])
                
                QuestionCard(questionKey: "survey_question_animal",
                             options: animalOptions,
                             selectedValue: $selectedOptions["spiritualAnimal"])
                
                QuestionCard(questionKey: "survey_question_epoch",
                             options: epochOptions,
                             selectedValue: $selectedOptions["historicalEra"])
                
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
    var questionKey: String
    var options: [SurveyOption]
    @Binding var selectedValue: String?
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(LocalizedStringKey(questionKey))
                .font(.body)
            
            ForEach(options) { option in
                HStack {
                    RadioButton(isSelected: option.value == selectedValue, action: {
                        selectedValue = option.value
                    })
                    Text(option.localizedLabel)
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
                .background(Circle().fill(isSelected ? ThemeColors.accent() : Color.clear))
                .overlay(
                    Circle()
                        .fill(Color.white)
                        .frame(width: 12, height: 12)
                        .opacity(isSelected ? 1 : 0)
                )
        }
        .buttonStyle(PlainButtonStyle()) // Suppression de la style de bouton par défaut
    }
}

struct SurveyOption: Identifiable, Hashable {
    let id: String
    let value: String
    let labelKey: String
    
    init(value: String, labelKey: String) {
        self.id = value
        self.value = value
        self.labelKey = labelKey
    }
    
    var localizedLabel: LocalizedStringKey {
        LocalizedStringKey(labelKey)
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
