package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Simple review model for hotel/accommodation reviews from Google Places
@Serializable
data class AccommodationReview(
    val authorName: String = "Anonymous",
    val rating: Double = 0.0,
    val text: String = "",
    val time: Long? = null
)

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
    val amenities: List<String> = emptyList(),
    // Extended properties for hotel details
    val address: String? = null,
    val description: String? = null,
    val photos: List<String> = emptyList(),
    val reviews: List<AccommodationReview> = emptyList(),
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val userRatingsTotal: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    // Helper property for amenities list
    val amenitiesList: List<String>
        get() = amenities
}

// ============ Hotel API Response Models ============

@Serializable
data class HotelSearchResponse(
    val data: List<Hotel> = emptyList(),
    val meta: HotelSearchMeta? = null
)

@Serializable
data class HotelSearchMeta(
    val count: Int = 0,
    val source: String = "fallback"
)

@Serializable
data class Hotel(
    val id: String,
    val hotelId: String,
    val name: String,
    val cityCode: String? = null,
    val rating: Double? = null,
    val type: String? = null,
    val pricePerNight: Double? = null,
    val currency: String? = null,
    val amenities: List<String> = emptyList(),
    val address: HotelAddress? = null,
    val geoCode: GeoCode? = null,
    val description: String? = null,
    val media: List<HotelMedia>? = null,
    val googleRating: Double? = null,
    val googleReviewCount: Int? = null,
    val googlePlaceId: String? = null
)

@Serializable
data class HotelAddress(
    val lines: List<String>? = null,
    val postalCode: String? = null,
    val cityName: String? = null,
    val countryCode: String? = null
)

@Serializable
data class GeoCode(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class HotelMedia(
    val uri: String,
    val category: String? = null
)

@Serializable
data class HotelOffersResponse(
    val data: List<HotelWithOffers> = emptyList(),
    val meta: HotelOffersMeta? = null
)

@Serializable
data class HotelOffersMeta(
    val count: Int = 0
)

@Serializable
data class HotelWithOffers(
    val hotel: Hotel,
    val offers: List<HotelOffer> = emptyList()
)

@Serializable
data class HotelOffer(
    val id: String,
    val hotelId: String,
    val roomType: String? = null,
    val roomDescription: String? = null,
    val bedType: String? = null,
    val price: HotelPrice? = null,
    val policies: HotelPolicies? = null,
    val guests: GuestInfo? = null
)

@Serializable
data class HotelPrice(
    val currency: String,
    val base: String,
    val total: String,
    val taxes: List<HotelTax>? = null
)

@Serializable
data class HotelTax(
    val amount: String,
    val currency: String,
    val code: String? = null
)

@Serializable
data class HotelPolicies(
    val cancellation: CancellationPolicy? = null,
    val checkInTime: String? = null,
    val checkOutTime: String? = null
)

@Serializable
data class CancellationPolicy(
    val deadline: String? = null,
    val amount: String? = null,
    val description: String? = null
)

@Serializable
data class GuestInfo(
    val adults: Int = 1
)

@Serializable
data class HotelDetailResponse(
    val hotel: Hotel? = null,
    val reviews: List<HotelReview> = emptyList(),
    val error: String? = null
)

@Serializable
data class HotelReviewsResponse(
    val reviews: List<HotelReview> = emptyList(),
    val message: String? = null
)

@Serializable
data class HotelReview(
    @SerialName("author_name")
    val authorName: String? = null,
    @SerialName("author_url")
    val authorUrl: String? = null,
    @SerialName("profile_photo_url")
    val profilePhotoUrl: String? = null,
    val rating: Int? = null,
    @SerialName("relative_time_description")
    val relativeTimeDescription: String? = null,
    val text: String? = null,
    val time: Long? = null
)

