package tn.esprit.wayfinder.utils

/**
 * Commission calculation utility for WayFinder
 * Handles commission calculation for flights, hotels, and other services
 */
object CommissionCalculator {
    
    // Commission rates (configurable)
    private const val FLIGHT_COMMISSION_RATE = 0.12 // 12% commission on flights
    private const val HOTEL_COMMISSION_RATE = 0.15 // 15% commission on hotels
    private const val DEFAULT_COMMISSION_RATE = 0.10 // 10% default commission
    
    // Minimum commission amounts
    private const val MIN_FLIGHT_COMMISSION = 5.0
    private const val MIN_HOTEL_COMMISSION = 3.0
    private const val MIN_DEFAULT_COMMISSION = 2.0
    
    /**
     * Calculate commission for a booking
     * @param basePrice The base price from the provider (flight/hotel)
     * @param bookingType Type of booking: "flight", "hotel", or other
     * @return CommissionData containing commission amount and final price
     */
    fun calculateCommission(
        basePrice: Double,
        bookingType: String = "flight"
    ): CommissionData {
        val commissionRate = when (bookingType.lowercase()) {
            "flight" -> FLIGHT_COMMISSION_RATE
            "hotel" -> HOTEL_COMMISSION_RATE
            else -> DEFAULT_COMMISSION_RATE
        }
        
        val commission = (basePrice * commissionRate).coerceAtLeast(
            when (bookingType.lowercase()) {
                "flight" -> MIN_FLIGHT_COMMISSION
                "hotel" -> MIN_HOTEL_COMMISSION
                else -> MIN_DEFAULT_COMMISSION
            }
        )
        
        val totalPrice = basePrice + commission
        
        return CommissionData(
            basePrice = basePrice,
            commission = commission,
            commissionRate = commissionRate,
            totalPrice = totalPrice,
            bookingType = bookingType
        )
    }
    
    /**
     * Calculate breakdown for display
     */
    fun calculateBreakdown(
        basePrice: Double,
        bookingType: String = "flight"
    ): PriceBreakdown {
        val commissionData = calculateCommission(basePrice, bookingType)
        
        return PriceBreakdown(
            basePrice = commissionData.basePrice,
            commission = commissionData.commission,
            commissionRate = commissionData.commissionRate,
            totalPrice = commissionData.totalPrice,
            bookingType = commissionData.bookingType,
            taxes = 0.0, // Taxes are typically included in base price
            serviceFees = 0.0 // Service fees are part of commission
        )
    }
}

/**
 * Data class for commission calculation results
 */
data class CommissionData(
    val basePrice: Double,
    val commission: Double,
    val commissionRate: Double,
    val totalPrice: Double,
    val bookingType: String
)

/**
 * Detailed price breakdown for UI display
 */
data class PriceBreakdown(
    val basePrice: Double,
    val commission: Double,
    val commissionRate: Double,
    val totalPrice: Double,
    val bookingType: String,
    val taxes: Double,
    val serviceFees: Double
) {
    val commissionPercentage: Int
        get() = (commissionRate * 100).toInt()
}

