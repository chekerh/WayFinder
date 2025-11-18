package tn.esprit.wayfinder.presentation.favorites

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class FavoritesRepository(private val apiService: ApiService) {
    
    suspend fun getFavorites(itemType: String? = null): List<Favorite> {
        return apiService.getFavorites(itemType)
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

