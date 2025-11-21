package tn.esprit.wayfinder.presentation.favorites

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class FavoritesRepository(private val apiService: ApiService) {
    
    suspend fun getFavorites(itemType: String? = null): List<Favorite> {
        Log.d("FavoritesRepository", "getFavorites called with itemType: $itemType")
        Log.d("FavoritesRepository", "About to call apiService.getFavorites...")
        Log.d("FavoritesRepository", "Thread: ${Thread.currentThread().name}")
        Log.d("FavoritesRepository", "apiService is null: ${apiService == null}")
        
        return try {
            Log.d("FavoritesRepository", "Calling apiService.getFavorites now...")
            val result = apiService.getFavorites(itemType)
            Log.d("FavoritesRepository", "apiService.getFavorites returned ${result.size} favorites")
            result
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("FavoritesRepository", "Timeout in apiService.getFavorites", e)
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("FavoritesRepository", "SocketTimeout in apiService.getFavorites", e)
            throw e
        } catch (e: java.net.UnknownHostException) {
            Log.e("FavoritesRepository", "UnknownHost in apiService.getFavorites", e)
            throw e
        } catch (e: java.io.IOException) {
            Log.e("FavoritesRepository", "IOException in apiService.getFavorites: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e("FavoritesRepository", "Unexpected error in apiService.getFavorites: ${e.javaClass.simpleName} - ${e.message}", e)
            e.printStackTrace()
            throw e
        }
    }
    
    suspend fun getFavoriteCount(itemType: String? = null): Int {
        val response = apiService.getFavoriteCount(itemType)
        return response.count
    }
    
    suspend fun checkFavorite(itemType: String, itemId: String): Boolean {
        val response = apiService.checkFavorite(itemType, itemId)
        return response.isFavorite
    }
    
    suspend fun addFavorite(
        itemType: String,
        itemId: String,
        itemData: Map<String, Any>? = null
    ): Favorite {
        val json = Json { ignoreUnknownKeys = true }
        val itemDataJson = itemData?.let { data ->
            buildJsonObject {
                data.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Number -> put(key, value)
                        is Boolean -> put(key, value)
                        else -> put(key, value.toString())
                    }
                }
            }
        }
        
        val request = CreateFavoriteRequest(
            itemType = itemType,
            itemId = itemId,
            itemData = itemDataJson
        )
        return apiService.addFavorite(request)
    }
    
    suspend fun removeFavorite(itemType: String, itemId: String) {
        apiService.removeFavorite(itemType, itemId)
    }
}

