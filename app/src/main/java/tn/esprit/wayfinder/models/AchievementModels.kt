package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AchievementType {
    @SerialName("first_booking")
    FIRST_BOOKING,
    
    @SerialName("explorer")
    EXPLORER, // 5 trips
    
    @SerialName("world_traveler")
    WORLD_TRAVELER, // 10 destinations
    
    @SerialName("frequent_flyer")
    FREQUENT_FLYER, // 20 trips
    
    @SerialName("budget_saver")
    BUDGET_SAVER, // Saved over $500
    
    @SerialName("early_bird")
    EARLY_BIRD, // Booked 3 months ahead
    
    @SerialName("social_butterfly")
    SOCIAL_BUTTERFLY, // Shared 5 journeys
    
    @SerialName("reviewer")
    REVIEWER, // Wrote 10 reviews
    
    @SerialName("streak_master")
    STREAK_MASTER, // 30-day streak
    
    @SerialName("points_collector")
    POINTS_COLLECTOR // Earned 1000 points
}

@Serializable
data class Achievement(
    val id: String,
    val type: AchievementType,
    val title: String,
    val description: String,
    val icon: String, // Icon name or emoji
    val unlockedAt: String? = null,
    val progress: Int = 0,
    val target: Int = 1
)

@Serializable
data class UserAchievements(
    val achievements: List<Achievement>,
    @SerialName("total_points") val totalPoints: Int = 0,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0
)

