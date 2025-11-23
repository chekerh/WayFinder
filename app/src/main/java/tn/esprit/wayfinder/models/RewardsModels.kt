package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPointsResponse(
    @SerialName("total_points") val totalPoints: Int,
    @SerialName("available_points") val availablePoints: Int,
    @SerialName("lifetime_points") val lifetimePoints: Int,
    @SerialName("recent_transactions") val recentTransactions: List<PointsTransaction>
)

@Serializable
data class PointsTransaction(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("user_id") val userId: String,
    val points: Int,
    val type: String, // "earned" | "redeemed" | "bonus" | "penalty"
    val source: String, // "onboarding" | "booking" | "review" | etc.
    val description: String? = null,
    @SerialName("transaction_date") val transactionDate: String
)

@Serializable
data class AwardPointsRequest(
    val points: Int,
    val source: String,
    val description: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class AwardPointsResponse(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("total_points") val totalPoints: Int,
    @SerialName("points_awarded") val pointsAwarded: Int
)

@Serializable
data class RedeemPointsRequest(
    val points: Int,
    val description: String,
    val metadata: Map<String, String>? = null
)

@Serializable
data class RedeemPointsResponse(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("remaining_points") val remainingPoints: Int
)

