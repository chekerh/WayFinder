package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.ui.theme.WayFinderTheme

// FIX: Re-added UI-specific models as private data classes inside the file.
private data class ChatMessage(val text: String, val isFromGemini: Boolean, val isQuickReply: Boolean = false)
private data class Pack(val title: String, val price: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val messages = listOf(
        ChatMessage("Bonjour! J'ai préparé 3 packs pour vous, lequel préférez-vous?", true),
        ChatMessage("Ou, proposez-moi vos idées!", true, isQuickReply = true),
        Pack("Pack 1: Madrid -> Berlin - Malaysian Airlines", "120TND"),
        Pack("Pack 2: Tunis -> Lyon - Tunisian Airlines", "100TND"),
        Pack("Pack 3: Paris -> Roma -> Hôtel", "150TND")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chat - Packs", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFEAF2FF))
            )
        },
        bottomBar = {
            Column {
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Consulter Gemini")
                }
                CustomBottomNavigationBar(navController = navController)
            }
        },
        containerColor = Color(0xFFF0F8FF)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { item ->
                when (item) {
                    is ChatMessage -> {
                        if (item.isFromGemini) {
                            GeminiMessageBubble(item)
                        }
                    }
                    is Pack -> {
                        PackItem(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiMessageBubble(message: ChatMessage) {
    val alignment = if (message.isQuickReply) Alignment.CenterEnd else Alignment.CenterStart
    val colors = if (message.isQuickReply) {
        ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
    } else {
        ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Button(onClick = { /*TODO*/ }, colors = colors, shape = RoundedCornerShape(12.dp)) {
            Text(message.text)
        }
    }
}

@Composable
private fun PackItem(pack: Pack) {
    Button(
        onClick = { /* TODO */ },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White, 
            contentColor = Color.Black
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(pack.title)
            Text(pack.price, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    WayFinderTheme {
        ChatScreen(rememberNavController())
    }
}
