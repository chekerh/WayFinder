package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChatModel {
    @SerialName("huggingface")
    HUGGINGFACE,
    
    @SerialName("openai_gpt4o_mini")
    OPENAI_GPT4O_MINI,
    
    @SerialName("openai_gpt4o")
    OPENAI_GPT4O
}

@Serializable
data class ChatMessageRequest(
    val message: String,
    val model: ChatModel? = null
)

@Serializable
data class FlightPack(
    val title: String,
    val price: String,
    val origin: String,
    val destination: String,
    val airline: String? = null,
    val details: String? = null
)

@Serializable
data class ChatMessageResponse(
    val message: String,
    @SerialName("model_used") val modelUsed: String,
    @SerialName("flight_packs") val flightPacks: List<FlightPack>? = null,
    @SerialName("session_id") val sessionId: String
)

@Serializable
data class SwitchModelRequest(
    val model: ChatModel
)

@Serializable
data class SwitchModelResponse(
    val success: Boolean,
    val model: String
)

@Serializable
data class ChatHistoryItem(
    val message: String,
    val role: String, // "user" or "assistant"
    @SerialName("model_used") val modelUsed: String,
    @SerialName("flight_packs") val flightPacks: List<FlightPack>? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ClearHistoryResponse(
    val success: Boolean
)

@Serializable
data class AvailableModel(
    val id: String,
    val name: String,
    val available: Boolean
)

@Serializable
data class AvailableModelsResponse(
    val models: List<AvailableModel>
)

