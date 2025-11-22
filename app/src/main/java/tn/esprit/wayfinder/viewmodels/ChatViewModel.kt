package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.presentation.chat.ChatRepository

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Success(val message: String) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

data class ChatMessageUi(
    val text: String,
    val isFromUser: Boolean,
    val modelUsed: String? = null,
    val flightPacks: List<FlightPack>? = null
)

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageUi>>(emptyList())
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()

    private val _availableModels = MutableStateFlow<List<AvailableModel>>(emptyList())
    val availableModels: StateFlow<List<AvailableModel>> = _availableModels.asStateFlow()

    private val _selectedModel = MutableStateFlow<ChatModel?>(null)
    val selectedModel: StateFlow<ChatModel?> = _selectedModel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAvailableModels()
        loadHistory()
    }

    fun sendMessage(message: String) {
        if (message.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Add user message to UI immediately
                val userMessage = ChatMessageUi(
                    text = message,
                    isFromUser = true
                )
                _messages.value = _messages.value + userMessage

                // Send to API
                val model = _selectedModel.value
                val response = chatRepository.sendMessage(message, model)

                // Add AI response to UI
                val aiMessage = ChatMessageUi(
                    text = response.message,
                    isFromUser = false,
                    modelUsed = response.modelUsed,
                    flightPacks = response.flightPacks
                )
                _messages.value = _messages.value + aiMessage

                _uiState.value = ChatUiState.Success("Message sent")
            } catch (e: Exception) {
                // Remove user message on error
                _messages.value = _messages.value.dropLast(1)
                _uiState.value = ChatUiState.Error(
                    e.message ?: "Failed to send message. Please try again."
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun switchModel(model: ChatModel) {
        viewModelScope.launch {
            try {
                val response = chatRepository.switchModel(model)
                if (response.success) {
                    _selectedModel.value = model
                    _uiState.value = ChatUiState.Success("Model switched to ${response.model}")
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(
                    e.message ?: "Failed to switch model. Please try again."
                )
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val history = chatRepository.getHistory(50)
                _messages.value = history.map { item ->
                    ChatMessageUi(
                        text = item.message,
                        isFromUser = item.role == "user",
                        modelUsed = if (item.role == "assistant") item.modelUsed else null,
                        flightPacks = item.flightPacks
                    )
                }
            } catch (e: Exception) {
                // Silently fail - history is optional
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                chatRepository.clearHistory()
                _messages.value = emptyList()
                _uiState.value = ChatUiState.Success("History cleared")
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(
                    e.message ?: "Failed to clear history"
                )
            }
        }
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val response = chatRepository.getAvailableModels()
                _availableModels.value = response.models
                
                // Set default model to first available one
                val firstAvailable = response.models.firstOrNull { it.available }
                if (firstAvailable != null) {
                    try {
                        _selectedModel.value = ChatModel.valueOf(firstAvailable.id.uppercase())
                    } catch (e: Exception) {
                        _selectedModel.value = ChatModel.HUGGINGFACE
                    }
                }
            } catch (e: Exception) {
                // Set default models if API fails
                _availableModels.value = listOf(
                    AvailableModel("huggingface", "Hugging Face (Free)", true),
                    AvailableModel("openai_gpt4o_mini", "OpenAI GPT-4o Mini", true),
                    AvailableModel("openai_gpt4o", "OpenAI GPT-4o", true)
                )
                _selectedModel.value = ChatModel.HUGGINGFACE
            }
        }
    }

    fun clearError() {
        _uiState.value = ChatUiState.Idle
    }
}

