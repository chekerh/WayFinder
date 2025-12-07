package tn.esprit.wayfinder.models

import kotlinx.serialization.Serializable

@Serializable
data class Accommodation(
    val id: String,
    val name: String,
    val type: String,
    val price: Double,
    val currency: String,
    val rating: Double,
    val imageUrl: String? = null,
    val location: String,
    val amenities: List<String> = emptyList()
)

