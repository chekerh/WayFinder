package tn.esprit.wayfinder.presentation.upsells

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import tn.esprit.wayfinder.manager.CacheManager
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService
import tn.esprit.wayfinder.presentation.catalog.CatalogRepository

class UpsellRepository(
    private val apiService: ApiService,
    private val catalogRepository: CatalogRepository? = null,
    private val context: Context? = null
) {
    
    private val cacheManager = context?.let { CacheManager(it) }
    private val TAG = "UpsellRepository"
    
    suspend fun getUpsellProducts(
        destinationId: String? = null,
        dates: String? = null,
        destinationCity: String? = null,
        userPreferences: Map<String, JsonElement>? = null
    ): List<UpsellProduct> = withContext(Dispatchers.IO) {
        val cacheKey = "cache_upsells_${destinationId}_${destinationCity}_${dates}"
        
        // Try cache first
        cacheManager?.get<List<UpsellProduct>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached upsell products")
            return@withContext cached
        }
        
        val products = mutableListOf<UpsellProduct>()
        
        // First, try to fetch activities from catalog API if we have a destination city
        if (destinationCity != null && catalogRepository != null) {
            try {
                // Extract themes from user preferences if available
                val themes = extractThemesFromPreferences(userPreferences)
                
                // Fetch activities from catalog
                val activitiesResponse = catalogRepository.getActivities(
                    city = destinationCity,
                    themes = themes,
                    limit = 20,
                    radiusMeters = 20000
                )
                
                // Map activities to upsell products
                val activityProducts = activitiesResponse.items.map { activity ->
                    mapActivityToUpsellProduct(activity, destinationCity)
                }
                
                products.addAll(activityProducts)
                Log.d(TAG, "Fetched ${activityProducts.size} activities from catalog API")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching activities from catalog API", e)
            }
        }
        
        // Also try to fetch traditional upsell products (insurance, transfers, etc.)
        try {
            val response = apiService.getUpsellProducts(destinationId, dates)
            products.addAll(response.products)
            Log.d(TAG, "Fetched ${response.products.size} traditional upsell products from API")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching traditional upsell products from API", e)
            // Add some default upsell products if API fails
            if (products.isEmpty()) {
                products.addAll(generateDefaultUpsellProducts())
            }
        }
        
        // If we have no products at all, use mock fallback
        if (products.isEmpty()) {
            Log.w(TAG, "No products found, using mock fallback")
            products.addAll(generateMockUpsellProducts())
        }
        
        // Cache the results
        cacheManager?.put(cacheKey, products, CacheManager.TTL_MEDIUM)
        Log.d(TAG, "Total ${products.size} upsell products available")
        
        products
    }
    
    /**
     * Extract activity themes from user preferences
     * Maps common preference keys to OpenTripMap activity kinds
     */
    private fun extractThemesFromPreferences(preferences: Map<String, JsonElement>?): String? {
        if (preferences == null) return null
        
        val themeMap = mapOf(
            "adventure" to "amusement_centre,theme_park,attraction",
            "culture" to "museum,monument,castle,archaeological,historic",
            "nature" to "natural,beach,park,viewpoint",
            "food" to "restaurant,food,bar,cafe",
            "nightlife" to "nightclub,bar,entertainment",
            "shopping" to "shop,market,shopping_centre",
            "sports" to "sport,stadium,swimming_pool",
            "religion" to "church,cathedral,mosque,temple"
        )
        
        val selectedThemes = mutableListOf<String>()
        
        preferences.forEach { (key, value) ->
            val keyLower = key.lowercase()
            themeMap.forEach { (prefKey, themeKinds) ->
                if (keyLower.contains(prefKey)) {
                    // Check if the value indicates interest
                    val valueStr = value.toString().lowercase()
                    if (valueStr.contains("true") || valueStr.contains("yes") || 
                        valueStr.contains("1") || valueStr.toDoubleOrNull()?.let { it > 0 } == true) {
                        selectedThemes.add(themeKinds)
                    }
                }
            }
        }
        
        return if (selectedThemes.isNotEmpty()) {
            selectedThemes.joinToString(",")
        } else {
            null
        }
    }
    
    /**
     * Map TravelActivity to UpsellProduct
     */
    private fun mapActivityToUpsellProduct(activity: TravelActivity, city: String): UpsellProduct {
        // Determine price - use activity price if available, otherwise estimate based on category
        val price = activity.price ?: estimateActivityPrice(activity.category)
        
        // Build features list
        val features = mutableListOf<String>()
        activity.tags.take(3).forEach { features.add(it) }
        if (activity.rating != null && activity.rating > 0) {
            features.add("⭐ ${String.format("%.1f", activity.rating)}")
        }
        if (activity.address != null) {
            features.add("📍 ${activity.address}")
        }
        
        return UpsellProduct(
            id = "activity_${activity.id}",
            name = activity.name,
            description = activity.description ?: "Découvrez ${activity.name} à ${city}",
            price = price,
            currency = "EUR", // Default currency, could be enhanced
            category = UpsellCategory.ACTIVITY,
            commissionRate = 10.0, // Standard commission for activities
            imageUrl = activity.imageUrl,
            features = features,
            recommended = activity.rating != null && activity.rating >= 4.0
        )
    }
    
    /**
     * Estimate activity price based on category
     */
    private fun estimateActivityPrice(category: String): Double {
        return when (category.lowercase()) {
            "museum", "monument", "castle" -> 15.0
            "amusement_centre", "theme_park" -> 45.0
            "restaurant", "food" -> 30.0
            "bar", "nightclub" -> 20.0
            "park", "beach", "natural" -> 0.0 // Free activities
            "sport", "stadium" -> 25.0
            else -> 20.0 // Default price
        }
    }
    
    /**
     * Generate default upsell products (insurance, transfers, etc.)
     */
    private fun generateDefaultUpsellProducts(): List<UpsellProduct> {
        return listOf(
            UpsellProduct(
                id = "travel_insurance_default",
                name = "Assurance Voyage",
                description = "Protection complète pour votre voyage",
                price = 29.99,
                currency = "EUR",
                category = UpsellCategory.TRAVEL_INSURANCE,
                commissionRate = 18.0,
                recommended = true,
                features = listOf("Annulation", "Bagages", "Assistance médicale")
            ),
            UpsellProduct(
                id = "airport_transfer_default",
                name = "Transfert Aéroport",
                description = "Transfert privé aller-retour",
                price = 45.0,
                currency = "EUR",
                category = UpsellCategory.AIRPORT_TRANSFER,
                commissionRate = 12.0,
                features = listOf("Aller-retour", "Conducteur professionnel")
            )
        )
    }
    
    private fun generateMockUpsellProducts(): List<UpsellProduct> {
        return listOf(
            UpsellProduct(
                id = "travel_insurance_1",
                name = "Assurance Voyage Complète",
                description = "Protection complète pour votre voyage avec annulation, bagages et assistance médicale",
                price = 29.99,
                currency = "EUR",
                category = UpsellCategory.TRAVEL_INSURANCE,
                commissionRate = 18.0,
                recommended = true,
                features = listOf("Annulation", "Bagages", "Assistance médicale", "Rapatriement")
            ),
            UpsellProduct(
                id = "airport_transfer_1",
                name = "Transfert Aéroport",
                description = "Transfert privé aller-retour entre l'aéroport et votre hôtel",
                price = 45.0,
                currency = "EUR",
                category = UpsellCategory.AIRPORT_TRANSFER,
                commissionRate = 12.0,
                features = listOf("Aller-retour", "Conducteur professionnel", "Suivi en temps réel")
            ),
            UpsellProduct(
                id = "car_rental_1",
                name = "Location de Voiture",
                description = "Voiture économique avec assurance complète pour 7 jours",
                price = 199.0,
                currency = "EUR",
                category = UpsellCategory.CAR_RENTAL,
                commissionRate = 15.0,
                features = listOf("Assurance complète", "Kilométrage illimité", "GPS inclus")
            ),
            UpsellProduct(
                id = "lounge_access_1",
                name = "Accès Salon Aéroport",
                description = "Accès au salon VIP avec repas, boissons et WiFi gratuit",
                price = 35.0,
                currency = "EUR",
                category = UpsellCategory.LOUNGE_ACCESS,
                commissionRate = 25.0,
                features = listOf("Repas inclus", "Boissons illimitées", "WiFi gratuit", "Confort")
            ),
            UpsellProduct(
                id = "wifi_data_1",
                name = "Forfait Internet Voyage",
                description = "10GB de données 4G/5G valable dans 50+ pays",
                price = 19.99,
                currency = "EUR",
                category = UpsellCategory.WIFI_DATA,
                commissionRate = 35.0,
                features = listOf("10GB de données", "50+ pays", "Vitesse 4G/5G", "Valable 30 jours")
            ),
            UpsellProduct(
                id = "seat_selection_1",
                name = "Sélection de Siège",
                description = "Choisissez votre siège à l'avance (fenêtre, couloir, ou siège premium)",
                price = 15.0,
                currency = "EUR",
                category = UpsellCategory.SEAT_SELECTION,
                commissionRate = 8.0,
                features = listOf("Choix du siège", "Réservation à l'avance", "Plus d'espace")
            )
        )
    }
}

