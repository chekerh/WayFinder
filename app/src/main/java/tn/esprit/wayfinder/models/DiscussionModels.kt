package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DiscussionUser(
    @SerialName("_id") val id: String,
    val username: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("profile_image_url") val profileImageUrl: String? = null
)

@Serializable
data class DiscussionPost(
    @SerialName("_id") val id: String,
    @SerialName("user_id") val userId: DiscussionUser,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val destination: String? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("liked_by") val likedBy: List<String> = emptyList(),
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class DiscussionComment(
    @SerialName("_id") val id: String,
    @SerialName("user_id") val userId: DiscussionUser,
    @SerialName("post_id") val postId: String,
    val content: String,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("liked_by") val likedBy: List<String> = emptyList(),
    @SerialName("parent_id") val parentId: String? = null,
    val replies: List<DiscussionComment> = emptyList(),
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class DiscussionPaginationInfo(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("has_next") val hasNext: Boolean = false,
    @SerialName("has_prev") val hasPrev: Boolean = false
)

@Serializable
data class PostsResponse(
    val data: List<DiscussionPost> = emptyList(),
    val pagination: DiscussionPaginationInfo = DiscussionPaginationInfo()
)

@Serializable
data class CommentsResponse(
    val data: List<DiscussionComment> = emptyList(),
    val pagination: DiscussionPaginationInfo = DiscussionPaginationInfo()
)

@Serializable
data class CreatePostRequest(
    val title: String,
    val content: String,
    val tags: List<String>? = null,
    val destination: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class CreateCommentRequest(
    val content: String,
    @SerialName("parent_id") val parentId: String? = null
)

