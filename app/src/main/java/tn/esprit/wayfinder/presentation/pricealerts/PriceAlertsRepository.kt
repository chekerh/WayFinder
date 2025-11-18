package tn.esprit.wayfinder.presentation.pricealerts

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class PriceAlertsRepository(private val apiService: ApiService) {

    suspend fun createPriceAlert(request: CreatePriceAlertRequest): PriceAlert {
        return apiService.createPriceAlert(request)
    }

    suspend fun getPriceAlerts(activeOnly: Boolean = false): List<PriceAlert> {
        return apiService.getPriceAlerts(activeOnly)
    }

    suspend fun getPriceAlert(alertId: String): PriceAlert {
        return apiService.getPriceAlert(alertId)
    }

    suspend fun updatePriceAlert(alertId: String, request: UpdatePriceAlertRequest): PriceAlert {
        return apiService.updatePriceAlert(alertId, request)
    }

    suspend fun deletePriceAlert(alertId: String): Map<String, String> {
        return apiService.deletePriceAlert(alertId)
    }

    suspend fun deactivatePriceAlert(alertId: String): PriceAlert {
        return apiService.deactivatePriceAlert(alertId)
    }
}

