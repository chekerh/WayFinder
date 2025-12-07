package tn.esprit.wayfinder.presentation.user

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import tn.esprit.wayfinder.manager.CacheManager
import tn.esprit.wayfinder.models.UpdateProfileRequest
import tn.esprit.wayfinder.models.User
import tn.esprit.wayfinder.models.UserPointsResponse
import tn.esprit.wayfinder.network.ApiService

class UserRepository(
    private val apiService: ApiService,
    private val context: Context? = null
) {
    
    private val cacheManager = context?.let { CacheManager(it) }
    private val TAG = "UserRepository"
    
    suspend fun getProfile(): User = withContext(Dispatchers.IO) {
        // Try cache first
        cacheManager?.get<User>(CacheManager.KEY_USER_PROFILE)?.let { cached ->
            Log.d(TAG, "Returning cached user profile")
            // Refresh in background
            refreshProfileInBackground()
            return@withContext cached
        }
        
        // Cache miss - fetch from API
        try {
            val user = apiService.getProfile()
            cacheManager?.put(CacheManager.KEY_USER_PROFILE, user, CacheManager.TTL_MEDIUM)
            Log.d(TAG, "Fetched and cached user profile from API")
            user
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile from API", e)
            throw e
        }
    }
    
    suspend fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        location: String? = null,
        bio: String? = null,
        preferences: List<String>? = null
    ): User {
        val request = UpdateProfileRequest(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            location = location,
            bio = bio,
            preferences = preferences
        )
        val user = apiService.updateProfile(request)
        // Update cache
        cacheManager?.put(CacheManager.KEY_USER_PROFILE, user, CacheManager.TTL_MEDIUM)
        return user
    }
    
    suspend fun uploadProfileImage(imagePart: MultipartBody.Part): User {
        val response = apiService.uploadProfileImage(imagePart)
        val user = response.user
        // Update cache
        cacheManager?.put(CacheManager.KEY_USER_PROFILE, user, CacheManager.TTL_MEDIUM)
        return user
    }

    suspend fun getUserPoints(): UserPointsResponse = withContext(Dispatchers.IO) {
        // Try cache first
        cacheManager?.get<UserPointsResponse>(CacheManager.KEY_USER_POINTS)?.let { cached ->
            Log.d(TAG, "Returning cached user points")
            // Refresh in background
            refreshUserPointsInBackground()
            return@withContext cached
        }
        
        // Cache miss - fetch from API
        try {
            val points = apiService.getUserPoints()
            cacheManager?.put(CacheManager.KEY_USER_POINTS, points, CacheManager.TTL_SHORT)
            Log.d(TAG, "Fetched and cached user points from API")
            points
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user points from API", e)
            throw e
        }
    }
    
    private suspend fun refreshProfileInBackground() {
        try {
            val user = apiService.getProfile()
            cacheManager?.put(CacheManager.KEY_USER_PROFILE, user, CacheManager.TTL_MEDIUM)
            Log.d(TAG, "Background refresh: updated user profile")
        } catch (e: Exception) {
            Log.w(TAG, "Background refresh failed for user profile", e)
        }
    }
    
    private suspend fun refreshUserPointsInBackground() {
        try {
            val points = apiService.getUserPoints()
            cacheManager?.put(CacheManager.KEY_USER_POINTS, points, CacheManager.TTL_SHORT)
            Log.d(TAG, "Background refresh: updated user points")
        } catch (e: Exception) {
            Log.w(TAG, "Background refresh failed for user points", e)
        }
    }
}

