package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.presentation.searchhistory.SearchHistoryRepository

sealed class SearchHistoryUiState {
    object Idle : SearchHistoryUiState()
    object Loading : SearchHistoryUiState()
    data class Success(
        val recentSearches: List<SearchHistory> = emptyList(),
        val savedSearches: List<SearchHistory> = emptyList(),
        val stats: SearchStatsResponse? = null
    ) : SearchHistoryUiState()
    data class Error(val message: String) : SearchHistoryUiState()
}

class SearchHistoryViewModel(private val searchHistoryRepository: SearchHistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchHistoryUiState>(SearchHistoryUiState.Idle)
    val uiState: StateFlow<SearchHistoryUiState> = _uiState.asStateFlow()

    fun recordSearch(request: CreateSearchHistoryRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                searchHistoryRepository.recordSearch(request)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = SearchHistoryUiState.Error(e.message ?: "Failed to record search")
            }
        }
    }

    fun loadRecentSearches(searchType: String? = null, limit: Int = 20, skip: Int = 0) {
        viewModelScope.launch {
            _uiState.value = SearchHistoryUiState.Loading
            try {
                val recent = searchHistoryRepository.getRecentSearches(searchType, limit, skip)
                val currentState = _uiState.value as? SearchHistoryUiState.Success
                _uiState.value = SearchHistoryUiState.Success(
                    recentSearches = recent,
                    savedSearches = currentState?.savedSearches ?: emptyList(),
                    stats = currentState?.stats
                )
            } catch (e: Exception) {
                _uiState.value = SearchHistoryUiState.Error(e.message ?: "Failed to load recent searches")
            }
        }
    }

    fun loadSavedSearches(searchType: String? = null, limit: Int = 50, skip: Int = 0) {
        viewModelScope.launch {
            _uiState.value = SearchHistoryUiState.Loading
            try {
                val saved = searchHistoryRepository.getSavedSearches(searchType, limit, skip)
                val currentState = _uiState.value as? SearchHistoryUiState.Success
                _uiState.value = SearchHistoryUiState.Success(
                    recentSearches = currentState?.recentSearches ?: emptyList(),
                    savedSearches = saved,
                    stats = currentState?.stats
                )
            } catch (e: Exception) {
                _uiState.value = SearchHistoryUiState.Error(e.message ?: "Failed to load saved searches")
            }
        }
    }

    fun saveSearch(searchId: String, savedName: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                searchHistoryRepository.saveSearch(searchId, savedName)
                loadSavedSearches()
                loadRecentSearches()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = SearchHistoryUiState.Error(e.message ?: "Failed to save search")
            }
        }
    }

    fun unsaveSearch(searchId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                searchHistoryRepository.unsaveSearch(searchId)
                loadSavedSearches()
                loadRecentSearches()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = SearchHistoryUiState.Error(e.message ?: "Failed to unsave search")
            }
        }
    }

    fun deleteSearchHistory(searchId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                searchHistoryRepository.deleteSearchHistory(searchId)
                loadRecentSearches()
                loadSavedSearches()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = SearchHistoryUiState.Error(e.message ?: "Failed to delete search")
            }
        }
    }

    fun clearRecentSearches(searchType: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                searchHistoryRepository.clearRecentSearches(searchType)
                loadRecentSearches(searchType)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = SearchHistoryUiState.Error(e.message ?: "Failed to clear recent searches")
            }
        }
    }

    fun loadSearchStats() {
        viewModelScope.launch {
            try {
                val stats = searchHistoryRepository.getSearchStats()
                val currentState = _uiState.value as? SearchHistoryUiState.Success
                _uiState.value = SearchHistoryUiState.Success(
                    recentSearches = currentState?.recentSearches ?: emptyList(),
                    savedSearches = currentState?.savedSearches ?: emptyList(),
                    stats = stats
                )
            } catch (e: Exception) {
                // Silently fail for stats
            }
        }
    }
}

