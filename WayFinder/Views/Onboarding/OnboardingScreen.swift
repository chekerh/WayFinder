//
//  OnboardingScreen.swift
//  WayFinder
//
//  Dynamic onboarding screen that fetches questions from backend
//

import Foundation
import SwiftUI

struct OnboardingScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var viewModel = OnboardingViewModel()
    @State private var navigateToHome = false
    @State private var selectedAnswers: [String] = []
    
    var body: some View {
        NavigationStack {
            ZStack {
                ThemeColors.background(colorScheme)
                    .ignoresSafeArea()
                
                Group {
                    switch viewModel.uiState {
                    case .idle:
                        EmptyView()
                    case .loading:
                        LoadingView(onSkip: {
                            viewModel.skipOnboarding()
                        })
                    case .questionLoaded(let question, let progress):
                        QuestionView(
                            question: question,
                            progress: progress,
                            selectedAnswers: $selectedAnswers,
                            onAnswer: { answer in
                                viewModel.submitAnswer(questionId: question.id, answer: answer)
                                selectedAnswers = []
                            },
                            onSkip: {
                                viewModel.skipOnboarding()
                            }
                        )
                    case .completed(let message):
                        CompletionView(message: message) {
                            navigateToHome = true
                        }
                    case .error(let message):
                        ErrorView(
                            message: message,
                            onRetry: {
                                viewModel.startOnboarding()
                            },
                            onGoHome: {
                                navigateToHome = true
                            }
                        )
                    }
                }
            }
            .navigationDestination(isPresented: $navigateToHome) {
                HomeScreen()
                    .navigationBarBackButtonHidden(true)
            }
            .task {
                await viewModel.verifyProgress()
                viewModel.startOnboarding()
            }
        }
    }
}

// MARK: - Loading View
struct LoadingView: View {
    let onSkip: () -> Void
    
    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)
            Text(String(localized: "Loading question..."))
                .font(.headline)
                .foregroundColor(.secondary)
            
            Button(action: onSkip) {
                Text(String(localized: "Skip"))
                    .font(.subheadline)
                    .foregroundColor(.blue)
            }
            .padding(.top, 20)
        }
    }
}

// MARK: - Question View
struct QuestionView: View {
    @Environment(\.colorScheme) private var colorScheme
    let question: OnboardingQuestion
    let progress: Progress
    @Binding var selectedAnswers: [String]
    let onAnswer: (AnswerValue) -> Void
    let onSkip: () -> Void
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Progress bar
                if let total = progress.total {
                    ProgressView(value: Double(progress.current), total: Double(total))
                        .progressViewStyle(LinearProgressViewStyle())
                        .padding(.horizontal)
                        .padding(.top)
                }
                
                // Question text
                Text(question.text)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
                    .padding(.top, 20)
                
                // Options
                if let options = question.options {
                    VStack(spacing: 12) {
                        ForEach(options) { option in
                            OptionButton(
                                option: option,
                                isSelected: selectedAnswers.contains(option.value),
                                questionType: question.questionType,
                                onTap: {
                                    handleOptionTap(option.value)
                                }
                            )
                        }
                    }
                    .padding(.horizontal)
                } else if question.questionType == .text {
                    TextField("Your answer", text: Binding(
                        get: { selectedAnswers.first ?? "" },
                        set: { selectedAnswers = [$0] }
                    ))
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)
                } else if question.questionType == .number {
                    TextField("Enter number", text: Binding(
                        get: { selectedAnswers.first ?? "" },
                        set: { selectedAnswers = [$0] }
                    ))
                    .keyboardType(.numberPad)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)
                }
                
                // Submit button
                Button(action: {
                    submitAnswer()
                }) {
                    Text(String(localized: "Continue"))
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(
                            canSubmit ? ThemeColors.accent() : Color.gray.opacity(0.3)
                        )
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .disabled(!canSubmit)
                .padding(.horizontal)
                .padding(.top, 8)
                
                // Skip button
                if !question.required {
                    Button(action: onSkip) {
                        Text(String(localized: "Skip"))
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 8)
                }
            }
            .padding(.vertical)
        }
    }
    
    private var canSubmit: Bool {
        if question.required {
            if question.questionType == .multipleChoice {
                let min = question.minSelections ?? 1
                let max = question.maxSelections ?? Int.max
                return selectedAnswers.count >= min && selectedAnswers.count <= max
            } else {
                return !selectedAnswers.isEmpty
            }
        }
        return true
    }
    
    private func handleOptionTap(_ value: String) {
        if question.questionType == .multipleChoice {
            if selectedAnswers.contains(value) {
                selectedAnswers.removeAll { $0 == value }
            } else {
                let max = question.maxSelections ?? Int.max
                if selectedAnswers.count < max {
                    selectedAnswers.append(value)
                }
            }
        } else {
            selectedAnswers = [value]
        }
    }
    
    private func submitAnswer() {
        guard canSubmit else { return }
        
        let answer: AnswerValue
        if question.questionType == .multipleChoice {
            answer = .array(selectedAnswers)
        } else if question.questionType == .number, let first = selectedAnswers.first, let number = Double(first) {
            answer = .number(number)
        } else if let first = selectedAnswers.first {
            answer = .string(first)
        } else {
            return
        }
        
        onAnswer(answer)
    }
}

// MARK: - Option Button
struct OptionButton: View {
    @Environment(\.colorScheme) private var colorScheme
    let option: QuestionOption
    let isSelected: Bool
    let questionType: QuestionType
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack {
                // Selection indicator
                if questionType == .multipleChoice {
                    Image(systemName: isSelected ? "checkmark.square.fill" : "square")
                        .foregroundColor(isSelected ? ThemeColors.accent() : .secondary)
                        .font(.system(size: 20))
                } else {
                    Circle()
                        .fill(isSelected ? ThemeColors.accent() : Color.clear)
                        .frame(width: 20, height: 20)
                        .overlay(
                            Circle()
                                .strokeBorder(isSelected ? ThemeColors.accent() : Color.secondary, lineWidth: 2)
                        )
                        .overlay(
                            Circle()
                                .fill(Color.white)
                                .frame(width: 8, height: 8)
                                .opacity(isSelected ? 1 : 0)
                        )
                }
                
                Text(option.label)
                    .font(.body)
                    .foregroundColor(ThemeColors.primaryText(colorScheme))
                
                Spacer()
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isSelected ? ThemeColors.accent().opacity(0.1) : ThemeColors.surface(colorScheme))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(isSelected ? ThemeColors.accent() : Color.clear, lineWidth: 2)
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Completion View
struct CompletionView: View {
    let message: String
    let onContinue: () -> Void
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 60))
                .foregroundColor(.green)
            
            Text(message)
                .font(.headline)
                .multilineTextAlignment(.center)
                .padding()
            
            Button(action: onContinue) {
                Text(String(localized: "Continue to Home"))
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(ThemeColors.accent())
                    .foregroundColor(.white)
                    .cornerRadius(12)
            }
            .padding(.horizontal)
        }
        .padding()
    }
}

// MARK: - Error View
struct ErrorView: View {
    let message: String
    let onRetry: () -> Void
    let onGoHome: () -> Void
    
    private var isAlreadyCompleted: Bool {
        message.lowercased().contains("already completed") || 
        message.lowercased().contains("déjà complété")
    }
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 60))
                .foregroundColor(.orange)
            
            Text(message)
                .font(.headline)
                .multilineTextAlignment(.center)
                .padding()
            
            Button(action: {
                if isAlreadyCompleted {
                    onGoHome()
                } else {
                    onRetry()
                }
            }) {
                Text(isAlreadyCompleted ? String(localized: "Continue to Home") : String(localized: "generic_retry"))
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(ThemeColors.accent())
                    .foregroundColor(.white)
                    .cornerRadius(12)
            }
            .padding(.horizontal)
        }
        .padding()
    }
}

