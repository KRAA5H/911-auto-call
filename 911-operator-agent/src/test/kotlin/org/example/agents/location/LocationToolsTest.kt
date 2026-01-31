package org.example.agents.location

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LocationToolsTest {
    
    @Test
    fun testVerifyAddress() {
        val tools = LocationTools()
        
        // Test a known address
        val result = tools.verifyAddress("10 Downing Street", "London")
        println("Verify Address Result: $result")
        
        assertTrue(result.isValid, "Address should be verified as valid")
        assertNotNull(result.normalizedAddress, "Normalized address should not be null")
        assertNotNull(result.coordinates, "Coordinates should not be null")
        assertEquals("10 Downing Street, London", result.normalizedAddress)
    }
    
    @Test
    fun testReverseGeocodeLandmarks() {
        val tools = LocationTools()
        
        // Test a famous landmark
        val result = tools.reverseGeocodeLandmarks("Eiffel Tower")
        println("Landmark Result: $result")
        
        assertTrue(result.found, "Landmark should be found")
        assertEquals("Eiffel Tower", result.name)
        assertNotNull(result.coordinates, "Coordinates should not be null")
        assertNotNull(result.address, "Address should not be null")
        assertTrue(result.address!!.contains("Paris"), "Address should contain Paris")
    }
    
    @Test
    fun testCalculateUnitEta() {
        val tools = LocationTools()
        
        // Test ETA calculation
        val result = tools.calculateUnitEta("Tower Bridge, London")
        println("ETA Result: $result")
        
        assertTrue(result.distanceKm >= 0, "Distance should be non-negative")
        assertTrue(result.estimatedTimeMinutes >= 1, "Time should be at least 1 minute")
        assertEquals("Emergency Unit Base, London", result.fromLocation)
        assertEquals("Tower Bridge, London", result.toLocation)
    }
}
