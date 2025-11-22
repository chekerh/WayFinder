package tn.esprit.wayfinder.presentation.chat

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class ChatRepository(private val apiService: ApiService) {

    suspend fun sendMessage(message: String, model: ChatModel? = null): ChatMessageResponse {
        return apiService.sendChatMessage(ChatMessageRequest(message, model))
    }

    suspend fun switchModel(model: ChatModel): SwitchModelResponse {
        return apiService.switchChatModel(SwitchModelRequest(model))
    }

    suspend fun getHistory(limit: Int = 50): List<ChatHistoryItem> {
        return apiService.getChatHistory(limit)
    }

    suspend fun clearHistory(): ClearHistoryResponse {
        return apiService.clearChatHistory()
    }

    suspend fun getAvailableModels(): AvailableModelsResponse {
        return apiService.getAvailableModels()
    }
}

