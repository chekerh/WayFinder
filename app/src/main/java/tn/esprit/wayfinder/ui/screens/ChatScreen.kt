package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.AvailableModel
import tn.esprit.wayfinder.models.ChatModel
import tn.esprit.wayfinder.models.FlightPack
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.ChatViewModel
import tn.esprit.wayfinder.viewmodels.ChatMessageUi
import tn.esprit.wayfinder.viewmodels.ChatUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    
    val messages by chatViewModel.messages.collectAsState()
    val availableModels by chatViewModel.availableModels.collectAsState()
    val selectedModel by chatViewModel.selectedModel.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val uiState by chatViewModel.uiState.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    var showModelSelector by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            StringTranslator.translate(context, "AI Travel Assistant"),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedModel?.let { model ->
                                availableModels.find { 
                                    try {
                                        ChatModel.valueOf(it.id.uppercase()) == model
                                    } catch (e: Exception) {
                                        false
                                    }
                                }?.name ?: "Hugging Face (Free)"
                            } ?: "Select Model",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    } 
                },
                actions = {
                    IconButton(onClick = { showModelSelector = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Select Model")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                // Message input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        placeholder = { Text(StringTranslator.translate(context, "Type your message...")) },
                        shape = RoundedCornerShape(28.dp),
                        enabled = !isLoading,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank() && !isLoading) {
                                        chatViewModel.sendMessage(messageText)
                                        messageText = ""
                                    }
                                },
                                enabled = messageText.isNotBlank() && !isLoading
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (messageText.isNotBlank() && !isLoading) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    )
                }
                CustomBottomNavigationBar(navController = navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Messages list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                state = listState
        ) {
                if (messages.isEmpty()) {
                    item {
                        WelcomeMessage()
                    }
                }
                
                items(messages) { message ->
                    ChatMessageBubble(message = message)
                }
                
                if (isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Error snackbar
            when (uiState) {
                is ChatUiState.Error -> {
                    LaunchedEffect(uiState) {
                        chatViewModel.clearError()
                    }
                }
                else -> {}
            }
        }
    }

    // Model selector dialog
    if (showModelSelector) {
        ModelSelectorDialog(
            availableModels = availableModels,
            selectedModel = selectedModel,
            onModelSelected = { model ->
                chatViewModel.switchModel(model)
                showModelSelector = false
            },
            onDismiss = { showModelSelector = false }
        )
    }
}

@Composable
private fun WelcomeMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "👋 Welcome!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "I'm your AI travel assistant. Ask me about flights, destinations, or travel recommendations based on your preferences!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessageUi) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                    
                    // Show model used for AI messages
                    if (!message.isFromUser && message.modelUsed != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "via ${message.modelUsed}",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            // Flight packs
            message.flightPacks?.forEach { pack ->
                Spacer(modifier = Modifier.height(8.dp))
                FlightPackCard(pack = pack)
            }
        }
    }
}

@Composable
private fun FlightPackCard(pack: FlightPack) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (pack.details != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pack.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = pack.price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelSelectorDialog(
    availableModels: List<AvailableModel>,
    selectedModel: ChatModel?,
    onModelSelected: (ChatModel) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select AI Model") },
        text = {
            Column {
                availableModels.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                try {
                                    val chatModel = ChatModel.valueOf(model.id.uppercase())
                                    onModelSelected(chatModel)
                                } catch (e: Exception) {
                                    // Handle invalid model
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = try {
                                val chatModel = ChatModel.valueOf(model.id.uppercase())
                                chatModel == selectedModel
                            } catch (e: Exception) {
                                false
                            },
                            onClick = {
                                try {
                                    val chatModel = ChatModel.valueOf(model.id.uppercase())
                                    onModelSelected(chatModel)
                                } catch (e: Exception) {
                                    // Handle invalid model
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (!model.available) {
                                Text(
                                    text = "Not available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
        }
    }
    )
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    WayFinderTheme {
        ChatScreen(rememberNavController())
    }
}
