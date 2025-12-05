package tn.esprit.wayfinder.utils

import tn.esprit.wayfinder.models.FlightOffer
import tn.esprit.wayfinder.models.SharedHotel

/**
 * Calculator for shared costs in group flights
 * Handles hotel sharing, cost splitting, and savings calculation
 */
object SharedCostCalculator {
    
    /**
     * Calculate cost breakdown for a group flight
     * @param flightOffer The selected flight offer
     * @param sharedHotel Optional shared hotel
     * @param memberCount Number of members in the group
     * @return Cost breakdown with savings
     */
    fun calculateGroupCosts(
        flightOffer: FlightOffer,
        sharedHotel: SharedHotel? = null,
        memberCount: Int
    ): GroupCostBreakdown {
        val flightPrice = flightOffer.price?.total?.toDoubleOrNull() ?: 0.0
        val currency = flightOffer.price?.currency ?: "EUR"
        
        // Calculate flight cost with commission per person
        val flightBreakdown = CommissionCalculator.calculateBreakdown(
            basePrice = flightPrice,
            bookingType = "flight"
        )
        val flightCostPerPerson = flightBreakdown.totalPrice
        
        // Calculate hotel cost per person if shared
        val hotelCostPerPerson = sharedHotel?.let {
            it.costPerPerson
        } ?: 0.0
        
        // Calculate total without sharing (individual booking)
        val totalWithoutSharing = flightCostPerPerson + (sharedHotel?.totalCost ?: 0.0)
        
        // Calculate total with sharing
        val totalWithSharing = flightCostPerPerson + hotelCostPerPerson
        
        // Calculate savings per person
        val savingsPerPerson = totalWithoutSharing - totalWithSharing
        
        // Total savings for the group
        val totalSavings = savingsPerPerson * memberCount
        
        return GroupCostBreakdown(
            flightCostPerPerson = flightCostPerPerson,
            hotelCostPerPerson = hotelCostPerPerson,
            totalWithoutSharing = totalWithoutSharing,
            totalWithSharing = totalWithSharing,
            savingsPerPerson = savingsPerPerson,
            totalSavings = totalSavings,
            currency = currency,
            memberCount = memberCount
        )
    }
    
    /**
     * Calculate hotel cost per person based on room type and number of people
     */
    fun calculateHotelCostPerPerson(
        totalHotelCost: Double,
        roomType: String,
        numberOfPeople: Int
    ): Double {
        return when (roomType.lowercase()) {
            "single" -> totalHotelCost // No sharing
            "double" -> totalHotelCost / 2.0
            "triple" -> totalHotelCost / 3.0
            "quad" -> totalHotelCost / 4.0
            else -> totalHotelCost / numberOfPeople.coerceAtLeast(1)
        }
    }
    
    /**
     * Estimate hotel savings based on room sharing
     */
    fun estimateHotelSavings(
        singleRoomCost: Double,
        sharedRoomCost: Double,
        numberOfPeople: Int
    ): Double {
        val individualCost = singleRoomCost * numberOfPeople
        val sharedCost = sharedRoomCost
        return (individualCost - sharedCost).coerceAtLeast(0.0)
    }
}

/**
 * Cost breakdown for group flights
 */
data class GroupCostBreakdown(
    val flightCostPerPerson: Double,
    val hotelCostPerPerson: Double,
    val totalWithoutSharing: Double,
    val totalWithSharing: Double,
    val savingsPerPerson: Double,
    val totalSavings: Double,
    val currency: String,
    val memberCount: Int
) {
    val savingsPercentage: Int
        get() = if (totalWithoutSharing > 0) {
            ((savingsPerPerson / totalWithoutSharing) * 100).toInt()
        } else {
            0
        }
}

