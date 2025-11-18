package tn.esprit.wayfinder.presentation.reviews

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class ReviewsRepository(private val apiService: ApiService) {

    suspend fun getReviews(itemType: String, itemId: String): List<Review> {
        return apiService.getReviews(itemType, itemId)
    }

    suspend fun getReviewStats(itemType: String, itemId: String): ReviewStatsResponse {
        return apiService.getReviewStats(itemType, itemId)
    }

    suspend fun checkUserReview(itemType: String, itemId: String): Review? {
        return apiService.checkUserReview(itemType, itemId)
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

