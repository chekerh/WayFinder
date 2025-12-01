package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.presentation.social.SocialRepository

sealed class SocialUiState {
    object Idle : SocialUiState()
    object Loading : SocialUiState()
    data class Success(
        val sharedTrips: List<SharedTrip> = emptyList(),
        val followers: List<UserPreview> = emptyList(),
        val following: List<UserPreview> = emptyList(),
        val followCounts: FollowCountsResponse? = null
    ) : SocialUiState()
    data class Error(val message: String) : SocialUiState()
}

data class MapMemoriesState(
    val isLoading: Boolean = false,
    val memories: MapMemoriesResponse? = null,
    val error: String? = null
)

class SocialViewModel(private val socialRepository: SocialRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SocialUiState>(SocialUiState.Idle)
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private val _followStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followStatus: StateFlow<Map<String, Boolean>> = _followStatus.asStateFlow()

    private val _mapMemoriesState = MutableStateFlow<MapMemoriesState>(MapMemoriesState())
    val mapMemoriesState: StateFlow<MapMemoriesState> = _mapMemoriesState.asStateFlow()

    fun loadSocialFeed(limit: Int = 20, skip: Int = 0) {
        viewModelScope.launch {
            _uiState.value = SocialUiState.Loading
            try {
                val trips = socialRepository.getSocialFeed(limit, skip)
                val currentState = _uiState.value as? SocialUiState.Success
                _uiState.value = SocialUiState.Success(
                    sharedTrips = trips,
                    followers = currentState?.followers ?: emptyList(),
                    following = currentState?.following ?: emptyList(),
                    followCounts = currentState?.followCounts
                )
            } catch (e: Exception) {
                _uiState.value = SocialUiState.Error(e.message ?: "Failed to load social feed")
            }
        }
    }

    fun loadFollowers(limit: Int = 50, skip: Int = 0) {
        viewModelScope.launch {
            try {
                val followers = socialRepository.getFollowers(limit, skip)
                val currentState = _uiState.value as? SocialUiState.Success
                _uiState.value = SocialUiState.Success(
                    sharedTrips = currentState?.sharedTrips ?: emptyList(),
                    followers = followers,
                    following = currentState?.following ?: emptyList(),
                    followCounts = currentState?.followCounts
                )
            } catch (e: Exception) {
                _uiState.value = SocialUiState.Error(e.message ?: "Failed to load followers")
            }
        }
    }

    fun loadFollowing(limit: Int = 50, skip: Int = 0) {
        viewModelScope.launch {
            try {
                val following = socialRepository.getFollowing(limit, skip)
                val currentState = _uiState.value as? SocialUiState.Success
                _uiState.value = SocialUiState.Success(
                    sharedTrips = currentState?.sharedTrips ?: emptyList(),
                    followers = currentState?.followers ?: emptyList(),
                    following = following,
                    followCounts = currentState?.followCounts
                )
            } catch (e: Exception) {
                _uiState.value = SocialUiState.Error(e.message ?: "Failed to load following")
            }
        }
    }

    fun loadFollowCounts(userId: String? = null) {
        viewModelScope.launch {
            try {
                val counts = if (userId != null) {
                    socialRepository.getFollowCountsByUserId(userId)
                } else {
                    socialRepository.getFollowCounts()
                }
                val currentState = _uiState.value as? SocialUiState.Success
                _uiState.value = SocialUiState.Success(
                    sharedTrips = currentState?.sharedTrips ?: emptyList(),
                    followers = currentState?.followers ?: emptyList(),
                    following = currentState?.following ?: emptyList(),
                    followCounts = counts
                )
            } catch (e: Exception) {
                // Silently fail for follow counts
            }
        }
    }

    fun checkFollowStatus(userId: String) {
        viewModelScope.launch {
            try {
                val isFollowing = socialRepository.checkFollowStatus(userId)
                _followStatus.value = _followStatus.value.toMutableMap().apply {
                    put(userId, isFollowing)
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    fun followUser(userId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                socialRepository.followUser(userId)
                _followStatus.value = _followStatus.value.toMutableMap().apply {
                    put(userId, true)
                }
                loadFollowCounts()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = SocialUiState.Error(e.message ?: "Failed to follow user")
            }
        }
    }

    fun unfollowUser(userId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                socialRepository.unfollowUser(userId)
                _followStatus.value = _followStatus.value.toMutableMap().apply {
                    put(userId, false)
                }
                loadFollowCounts()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = SocialUiState.Error(e.message ?: "Failed to unfollow user")
            }
        }
    }

    fun shareTrip(request: ShareTripRequest, onSuccess: (SharedTrip) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val trip = socialRepository.shareTrip(request)
                val currentState = _uiState.value as? SocialUiState.Success
                _uiState.value = SocialUiState.Success(
                    sharedTrips = listOf(trip) + (currentState?.sharedTrips ?: emptyList()),
                    followers = currentState?.followers ?: emptyList(),
                    following = currentState?.following ?: emptyList(),
                    followCounts = currentState?.followCounts
                )
                onSuccess(trip)
            } catch (e: Exception) {
                _uiState.value = SocialUiState.Error(e.message ?: "Failed to share trip")
            }
        }
    }

    fun likeSharedTrip(tripId: String, onSuccess: (Int) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = socialRepository.likeSharedTrip(tripId)
                val currentState = _uiState.value as? SocialUiState.Success
                val updatedTrips = currentState?.sharedTrips?.map { trip ->
                    if (trip.id == tripId) {
                        trip.copy(likesCount = response.likesCount)
                    } else {
                        trip
                    }
                } ?: emptyList()
                _uiState.value = SocialUiState.Success(
                    sharedTrips = updatedTrips,
                    followers = currentState?.followers ?: emptyList(),
                    following = currentState?.following ?: emptyList(),
                    followCounts = currentState?.followCounts
                )
                onSuccess(response.likesCount)
            } catch (e: Exception) {
                // Silently fail for likes
            }
        }
    }

    fun getUserSharedTrips(userId: String, limit: Int = 20, skip: Int = 0) {
        viewModelScope.launch {
            _uiState.value = SocialUiState.Loading
            try {
                val trips = socialRepository.getUserSharedTrips(userId, limit, skip)
                val currentState = _uiState.value as? SocialUiState.Success
                _uiState.value = SocialUiState.Success(
                    sharedTrips = trips,
                    followers = currentState?.followers ?: emptyList(),
                    following = currentState?.following ?: emptyList(),
                    followCounts = currentState?.followCounts
                )
            } catch (e: Exception) {
                _uiState.value = SocialUiState.Error(e.message ?: "Failed to load user trips")
            }
        }
    }

    fun loadMapMemories() {
        viewModelScope.launch {
            _mapMemoriesState.value = MapMemoriesState(isLoading = true)
            try {
                val memories = socialRepository.getMapMemories()
                _mapMemoriesState.value = MapMemoriesState(
                    isLoading = false,
                    memories = memories,
                    error = null
                )
            } catch (e: Exception) {
                _mapMemoriesState.value = MapMemoriesState(
                    isLoading = false,
                    memories = null,
                    error = e.message ?: "Failed to load map memories"
                )
            }
        }
    }
}

