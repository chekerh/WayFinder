package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.presentation.discussion.DiscussionRepository

sealed class DiscussionUiState {
    object Idle : DiscussionUiState()
    object Loading : DiscussionUiState()
    data class Success(
        val posts: List<DiscussionPost>,
        val total: Int,
        val hasMore: Boolean
    ) : DiscussionUiState()
    data class Error(val message: String) : DiscussionUiState()
}

sealed class PostDetailUiState {
    object Idle : PostDetailUiState()
    object Loading : PostDetailUiState()
    data class Success(
        val post: DiscussionPost,
        val comments: List<DiscussionComment>
    ) : PostDetailUiState()
    data class Error(val message: String) : PostDetailUiState()
}

class DiscussionViewModel(
    private val discussionRepository: DiscussionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscussionUiState>(DiscussionUiState.Idle)
    val uiState: StateFlow<DiscussionUiState> = _uiState.asStateFlow()

    private val _postDetailState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Idle)
    val postDetailState: StateFlow<PostDetailUiState> = _postDetailState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var currentDestination: String? = null

    fun loadPosts(destination: String? = null, refresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (refresh) {
                    currentPage = 0
                    currentDestination = destination
                }
                _uiState.value = DiscussionUiState.Loading
                val response = discussionRepository.getPosts(
                    limit = pageSize,
                    skip = currentPage * pageSize,
                    destination = destination ?: currentDestination
                )
                val existingPosts = if (refresh || currentPage == 0) {
                    emptyList()
                } else {
                    (_uiState.value as? DiscussionUiState.Success)?.posts ?: emptyList()
                }
                _uiState.value = DiscussionUiState.Success(
                    posts = existingPosts + response.posts,
                    total = response.total,
                    hasMore = response.posts.size == pageSize
                )
                currentPage++
            } catch (e: Exception) {
                _uiState.value = DiscussionUiState.Error(
                    e.message ?: "Failed to load posts"
                )
            }
        }
    }

    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            try {
                _postDetailState.value = PostDetailUiState.Loading
                val post = discussionRepository.getPost(postId)
                val comments = discussionRepository.getComments(postId)
                _postDetailState.value = PostDetailUiState.Success(
                    post = post,
                    comments = comments.comments
                )
            } catch (e: Exception) {
                _postDetailState.value = PostDetailUiState.Error(
                    e.message ?: "Failed to load post details"
                )
            }
        }
    }

    fun createPost(
        title: String,
        content: String,
        tags: List<String>? = null,
        destination: String? = null,
        imageUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                discussionRepository.createPost(title, content, tags, destination, imageUrl)
                loadPosts(currentDestination, refresh = true)
            } catch (e: Exception) {
                _uiState.value = DiscussionUiState.Error(
                    e.message ?: "Failed to create post"
                )
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                discussionRepository.likePost(postId)
                // Refresh posts to update like count
                loadPosts(currentDestination, refresh = true)
            } catch (e: Exception) {
                // Silently fail - user can retry
            }
        }
    }

    fun createComment(postId: String, content: String) {
        viewModelScope.launch {
            try {
                discussionRepository.createComment(postId, content)
                loadPostDetail(postId)
            } catch (e: Exception) {
                _postDetailState.value = PostDetailUiState.Error(
                    e.message ?: "Failed to create comment"
                )
            }
        }
    }

    fun likeComment(commentId: String, postId: String) {
        viewModelScope.launch {
            try {
                discussionRepository.likeComment(commentId)
                loadPostDetail(postId)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    fun retry() {
        loadPosts(currentDestination, refresh = true)
    }
}

