package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.ActivityFeedResponse
import tn.esprit.wayfinder.models.TravelActivity
import tn.esprit.wayfinder.presentation.catalog.CatalogRepository

sealed class ActivitiesUiState {
    object Idle : ActivitiesUiState()
    object Loading : ActivitiesUiState()
    data class Success(
        val city: String,
        val activities: List<TravelActivity>,
        val categories: List<String>,
        val source: String,
        val lastUpdated: Long
    ) : ActivitiesUiState()
    data class Error(val message: String) : ActivitiesUiState()
}

class ActivitiesViewModel(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivitiesUiState>(ActivitiesUiState.Idle)
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()

    private var currentCity: String = "Paris"
    private var selectedCategory: String? = null
    private var lastResponse: ActivityFeedResponse? = null
    private val _selectedCategoryFlow = MutableStateFlow<String?>(null)
    val selectedCategoryFlow: StateFlow<String?> = _selectedCategoryFlow.asStateFlow()

    fun loadActivities(city: String = currentCity, category: String? = selectedCategory) {
        currentCity = city
        selectedCategory = category
        _selectedCategoryFlow.value = category

        viewModelScope.launch {
            try {
                _uiState.value = ActivitiesUiState.Loading
                val response = catalogRepository.getActivities(
                    city = city,
                    themes = category,
                    limit = 20
                )
                lastResponse = response
                emitSuccess(response)
            } catch (e: Exception) {
                _uiState.value = ActivitiesUiState.Error(
                    e.message ?: "Impossible de charger les activités pour $city."
                )
            }
        }
    }

    fun retry() {
        loadActivities(currentCity, selectedCategory)
    }

    fun onCategorySelected(category: String?) {
        val normalizedCategory = if (category == selectedCategory) null else category
        selectedCategory = normalizedCategory
        _selectedCategoryFlow.value = normalizedCategory
        val snapshot = lastResponse
        if (snapshot == null) {
            loadActivities(currentCity, normalizedCategory)
        } else {
            emitSuccess(snapshot)
        }
    }

    fun onCitySelected(city: String) {
        if (city.equals(currentCity, ignoreCase = true)) {
            return
        }
        loadActivities(city, null)
    }

    private fun emitSuccess(response: ActivityFeedResponse) {
        val baseActivities = response.items
        if (baseActivities.isEmpty()) {
            _uiState.value = ActivitiesUiState.Error("Aucune activité disponible pour ${response.city}.")
            return
        }

        val filtered = selectedCategory?.let { category ->
            baseActivities.filter { it.category.equals(category, ignoreCase = true) }
        } ?: baseActivities

        val categories = baseActivities.map { it.category }.distinct()

        _uiState.value = ActivitiesUiState.Success(
            city = response.city,
            activities = filtered,
            categories = categories,
            source = response.source,
            lastUpdated = System.currentTimeMillis()
        )
    }
}

