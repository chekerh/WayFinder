package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.Review
import tn.esprit.wayfinder.models.ReviewStatsResponse
import tn.esprit.wayfinder.presentation.reviews.ReviewsRepository

sealed class ReviewsUiState {
    object Idle : ReviewsUiState()
    object Loading : ReviewsUiState()
    data class Success(
        val reviews: List<Review>,
        val stats: ReviewStatsResponse? = null,
        val userReview: Review? = null
    ) : ReviewsUiState()
    data class Error(val message: String) : ReviewsUiState()
}

class ReviewsViewModel(private val reviewsRepository: ReviewsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewsUiState>(ReviewsUiState.Idle)
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    fun loadReviews(itemType: String, itemId: String) {
        viewModelScope.launch {
            _uiState.value = ReviewsUiState.Loading
            try {
                val reviews = reviewsRepository.getReviews(itemType, itemId)
                val stats = reviewsRepository.getReviewStats(itemType, itemId)
                val userReview = reviewsRepository.checkUserReview(itemType, itemId)
                _uiState.value = ReviewsUiState.Success(reviews, stats, userReview)
            } catch (e: Exception) {
                _uiState.value = ReviewsUiState.Error(e.message ?: "Failed to load reviews")
            }
        }
    }

    fun createReview(itemType: String, itemId: String, rating: Int, comment: String?, details: Map<String, Int>? = null) {
        viewModelScope.launch {
            try {
                reviewsRepository.createReview(itemType, itemId, rating, comment, details)
                loadReviews(itemType, itemId) // Refresh reviews
            } catch (e: Exception) {
                _uiState.value = ReviewsUiState.Error(e.message ?: "Failed to create review")
            }
        }
    }

    fun updateReview(reviewId: String, itemType: String, itemId: String, rating: Int?, comment: String?, details: Map<String, Int>? = null) {
        viewModelScope.launch {
            try {
                reviewsRepository.updateReview(reviewId, rating, comment, details)
                loadReviews(itemType, itemId) // Refresh reviews
            } catch (e: Exception) {
                _uiState.value = ReviewsUiState.Error(e.message ?: "Failed to update review")
            }
        }
    }

    fun deleteReview(reviewId: String, itemType: String, itemId: String) {
        viewModelScope.launch {
            try {
                reviewsRepository.deleteReview(reviewId)
                loadReviews(itemType, itemId) // Refresh reviews
            } catch (e: Exception) {
                _uiState.value = ReviewsUiState.Error(e.message ?: "Failed to delete review")
            }
        }
    }
}

