package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.clickable
// Main Composable for the Survey Screen
@Composable
fun SurveyScreen(navController: NavController) {
    val scrollState = rememberScrollState() // Scroll state for making the content scrollable

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // Enable scrolling
            .padding(16.dp)
    ) {
        // Title Text
        Text(
            text = "Bienvenue sur Wayfindr",
            style = MaterialTheme.typography.headlineSmall, // Corrected: M3 typography
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Introduction Text
        Text(
            text = "Ce formulaire nous aide à personnaliser l'application en fonction de tes préférences et envies ! 🧳",
            style = MaterialTheme.typography.bodyLarge, // Corrected: M3 typography
            modifier = Modifier.padding(bottom = 16.dp),
            color = Color.Gray
        )

        // Fun and personal questions for the survey
        QuestionCard(
            question = "Quel type de voyage préfères-tu ?",
            options = listOf("Vacances à la plage 🏖️", "Aventure en montagne 🏔️", "Voyage culturel 🏛️", "Road trip 🚗")
        )

        QuestionCard(
            question = "Comment te sens-tu en vacances ?",
            options = listOf("Détendu et zen 😌", "Énergisé et curieux 🌍", "Excité et aventureux 🏄‍♂️")
        )

        QuestionCard(
            question = "Si tu pouvais téléporter tes affaires, où irais-tu ?",
            options = listOf("Îles tropicales 🏝️", "Capitale culturelle 🏙️", "Montagnes enneigées 🏔️")
        )

        QuestionCard(
            question = "Quel est ton mode de transport préféré ?",
            options = listOf("Avion ✈️", "Train 🚄", "Bateau ⛴️", "Voiture 🚗")
        )

        QuestionCard(
            question = "As-tu un hobby qui t'inspire durant tes voyages ?",
            options = listOf("Photographie 📸", "Randonnée 🥾", "Cuisine locale 🍽️", "Sports extrêmes 🧗")
        )

        QuestionCard(
            question = "Si tu pouvais voyager dans le temps, où irais-tu ?",
            options = listOf("À l'époque des dinosaures 🦖", "Dans le futur 🌌", "À l'ère des grandes civilisations anciennes 🏛️")
        )

        QuestionCard(
            question = "Quel budget es-tu prêt à investir pour ton prochain voyage ?",
            options = listOf("Moins de 500 TND 💸", "500–1000 TND 💰", "Plus de 1000 TND 💎")
        )

        // Additional Questions
        QuestionCard(
            question = "Quel genre d'activités te font vibrer pendant un voyage ?",
            options = listOf("Explorer des musées 🎨", "Découvrir de nouveaux restaurants 🍽️", "Faire du sport 🏅", "Se détendre au spa 💆‍♂️")
        )

        QuestionCard(
            question = "Es-tu plutôt du matin ou du soir ?",
            options = listOf("Le matin 🌅", "Le soir 🌙")
        )

        QuestionCard(
            question = "Quel est ton animal spirituel en voyage ?",
            options = listOf("Lion 🦁", "Aigle 🦅", "Dauphin 🐬", "Koala 🐨")
        )

        // New Fun Question
        QuestionCard(
            question = "Si tu pouvais vivre dans une époque différente, laquelle choisirais-tu ?",
            options = listOf("Les années 60 🎉", "L’ère victorienne 👑", "Le futur du XXIIe siècle 🚀")
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = { /* TODO: Handle the submit action or navigate to next screen */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Soumettre")
        }
    }
}

// Composable for each question card with options
@Composable
fun QuestionCard(question: String, options: List<String>) {
    var selectedOption by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = question, style = MaterialTheme.typography.bodyLarge) // Corrected: M3 typography

        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedOption = option } // Make the whole row clickable
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = (selectedOption == option), // Manage selection state here
                    onClick = { selectedOption = option } // Handle selection
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = option, style = MaterialTheme.typography.bodyMedium) // Corrected: M3 typography
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SurveyScreenPreview() {
    MaterialTheme {
        SurveyScreen(navController = rememberNavController())
    }
}
