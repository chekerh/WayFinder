package tn.esprit.wayfinder.manager

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import android.util.Log
import kotlin.PublishedApi

/**
 * CacheManager provides persistent local storage for API responses
 * Uses EncryptedSharedPreferences for secure storage
 * Supports TTL (Time-To-Live) for cache expiration
 */
class CacheManager(context: Context) {
    
    private val TAG = "CacheManager"
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    internal val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "wayfinder_cache",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
    }
    
    /**
     * Cache data with a TTL (Time-To-Live) in seconds
     * @param key Cache key
     * @param data Data to cache (must be serializable)
     * @param ttlSeconds Time to live in seconds (default: 5 minutes)
     */
    inline fun <reified T> put(key: String, data: T, ttlSeconds: Long = 300) {
        putInternal(key, data, ttlSeconds, serializer())
    }
    
    /**
     * Internal implementation that accepts explicit serializer
     */
    @PublishedApi
    internal fun <T> putInternal(key: String, data: T, ttlSeconds: Long, serializer: KSerializer<T>) {
        try {
            val cacheEntry = CacheEntry(
                data = json.encodeToString(serializer, data),
                timestamp = System.currentTimeMillis(),
                ttlSeconds = ttlSeconds
            )
            val entryJson = json.encodeToString(CacheEntry.serializer(), cacheEntry)
            sharedPreferences.edit().putString(key, entryJson).apply()
            Log.d(TAG, "Cached data for key: $key (TTL: ${ttlSeconds}s)")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching data for key: $key", e)
        }
    }
    
    /**
     * Get cached data if it exists and hasn't expired
     * @param key Cache key
     * @return Cached data or null if not found or expired
     */
    inline fun <reified T> get(key: String): T? {
        return getInternal(key, serializer())
    }
    
    /**
     * Internal implementation that accepts explicit serializer
     */
    @PublishedApi
    internal fun <T> getInternal(key: String, serializer: KSerializer<T>): T? {
        return try {
            val entryJson = sharedPreferences.getString(key, null) ?: return null
            val cacheEntry = json.decodeFromString(CacheEntry.serializer(), entryJson)
            
            // Check if cache is expired
            val age = System.currentTimeMillis() - cacheEntry.timestamp
            val ageSeconds = age / 1000
            
            if (ageSeconds >= cacheEntry.ttlSeconds) {
                Log.d(TAG, "Cache expired for key: $key (age: ${ageSeconds}s, TTL: ${cacheEntry.ttlSeconds}s)")
                // Remove expired cache
                sharedPreferences.edit().remove(key).apply()
                return null
            }
            
            Log.d(TAG, "Cache hit for key: $key (age: ${ageSeconds}s, TTL: ${cacheEntry.ttlSeconds}s)")
            json.decodeFromString(serializer, cacheEntry.data)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cache for key: $key", e)
            // Remove corrupted cache
            sharedPreferences.edit().remove(key).apply()
            null
        }
    }
    
    /**
     * Check if cache exists and is valid
     */
    fun isValid(key: String): Boolean {
        return get<Any>(key) != null
    }
    
    /**
     * Remove cached data
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
        Log.d(TAG, "Removed cache for key: $key")
    }
    
    /**
     * Clear all cache
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
        Log.d(TAG, "Cleared all cache")
    }
    
    /**
     * Clear expired cache entries
     */
    fun clearExpired() {
        try {
            val allEntries = sharedPreferences.all
            val now = System.currentTimeMillis()
            val editor = sharedPreferences.edit()
            var cleared = 0
            
            allEntries.forEach { (key, value) ->
                if (value is String) {
                    try {
                        val cacheEntry = json.decodeFromString(CacheEntry.serializer(), value)
                        val age = (now - cacheEntry.timestamp) / 1000
                        if (age >= cacheEntry.ttlSeconds) {
                            editor.remove(key)
                            cleared++
                        }
                    } catch (e: Exception) {
                        // Invalid entry, remove it
                        editor.remove(key)
                        cleared++
                    }
                }
            }
            
            editor.apply()
            if (cleared > 0) {
                Log.d(TAG, "Cleared $cleared expired cache entries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing expired cache", e)
        }
    }
    
    /**
     * Get cache age in seconds
     */
    fun getCacheAge(key: String): Long? {
        return try {
            val entryJson = sharedPreferences.getString(key, null) ?: return null
            val cacheEntry = json.decodeFromString<CacheEntry>(entryJson)
            (System.currentTimeMillis() - cacheEntry.timestamp) / 1000
        } catch (e: Exception) {
            null
        }
    }
    
    @kotlinx.serialization.Serializable
    data class CacheEntry(
        val data: String,
        val timestamp: Long,
        val ttlSeconds: Long
    )
    
    companion object {
        // Cache keys
        const val KEY_NOTIFICATIONS = "cache_notifications"
        const val KEY_NOTIFICATIONS_UNREAD = "cache_notifications_unread"
        const val KEY_UNREAD_COUNT = "cache_unread_count"
        const val KEY_BOOKING_HISTORY = "cache_booking_history"
        const val KEY_USER_PROFILE = "cache_user_profile"
        const val KEY_USER_POINTS = "cache_user_points"
        const val KEY_RECOMMENDED_FLIGHTS = "cache_recommended_flights"
        const val KEY_EXPLORE_OFFERS = "cache_explore_offers"
        const val KEY_ACTIVITIES = "cache_activities"
        
        // TTL constants (in seconds)
        const val TTL_SHORT = 60L // 1 minute - for frequently changing data
        const val TTL_MEDIUM = 300L // 5 minutes - for moderately changing data
        const val TTL_LONG = 1800L // 30 minutes - for slowly changing data
        const val TTL_VERY_LONG = 3600L // 1 hour - for rarely changing data
    }
}

