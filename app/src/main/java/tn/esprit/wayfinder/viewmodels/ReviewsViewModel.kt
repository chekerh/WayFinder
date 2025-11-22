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
                // Load reviews - return empty list if error (no error state for empty reviews)
                val reviews = try {
                    reviewsRepository.getReviews(itemType, itemId)
                } catch (e: Exception) {
                    android.util.Log.e("ReviewsViewModel", "Error loading reviews: ${e.message}", e)
                    emptyList() // Return empty list instead of failing
                }
                
                // Load stats - return null if error
                val stats = try {
                    reviewsRepository.getReviewStats(itemType, itemId)
                } catch (e: Exception) {
                    android.util.Log.e("ReviewsViewModel", "Error loading stats: ${e.message}", e)
                    null // Return null if stats can't be loaded
                }
                
                // Check user review - return null if error
                val userReview = try {
                    reviewsRepository.checkUserReview(itemType, itemId)
                } catch (e: Exception) {
                    android.util.Log.e("ReviewsViewModel", "Error checking user review: ${e.message}", e)
                    null // Return null if user review check fails
                }
                
                // Always set to Success state, even if some data is missing
                // This prevents error messages from showing when data is simply not available
                _uiState.value = ReviewsUiState.Success(reviews, stats, userReview)
            } catch (e: Exception) {
                android.util.Log.e("ReviewsViewModel", "Unexpected error: ${e.message}", e)
                // Don't show error state - instead show empty state
                // This prevents technical error messages from appearing in UI
                _uiState.value = ReviewsUiState.Success(emptyList(), null, null)
            }
        }
    }

    fun createReview(itemType: String, itemId: String, rating: Int, comment: String?, details: Map<String, Int>? = null) {
        viewModelScope.launch {
            try {
                reviewsRepository.createReview(itemType, itemId, rating, comment, details)
                loadReviews(itemType, itemId) // Refresh reviews
            } catch (e: Exception) {
                android.util.Log.e("ReviewsViewModel", "Error creating review: ${e.message}", e)
                _uiState.value = ReviewsUiState.Error("Impossible de créer l'avis. Réessayez plus tard.")
            }
        }
    }

    fun updateReview(reviewId: String, itemType: String, itemId: String, rating: Int?, comment: String?, details: Map<String, Int>? = null) {
        viewModelScope.launch {
            try {
                reviewsRepository.updateReview(reviewId, rating, comment, details)
                loadReviews(itemType, itemId) // Refresh reviews
            } catch (e: Exception) {
                android.util.Log.e("ReviewsViewModel", "Error updating review: ${e.message}", e)
                _uiState.value = ReviewsUiState.Error("Impossible de modifier l'avis. Réessayez plus tard.")
            }
        }
    }

    fun deleteReview(reviewId: String, itemType: String, itemId: String) {
        viewModelScope.launch {
            try {
                reviewsRepository.deleteReview(reviewId)
                loadReviews(itemType, itemId) // Refresh reviews
            } catch (e: Exception) {
                android.util.Log.e("ReviewsViewModel", "Error deleting review: ${e.message}", e)
                _uiState.value = ReviewsUiState.Error("Impossible de supprimer l'avis. Réessayez plus tard.")
            }
        }
    }
}

