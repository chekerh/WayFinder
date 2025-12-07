package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpsellProduct(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val currency: String,
    val category: UpsellCategory,
    val commissionRate: Double, // Commission percentage (e.g., 15.0 for 15%)
    val imageUrl: String? = null,
    val features: List<String> = emptyList(),
    val recommended: Boolean = false
)

@Serializable
enum class UpsellCategory {
    @SerialName("travel_insurance")
    TRAVEL_INSURANCE,
    
    @SerialName("airport_transfer")
    AIRPORT_TRANSFER,
    
    @SerialName("car_rental")
    CAR_RENTAL,
    
    @SerialName("activity")
    ACTIVITY,
    
    @SerialName("baggage_insurance")
    BAGGAGE_INSURANCE,
    
    @SerialName("seat_selection")
    SEAT_SELECTION,
    
    @SerialName("lounge_access")
    LOUNGE_ACCESS,
    
    @SerialName("wifi_data")
    WIFI_DATA
}

@Serializable
data class UpsellProductsResponse(
    val products: List<UpsellProduct>,
    val destination: String? = null,
    val dates: String? = null
)

@Serializable
data class SelectedUpsell(
    val productId: String,
    val quantity: Int = 1,
    val price: Double,
    val currency: String,
    val commissionRate: Double,
    val commissionAmount: Double
)

@Serializable
data class UpsellSelectionRequest(
    val destinationId: String,
    val accommodationId: String? = null,
    val selectedUpsells: List<SelectedUpsell> = emptyList()
)

