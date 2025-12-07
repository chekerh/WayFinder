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
    
    // Track ongoing like operations to prevent duplicate updates
    private val ongoingLikes = mutableSetOf<String>()

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
        imageUrl: String? = null,
        currentUserId: String? = null
    ) {
        viewModelScope.launch {
            try {
                val newPost = discussionRepository.createPost(title, content, tags, destination, imageUrl)
                android.util.Log.d("DiscussionViewModel", "Post created successfully: ${newPost.id}")
                
                // Add post optimistically to UI immediately
                val currentState = _uiState.value
                if (currentState is DiscussionUiState.Success) {
                    // Add new post at the beginning of the list
                    val updatedPosts = listOf(newPost) + currentState.posts
                    _uiState.value = currentState.copy(
                        posts = updatedPosts,
                        total = currentState.total + 1
                    )
                    android.util.Log.d("DiscussionViewModel", "Post added to UI, total posts: ${updatedPosts.size}")
                } else {
                    // If not in success state, reload
                    android.util.Log.d("DiscussionViewModel", "Not in success state, reloading posts")
                    loadPosts(currentDestination, refresh = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("DiscussionViewModel", "Error creating post: ${e.message}", e)
                _uiState.value = DiscussionUiState.Error(
                    e.message ?: "Failed to create post"
                )
            }
        }
    }

    fun likePost(postId: String, currentUserId: String?) {
        // Optimistic update: update UI immediately
        val currentState = _uiState.value
        if (currentState is DiscussionUiState.Success) {
            val updatedPosts = currentState.posts.map { post ->
                if (post.id == postId) {
                    val isCurrentlyLiked = currentUserId != null && post.likedBy.contains(currentUserId)
                    val newLikedBy = if (isCurrentlyLiked) {
                        // Unlike: remove user from likedBy
                        post.likedBy.filter { it != currentUserId }
                    } else {
                        // Like: add user to likedBy
                        (post.likedBy + (currentUserId ?: "")).distinct()
                    }
                    post.copy(
                        likesCount = if (isCurrentlyLiked) post.likesCount - 1 else post.likesCount + 1,
                        likedBy = newLikedBy
                    )
                } else {
                    post
                }
            }
            _uiState.value = currentState.copy(posts = updatedPosts)
        }
        
        // Update post detail if it's the same post
        val currentDetailState = _postDetailState.value
        if (currentDetailState is PostDetailUiState.Success && currentDetailState.post.id == postId) {
            val post = currentDetailState.post
            val isCurrentlyLiked = currentUserId != null && post.likedBy.contains(currentUserId)
            val newLikedBy = if (isCurrentlyLiked) {
                post.likedBy.filter { it != currentUserId }
            } else {
                (post.likedBy + (currentUserId ?: "")).distinct()
            }
            _postDetailState.value = currentDetailState.copy(
                post = post.copy(
                    likesCount = if (isCurrentlyLiked) post.likesCount - 1 else post.likesCount + 1,
                    likedBy = newLikedBy
                )
            )
        }
        
        // Make API call in background
        viewModelScope.launch {
            try {
                discussionRepository.likePost(postId)
            } catch (e: Exception) {
                // Rollback on error
                loadPosts(currentDestination, refresh = true)
            }
        }
    }

    fun createComment(postId: String, content: String, parentId: String? = null, currentUser: DiscussionUser?) {
        // Optimistic update: add comment immediately to UI
        val currentDetailState = _postDetailState.value
        if (currentDetailState is PostDetailUiState.Success && 
            currentDetailState.post.id == postId && 
            currentUser != null) {
            // Create a temporary comment object for optimistic update
            val tempCommentId = "temp_${System.currentTimeMillis()}"
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())
            
            val tempComment = DiscussionComment(
                id = tempCommentId,
                userId = currentUser,
                postId = postId,
                content = content,
                likesCount = 0,
                likedBy = emptyList(),
                parentId = parentId,
                replies = emptyList(),
                createdAt = now,
                updatedAt = now
            )
            
            val updatedComments = if (parentId != null) {
                // This is a reply - add it to the parent comment's replies
                currentDetailState.comments.map { comment ->
                    if (comment.id == parentId) {
                        // Parent is a top-level comment
                        comment.copy(replies = comment.replies + tempComment)
                    } else {
                        // Check if parentId is in replies (nested reply)
                        val updatedReplies = comment.replies.map { reply ->
                            if (reply.id == parentId) {
                                reply.copy(replies = reply.replies + tempComment)
                            } else {
                                reply
                            }
                        }
                        comment.copy(replies = updatedReplies)
                    }
                }
            } else {
                // This is a top-level comment - add it at the beginning
                listOf(tempComment) + currentDetailState.comments
            }
            
            _postDetailState.value = currentDetailState.copy(comments = updatedComments)
        }
        
        // Make API call in background
        viewModelScope.launch {
            try {
                val newComment = discussionRepository.createComment(postId, content, parentId)
                // Replace temporary comment with real one
                val updatedState = _postDetailState.value
                if (updatedState is PostDetailUiState.Success && updatedState.post.id == postId) {
                    val finalComments = if (parentId != null) {
                        // For replies, find and replace the temp comment in replies
                        updatedState.comments.map { comment ->
                            if (comment.id == parentId) {
                                // Replace temp reply in top-level comment
                                comment.copy(replies = comment.replies.map { reply ->
                                    if (reply.id.startsWith("temp_")) newComment else reply
                                })
                            } else {
                                // Check nested replies
                                comment.copy(replies = comment.replies.map { reply ->
                                    if (reply.id == parentId) {
                                        reply.copy(replies = reply.replies.map { nestedReply ->
                                            if (nestedReply.id.startsWith("temp_")) newComment else nestedReply
                                        })
                                    } else {
                                        reply
                                    }
                                })
                            }
                        }
                    } else {
                        // For top-level comments, replace temp with real
                        updatedState.comments.map { comment ->
                            if (comment.id.startsWith("temp_")) {
                                newComment
                            } else {
                                comment
                            }
                        }
                    }
                    _postDetailState.value = updatedState.copy(comments = finalComments)
                } else {
                    // If state doesn't match, reload to get fresh data
                    loadPostDetail(postId)
                }
            } catch (e: Exception) {
                // Rollback on error - remove temporary comment
                val rollbackState = _postDetailState.value
                if (rollbackState is PostDetailUiState.Success) {
                    val cleanedComments = if (parentId != null) {
                        rollbackState.comments.map { comment ->
                            if (comment.id == parentId) {
                                comment.copy(replies = comment.replies.filter { !it.id.startsWith("temp_") })
                            } else {
                                comment.copy(replies = comment.replies.map { reply ->
                                    if (reply.id == parentId) {
                                        reply.copy(replies = reply.replies.filter { !it.id.startsWith("temp_") })
                                    } else {
                                        reply
                                    }
                                })
                            }
                        }
                    } else {
                        rollbackState.comments.filter { !it.id.startsWith("temp_") }
                    }
                    _postDetailState.value = rollbackState.copy(comments = cleanedComments)
                }
                _postDetailState.value = PostDetailUiState.Error(
                    e.message ?: "Failed to create comment"
                )
            }
        }
    }

    fun likeComment(commentId: String, postId: String, currentUserId: String?) {
        // Prevent duplicate like operations on the same comment
        if (ongoingLikes.contains(commentId)) {
            android.util.Log.d("DiscussionViewModel", "Like operation already in progress for comment $commentId, ignoring")
            return
        }
        
        ongoingLikes.add(commentId)
        
        // Helper function to update a comment recursively (handles nested replies)
        fun updateCommentLike(comment: DiscussionComment): DiscussionComment {
            return if (comment.id == commentId) {
                // This is the comment being liked
                val isCurrentlyLiked = currentUserId != null && comment.likedBy.contains(currentUserId)
                val newLikedBy = if (isCurrentlyLiked) {
                    comment.likedBy.filter { it != currentUserId }
                } else {
                    (comment.likedBy + (currentUserId ?: "")).distinct()
                }
                comment.copy(
                    likesCount = if (isCurrentlyLiked) comment.likesCount - 1 else comment.likesCount + 1,
                    likedBy = newLikedBy
                )
            } else {
                // Check replies recursively
                val updatedReplies = comment.replies.map { reply -> updateCommentLike(reply) }
                comment.copy(replies = updatedReplies)
            }
        }
        
        // Optimistic update: update UI immediately (only once, atomically)
        val currentDetailState = _postDetailState.value
        if (currentDetailState is PostDetailUiState.Success) {
            // Calculate the new state
            val updatedComments = currentDetailState.comments.map { comment -> updateCommentLike(comment) }
            // Single atomic update - this prevents flickering
            _postDetailState.value = currentDetailState.copy(comments = updatedComments)
            android.util.Log.d("DiscussionViewModel", "Optimistic update applied for comment $commentId")
        }
        
        // Make API call in background - don't update state on success since optimistic update already handled it
        viewModelScope.launch {
            try {
                // Call API but ignore the response - optimistic update already handled UI
                discussionRepository.likeComment(commentId)
                // Explicitly do nothing on success - keep the optimistic update
                // No state change, no refresh!
            } catch (e: Exception) {
                // Only rollback on error - but do it silently without showing Loading state
                android.util.Log.e("DiscussionViewModel", "Error liking comment: ${e.message}", e)
                
                // Rollback optimistic update manually without triggering Loading state
                val rollbackState = _postDetailState.value
                if (rollbackState is PostDetailUiState.Success) {
                    // Rollback the optimistic update
                    fun rollbackComment(c: DiscussionComment): DiscussionComment {
                        return if (c.id == commentId) {
                            // Rollback this comment's like
                            val wasLiked = currentUserId != null && c.likedBy.contains(currentUserId)
                            val rolledBackLikedBy = if (wasLiked) {
                                // Was liked, remove it
                                c.likedBy.filter { it != currentUserId }
                            } else {
                                // Was not liked, add it back (shouldn't happen but just in case)
                                (c.likedBy + (currentUserId ?: "")).distinct()
                            }
                            c.copy(
                                likesCount = if (wasLiked) c.likesCount - 1 else c.likesCount + 1,
                                likedBy = rolledBackLikedBy
                            )
                        } else {
                            val updatedReplies = c.replies.map { reply -> rollbackComment(reply) }
                            c.copy(replies = updatedReplies)
                        }
                    }
                    
                    val rolledBackComments = rollbackState.comments.map { comment -> rollbackComment(comment) }
                    // Update state directly without going through Loading - no refresh!
                    _postDetailState.value = rollbackState.copy(comments = rolledBackComments)
                    
                    // Silently reload in background to sync with server, but don't show Loading
                    viewModelScope.launch {
                        try {
                            val post = discussionRepository.getPost(postId)
                            val comments = discussionRepository.getComments(postId)
                            // Only update if still in Success state (user hasn't navigated away)
                            val currentState = _postDetailState.value
                            if (currentState is PostDetailUiState.Success) {
                                _postDetailState.value = PostDetailUiState.Success(
                                    post = post,
                                    comments = comments.comments
                                )
                            }
                        } catch (syncError: Exception) {
                            // Ignore sync errors - we already rolled back
                            android.util.Log.e("DiscussionViewModel", "Error syncing after rollback: ${syncError.message}")
                        }
                    }
                }
            } finally {
                // Always remove from ongoing operations
                ongoingLikes.remove(commentId)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                discussionRepository.deletePost(postId)
                // Remove from UI state
                val currentState = _uiState.value
                if (currentState is DiscussionUiState.Success) {
                    val updatedPosts = currentState.posts.filter { it.id != postId }
                    _uiState.value = currentState.copy(
                        posts = updatedPosts,
                        total = currentState.total - 1
                    )
                }
                // Also update post detail state if it's the deleted post
                val detailState = _postDetailState.value
                if (detailState is PostDetailUiState.Success && detailState.post.id == postId) {
                    _postDetailState.value = PostDetailUiState.Error("Post deleted")
                }
            } catch (e: Exception) {
                _uiState.value = DiscussionUiState.Error(
                    e.message ?: "Failed to delete post"
                )
            }
        }
    }

    fun deleteComment(commentId: String, postId: String) {
        viewModelScope.launch {
            try {
                discussionRepository.deleteComment(commentId)
                // Remove from UI state optimistically
                val currentDetailState = _postDetailState.value
                if (currentDetailState is PostDetailUiState.Success) {
                    // Helper function to remove comment recursively
                    fun removeComment(comment: DiscussionComment): DiscussionComment? {
                        return if (comment.id == commentId) {
                            null // Remove this comment
                        } else {
                            // Check replies
                            val updatedReplies = comment.replies.mapNotNull { reply -> removeComment(reply) }
                            comment.copy(replies = updatedReplies)
                        }
                    }
                    
                    val updatedComments = currentDetailState.comments.mapNotNull { comment -> removeComment(comment) }
                    // Update post comment count
                    val updatedPost = currentDetailState.post.copy(
                        commentsCount = (currentDetailState.post.commentsCount - 1).coerceAtLeast(0)
                    )
                    _postDetailState.value = currentDetailState.copy(
                        post = updatedPost,
                        comments = updatedComments
                    )
                }
            } catch (e: Exception) {
                // Reload on error
                loadPostDetail(postId)
            }
        }
    }

    fun retry() {
        loadPosts(currentDestination, refresh = true)
    }
}

