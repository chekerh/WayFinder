package tn.esprit.wayfinder.presentation.discussion

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class DiscussionRepository(private val apiService: ApiService) {
    
    suspend fun getPosts(limit: Int = 20, skip: Int = 0, destination: String? = null): PostsResponse {
        return apiService.getPosts(limit, skip, destination)
    }
    
    suspend fun getPost(postId: String): DiscussionPost {
        return apiService.getPost(postId)
    }
    
    suspend fun createPost(
        title: String,
        content: String,
        tags: List<String>? = null,
        destination: String? = null,
        imageUrl: String? = null
    ): DiscussionPost {
        val request = CreatePostRequest(
            title = title,
            content = content,
            tags = tags,
            destination = destination,
            imageUrl = imageUrl
        )
        return apiService.createPost(request)
    }
    
    suspend fun updatePost(
        postId: String,
        title: String? = null,
        content: String? = null,
        tags: List<String>? = null
    ): DiscussionPost {
        val request = CreatePostRequest(
            title = title ?: "",
            content = content ?: "",
            tags = tags
        )
        return apiService.updatePost(postId, request)
    }
    
    suspend fun deletePost(postId: String) {
        apiService.deletePost(postId)
    }
    
    suspend fun likePost(postId: String): DiscussionPost {
        return apiService.likePost(postId)
    }
    
    suspend fun getComments(postId: String, limit: Int = 50, skip: Int = 0): CommentsResponse {
        return apiService.getComments(postId, limit, skip)
    }
    
    suspend fun createComment(postId: String, content: String, parentId: String? = null): DiscussionComment {
        val request = CreateCommentRequest(content = content, parentId = parentId)
        return apiService.createComment(postId, request)
    }
    
    suspend fun likeComment(commentId: String): DiscussionComment {
        return apiService.likeComment(commentId)
    }
    
    suspend fun deleteComment(commentId: String) {
        apiService.deleteComment(commentId)
    }
}

