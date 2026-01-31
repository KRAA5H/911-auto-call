package org.example.agents.location

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

/**
 * LocationTools provides location-based services for the 911 emergency response system.
 * This includes address verification, landmark geocoding, and ETA calculations.
 */
class LocationTools {
    
    // Hardcoded emergency unit location (example: London)
    private val unitLocation = Location(51.5074, -0.1278, "Emergency Unit Base, London")
    
    @Serializable
    data class Location(
        val latitude: Double,
        val longitude: Double,
        val address: String
    )
    
    @Serializable
    data class AddressVerificationResult(
        val isValid: Boolean,
        val normalizedAddress: String?,
        val coordinates: Pair<Double, Double>?,
        val message: String
    )
    
    @Serializable
    data class LandmarkResult(
        val found: Boolean,
        val name: String,
        val coordinates: Pair<Double, Double>?,
        val address: String?,
        val message: String
    )
    
    @Serializable
    data class EtaResult(
        val fromLocation: String,
        val toLocation: String,
        val distanceKm: Double,
        val estimatedTimeMinutes: Int,
        val message: String
    )
    
    /**
     * Verifies if an address exists and returns normalized address with coordinates.
     * 
     * @param address The street address to verify
     * @param city The city name
     * @return AddressVerificationResult with verification details
     */
    fun verifyAddress(address: String, city: String): AddressVerificationResult {
        return runBlocking {
            try {
                // In a production system, this would call a geocoding API (e.g., Google Maps, OpenStreetMap)
                // For this implementation, we'll use a mock verification system
                
                val fullAddress = "$address, $city"
                
                // Mock verification logic - in production, would call actual geocoding API
                val isKnownAddress = when {
                    address.contains("Downing Street", ignoreCase = true) && 
                    city.contains("London", ignoreCase = true) -> true
                    address.contains("Main Street", ignoreCase = true) -> true
                    address.contains("Park Avenue", ignoreCase = true) -> true
                    else -> false
                }
                
                if (isKnownAddress) {
                    // Mock coordinates for known addresses
                    val coords = when {
                        address.contains("Downing Street", ignoreCase = true) -> Pair(51.5034, -0.1276)
                        else -> Pair(51.5074 + (Math.random() - 0.5) * 0.1, -0.1278 + (Math.random() - 0.5) * 0.1)
                    }
                    
                    AddressVerificationResult(
                        isValid = true,
                        normalizedAddress = fullAddress,
                        coordinates = coords,
                        message = "Address verified successfully"
                    )
                } else {
                    AddressVerificationResult(
                        isValid = false,
                        normalizedAddress = null,
                        coordinates = null,
                        message = "Address could not be verified. In production, this would use a geocoding API."
                    )
                }
            } catch (e: Exception) {
                AddressVerificationResult(
                    isValid = false,
                    normalizedAddress = null,
                    coordinates = null,
                    message = "Error verifying address: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Reverse geocodes a landmark name to get its coordinates and address.
     * 
     * @param landmark The name of the landmark (e.g., "Eiffel Tower", "Big Ben")
     * @return LandmarkResult with landmark details
     */
    fun reverseGeocodeLandmarks(landmark: String): LandmarkResult {
        return runBlocking {
            try {
                // In production, this would call a places/landmarks API
                // For now, using a mock database of famous landmarks
                
                val landmarkData = when {
                    landmark.contains("Eiffel Tower", ignoreCase = true) -> 
                        Triple(48.8584, 2.2945, "Champ de Mars, 5 Avenue Anatole France, 75007 Paris, France")
                    landmark.contains("Big Ben", ignoreCase = true) -> 
                        Triple(51.5007, -0.1246, "Westminster, London SW1A 0AA, United Kingdom")
                    landmark.contains("Tower Bridge", ignoreCase = true) -> 
                        Triple(51.5055, -0.0754, "Tower Bridge Rd, London SE1 2UP, United Kingdom")
                    landmark.contains("Statue of Liberty", ignoreCase = true) -> 
                        Triple(40.6892, -74.0445, "Liberty Island, New York, NY 10004, United States")
                    else -> null
                }
                
                if (landmarkData != null) {
                    LandmarkResult(
                        found = true,
                        name = landmark,
                        coordinates = Pair(landmarkData.first, landmarkData.second),
                        address = landmarkData.third,
                        message = "Landmark found and geocoded successfully"
                    )
                } else {
                    LandmarkResult(
                        found = false,
                        name = landmark,
                        coordinates = null,
                        address = null,
                        message = "Landmark not found in database. In production, this would use a places API."
                    )
                }
            } catch (e: Exception) {
                LandmarkResult(
                    found = false,
                    name = landmark,
                    coordinates = null,
                    address = null,
                    message = "Error geocoding landmark: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Calculates the estimated time of arrival (ETA) from the emergency unit location to a destination.
     * 
     * @param destination The destination address or landmark
     * @return EtaResult with distance and time estimates
     */
    fun calculateUnitEta(destination: String): EtaResult {
        return runBlocking {
            try {
                // In production, this would use a routing API (Google Maps Directions, OSRM, etc.)
                // For now, using simplified calculation based on straight-line distance
                
                // First, try to geocode the destination
                val destCoords = when {
                    destination.contains("Tower Bridge", ignoreCase = true) -> Pair(51.5055, -0.0754)
                    destination.contains("Big Ben", ignoreCase = true) -> Pair(51.5007, -0.1246)
                    destination.contains("Downing Street", ignoreCase = true) -> Pair(51.5034, -0.1276)
                    else -> {
                        // Try to parse as coordinates or use a default
                        Pair(51.5074 + (Math.random() - 0.5) * 0.2, -0.1278 + (Math.random() - 0.5) * 0.2)
                    }
                }
                
                // Calculate straight-line distance using Haversine formula
                val distance = calculateDistance(
                    unitLocation.latitude, unitLocation.longitude,
                    destCoords.first, destCoords.second
                )
                
                // Estimate time: assuming average speed of 60 km/h for emergency vehicles
                // Add 20% to account for actual road routing vs straight-line
                val adjustedDistance = distance * 1.2
                val timeInMinutes = (adjustedDistance / 60.0 * 60.0).toInt()
                
                EtaResult(
                    fromLocation = unitLocation.address,
                    toLocation = destination,
                    distanceKm = String.format("%.2f", adjustedDistance).toDouble(),
                    estimatedTimeMinutes = timeInMinutes.coerceAtLeast(1), // Minimum 1 minute
                    message = "ETA calculated successfully (mock calculation)"
                )
            } catch (e: Exception) {
                EtaResult(
                    fromLocation = unitLocation.address,
                    toLocation = destination,
                    distanceKm = 0.0,
                    estimatedTimeMinutes = 0,
                    message = "Error calculating ETA: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Calculates the distance between two points using the Haversine formula.
     * 
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return R * c
    }
}
