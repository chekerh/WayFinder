package tn.esprit.wayfinder.presentation.reviews

import android.util.Log
import retrofit2.HttpException
import kotlinx.serialization.SerializationException
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class ReviewsRepository(private val apiService: ApiService) {

    suspend fun getReviews(itemType: String, itemId: String): List<Review> {
        return try {
            apiService.getReviews(itemType, itemId)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // No reviews found - return empty list
                Log.d("ReviewsRepository", "No reviews found for $itemType/$itemId (404)")
                emptyList()
            } else {
                Log.e("ReviewsRepository", "HTTP error loading reviews: ${e.code()} - ${e.message()}")
                throw e
            }
        } catch (e: SerializationException) {
            Log.e("ReviewsRepository", "JSON parsing error for reviews: ${e.message ?: "Unknown parsing error"}", e)
            // Return empty list if JSON parsing fails (e.g., empty response)
            emptyList()
        } catch (e: Exception) {
            Log.e("ReviewsRepository", "Error loading reviews: ${e.message}", e)
            // For any other error, return empty list to avoid blocking the UI
            emptyList()
        }
    }

    suspend fun getReviewStats(itemType: String, itemId: String): ReviewStatsResponse? {
        return try {
            apiService.getReviewStats(itemType, itemId)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // No stats available - return null
                Log.d("ReviewsRepository", "No stats found for $itemType/$itemId (404)")
                null
            } else {
                Log.e("ReviewsRepository", "HTTP error loading stats: ${e.code()} - ${e.message()}")
                null
            }
        } catch (e: SerializationException) {
            Log.e("ReviewsRepository", "JSON parsing error for stats: ${e.message ?: "Unknown parsing error"}", e)
            null
        } catch (e: Exception) {
            Log.e("ReviewsRepository", "Error loading stats: ${e.message}", e)
            null
        }
    }

    suspend fun checkUserReview(itemType: String, itemId: String): Review? {
        return try {
            apiService.checkUserReview(itemType, itemId)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // User has no review - return null
                Log.d("ReviewsRepository", "User has no review for $itemType/$itemId (404)")
                null
            } else {
                Log.e("ReviewsRepository", "HTTP error checking user review: ${e.code()} - ${e.message()}")
                null
            }
        } catch (e: SerializationException) {
            Log.e("ReviewsRepository", "JSON parsing error for user review: ${e.message ?: "Unknown parsing error"}", e)
            null
        } catch (e: Exception) {
            Log.e("ReviewsRepository", "Error checking user review: ${e.message}", e)
            null
        }
    }

    suspend fun getUserReviews(itemType: String? = null): List<Review> {
        return apiService.getUserReviews(itemType)
    }

    suspend fun createReview(itemType: String, itemId: String, rating: Int, comment: String?, details: Map<String, Int>?): Review {
        val request = CreateReviewRequest(itemType, itemId, rating, comment, details)
        return apiService.createReview(request)
    }

    suspend fun updateReview(reviewId: String, rating: Int?, comment: String?, details: Map<String, Int>?): Review {
        val request = UpdateReviewRequest(rating, comment, details)
        return apiService.updateReview(reviewId, request)
    }

    suspend fun deleteReview(reviewId: String): Map<String, String> {
        return apiService.deleteReview(reviewId)
    }
}

