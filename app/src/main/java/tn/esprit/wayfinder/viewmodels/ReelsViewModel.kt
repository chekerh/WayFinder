package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import tn.esprit.wayfinder.models.DiscussionPost
import tn.esprit.wayfinder.models.Journey
import tn.esprit.wayfinder.presentation.discussion.DiscussionRepository
import tn.esprit.wayfinder.presentation.journey.JourneyRepository

/**
 * Unified content item for reels feed
 */
sealed class ReelContentItem {
    data class PostItem(val post: DiscussionPost) : ReelContentItem()
    data class JourneyItem(val journey: Journey) : ReelContentItem()
    
    val id: String
        get() = when (this) {
            is PostItem -> "post_${post.id}"
            is JourneyItem -> "journey_${journey.id}"
        }
    
    val likesCount: Int
        get() = when (this) {
            is PostItem -> post.likesCount
            is JourneyItem -> journey.likesCount
        }
    
    val commentsCount: Int
        get() = when (this) {
            is PostItem -> post.commentsCount
            is JourneyItem -> journey.commentsCount
        }
    
    val createdAt: String
        get() = when (this) {
            is PostItem -> post.createdAt
            is JourneyItem -> journey.createdAt ?: ""
        }
    
    val isLiked: Boolean
        get() = when (this) {
            is PostItem -> false // Will be determined by likedBy list
            is JourneyItem -> journey.isLiked
        }
}

sealed class ReelsUiState {
    object Idle : ReelsUiState()
    object Loading : ReelsUiState()
    data class Success(
        val items: List<ReelContentItem>,
        val hasMore: Boolean = false
    ) : ReelsUiState()
    data class Error(val message: String) : ReelsUiState()
}

class ReelsViewModel(
    private val discussionRepository: DiscussionRepository,
    private val journeyRepository: JourneyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ReelsUiState>(ReelsUiState.Idle)
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()
    
    private var currentPage = 0
    private val pageSize = 10
    private var hasMore = true
    
    fun loadReelsFeed(refresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (refresh) {
                    currentPage = 0
                    hasMore = true
                }
                
                if (!hasMore && !refresh) {
                    return@launch
                }
                
                _uiState.value = ReelsUiState.Loading
                
                // Fetch posts and journeys in parallel
                val postsDeferred = async {
                    try {
                        discussionRepository.getPosts(
                            limit = pageSize,
                            skip = currentPage * pageSize,
                            destination = null
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ReelsViewModel", "Error loading posts: ${e.message}", e)
                        null
                    }
                }
                
                val journeysDeferred = async {
                    try {
                        journeyRepository.getJourneys(limit = pageSize, skip = currentPage * pageSize)
                    } catch (e: Exception) {
                        android.util.Log.e("ReelsViewModel", "Error loading journeys: ${e.message}", e)
                        emptyList()
                    }
                }
                
                val postsResponse = postsDeferred.await()
                val journeys = journeysDeferred.await()
                
                // Convert to ReelContentItem
                val postItems = postsResponse?.data?.map { ReelContentItem.PostItem(it) } ?: emptyList()
                val journeyItems = journeys.map { ReelContentItem.JourneyItem(it) }
                
                // Merge and sort by engagement (likes + comments) and recency
                val allItems = (postItems + journeyItems).sortedWith(
                    compareByDescending<ReelContentItem> { 
                        it.likesCount + it.commentsCount 
                    }.thenByDescending { 
                        it.createdAt 
                    }
                )
                
                val existingItems = if (refresh || currentPage == 0) {
                    emptyList()
                } else {
                    (_uiState.value as? ReelsUiState.Success)?.items ?: emptyList()
                }
                
                val updatedItems = existingItems + allItems
                hasMore = allItems.isNotEmpty() && (postsResponse?.data?.size == pageSize || journeys.size == pageSize)
                
                _uiState.value = ReelsUiState.Success(
                    items = updatedItems,
                    hasMore = hasMore
                )
                
                currentPage++
            } catch (e: Exception) {
                android.util.Log.e("ReelsViewModel", "Error loading reels feed: ${e.message}", e)
                _uiState.value = ReelsUiState.Error(
                    e.message ?: "Failed to load reels feed"
                )
            }
        }
    }
    
    fun likeItem(item: ReelContentItem, currentUserId: String?) {
        viewModelScope.launch {
            try {
                when (item) {
                    is ReelContentItem.PostItem -> {
                        discussionRepository.likePost(item.post.id)
                        // Update state optimistically
                        val currentState = _uiState.value
                        if (currentState is ReelsUiState.Success) {
                            val updatedItems = currentState.items.map { currentItem ->
                                if (currentItem.id == item.id && currentItem is ReelContentItem.PostItem) {
                                    val isLiked = currentUserId != null && currentItem.post.likedBy.contains(currentUserId)
                                    ReelContentItem.PostItem(
                                        post = currentItem.post.copy(
                                            likesCount = if (isLiked) currentItem.post.likesCount - 1 else currentItem.post.likesCount + 1,
                                            likedBy = if (isLiked) {
                                                currentItem.post.likedBy.filter { it != currentUserId }
                                            } else {
                                                (currentItem.post.likedBy + (currentUserId ?: "")).distinct()
                                            }
                                        )
                                    )
                                } else {
                                    currentItem
                                }
                            }
                            _uiState.value = currentState.copy(items = updatedItems)
                        }
                    }
                    is ReelContentItem.JourneyItem -> {
                        journeyRepository.likeJourney(item.journey.id)
                        // Update state optimistically
                        val currentState = _uiState.value
                        if (currentState is ReelsUiState.Success) {
                            val updatedItems = currentState.items.map { currentItem ->
                                if (currentItem.id == item.id && currentItem is ReelContentItem.JourneyItem) {
                                    ReelContentItem.JourneyItem(
                                        journey = currentItem.journey.copy(
                                            likesCount = if (currentItem.journey.isLiked) currentItem.journey.likesCount - 1 else currentItem.journey.likesCount + 1,
                                            isLiked = !currentItem.journey.isLiked
                                        )
                                    )
                                } else {
                                    currentItem
                                }
                            }
                            _uiState.value = currentState.copy(items = updatedItems)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ReelsViewModel", "Error liking item: ${e.message}", e)
                // Reload on error
                loadReelsFeed(refresh = true)
            }
        }
    }
    
    fun incrementCommentCount(itemId: String) {
        val currentState = _uiState.value
        if (currentState is ReelsUiState.Success) {
            val updatedItems = currentState.items.map { currentItem ->
                if (currentItem.id == itemId) {
                    when (currentItem) {
                        is ReelContentItem.PostItem -> {
                            ReelContentItem.PostItem(
                                post = currentItem.post.copy(
                                    commentsCount = currentItem.post.commentsCount + 1
                                )
                            )
                        }
                        is ReelContentItem.JourneyItem -> {
                            ReelContentItem.JourneyItem(
                                journey = currentItem.journey.copy(
                                    commentsCount = currentItem.journey.commentsCount + 1
                                )
                            )
                        }
                    }
                } else {
                    currentItem
                }
            }
            _uiState.value = currentState.copy(items = updatedItems)
        }
    }
    
    fun retry() {
        loadReelsFeed(refresh = true)
    }
}

