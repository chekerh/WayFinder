package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.models.OnboardingQuestion // FIX: Import from models
import tn.esprit.wayfinder.models.Progress // FIX: Import from models
import tn.esprit.wayfinder.models.QuestionOption // FIX: Import from models
import tn.esprit.wayfinder.ui.theme.WayFinderTheme

@Composable
fun SurveyScreen(navController: NavController) {
    // This is a placeholder for the real ViewModel logic
    val sampleQuestion = OnboardingQuestion(
        id = "q1",
        type = "single_choice",
        text = "What type of trip are you planning?",
        options = listOf(
            QuestionOption("business", "Business"),
            QuestionOption("leisure", "Leisure"),
            QuestionOption("adventure", "Adventure")
        ),
        required = true,
        minSelections = null,
        maxSelections = null
    )
    val sampleProgress = Progress(current = 1, total = 8)
    
    QuestionScreen(question = sampleQuestion, progress = sampleProgress, onAnswer = {})
}

@Composable
fun QuestionScreen(
    question: OnboardingQuestion,
    progress: Progress,
    onAnswer: (Any) -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F8FF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Question ${progress.current} of ~${progress.total ?: 8}", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress.current.toFloat() / (progress.total?.toFloat() ?: 8f), // FIX: Direct float calculation
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(text = question.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(32.dp))

        when (question.type) {
            "single_choice" -> {
                question.options?.forEach { option ->
                    val isSelected = selectedOption == option.value
                    Button(
                        onClick = { selectedOption = option.value },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF1976D2) else Color.White,
                            contentColor = if (isSelected) Color.White else Color.Black
                        )
                    ) {
                        Text(option.label)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { if (selectedOption != null) onAnswer(selectedOption!!) },
            enabled = selectedOption != null,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SurveyScreenPreview() {
    WayFinderTheme {
        SurveyScreen(rememberNavController())
    }
}
