package tn.esprit.wayfinder.presentation.searchhistory

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class SearchHistoryRepository(private val apiService: ApiService) {

    suspend fun recordSearch(request: CreateSearchHistoryRequest): SearchHistory {
        return apiService.recordSearch(request)
    }

    suspend fun getRecentSearches(searchType: String? = null, limit: Int = 20, skip: Int = 0): List<SearchHistory> {
        return apiService.getRecentSearches(searchType, limit, skip)
    }

    suspend fun getSavedSearches(searchType: String? = null, limit: Int = 50, skip: Int = 0): List<SearchHistory> {
        return apiService.getSavedSearches(searchType, limit, skip)
    }

    suspend fun saveSearch(searchId: String, savedName: String): SearchHistory {
        return apiService.saveSearch(searchId, SaveSearchRequest(savedName))
    }

    suspend fun unsaveSearch(searchId: String): SearchHistory {
        return apiService.unsaveSearch(searchId)
    }

    suspend fun deleteSearchHistory(searchId: String): Map<String, String> {
        return apiService.deleteSearchHistory(searchId)
    }

    suspend fun clearRecentSearches(searchType: String? = null): Map<String, String> {
        return apiService.clearRecentSearches(searchType)
    }

    suspend fun getSearchStats(): SearchStatsResponse {
        return apiService.getSearchStats()
    }
}

