package tn.esprit.wayfinder.presentation.social

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class SocialRepository(private val apiService: ApiService) {

    suspend fun followUser(userId: String): FollowResponse {
        return apiService.followUser(FollowUserRequest(userId))
    }

    suspend fun unfollowUser(userId: String): FollowResponse {
        return apiService.unfollowUser(FollowUserRequest(userId))
    }

    suspend fun checkFollowStatus(userId: String): Boolean {
        return apiService.checkFollowStatus(userId).isFollowing
    }

    suspend fun getFollowers(limit: Int = 50, skip: Int = 0): List<UserPreview> {
        return apiService.getFollowers(limit, skip)
    }

    suspend fun getFollowing(limit: Int = 50, skip: Int = 0): List<UserPreview> {
        return apiService.getFollowing(limit, skip)
    }

    suspend fun getFollowCounts(): FollowCountsResponse {
        return apiService.getFollowCounts()
    }

    suspend fun getFollowCountsByUserId(userId: String): FollowCountsResponse {
        return apiService.getFollowCountsByUserId(userId)
    }

    suspend fun shareTrip(request: ShareTripRequest): SharedTrip {
        return apiService.shareTrip(request)
    }

    suspend fun updateSharedTrip(tripId: String, request: UpdateSharedTripRequest): SharedTrip {
        return apiService.updateSharedTrip(tripId, request)
    }

    suspend fun deleteSharedTrip(tripId: String): Map<String, String> {
        return apiService.deleteSharedTrip(tripId)
    }

    suspend fun getSharedTrip(tripId: String): SharedTrip {
        return apiService.getSharedTrip(tripId)
    }

    suspend fun getUserSharedTrips(userId: String, limit: Int = 20, skip: Int = 0): List<SharedTrip> {
        return apiService.getUserSharedTrips(userId, limit, skip)
    }

    suspend fun getSocialFeed(limit: Int = 20, skip: Int = 0): List<SharedTrip> {
        return apiService.getSocialFeed(limit, skip)
    }

    suspend fun likeSharedTrip(tripId: String): LikeResponse {
        return apiService.likeSharedTrip(tripId)
    }
}

