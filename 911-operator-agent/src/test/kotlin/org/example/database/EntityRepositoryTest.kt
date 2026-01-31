package org.example.database

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteAll
import java.nio.file.Path
import java.time.LocalDateTime

class EntityRepositoryTest {
    
    companion object {
        @TempDir
        @JvmStatic
        lateinit var tempDir: Path
        
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            val dbPath = tempDir.resolve("test_entities.db").toString()
            DatabaseFactory.init(dbPath)
        }
    }
    
    @BeforeEach
    fun cleanDatabase() {
        transaction {
            ExtractedEntities.deleteAll()
            Transcripts.deleteAll()
            CallRecords.deleteAll()
        }
    }
    
    @Test
    fun testAddEntity() {
        val callId = "call-001"
        CallRepository.createCall(callId)
        
        val extractedAt = LocalDateTime.now()
        val entityId = EntityRepository.addEntity(
            callId = callId,
            entityType = "address",
            entityValue = "123 Main Street, Springfield",
            confidence = 0.92,
            extractedAt = extractedAt
        )
        
        assertTrue(entityId > 0, "Entity ID should be positive")
        
        // Verify entity was added
        val entities = EntityRepository.getEntities(callId)
        assertEquals(1, entities.size, "Should have one entity")
        
        val entity = entities.first()
        assertEquals(callId, entity[ExtractedEntities.callId])
        assertEquals("address", entity[ExtractedEntities.entityType])
        assertEquals("123 Main Street, Springfield", entity[ExtractedEntities.entityValue])
        assertEquals(0.92, entity[ExtractedEntities.confidence])
    }
    
    @Test
    fun testAddEntityWithDefaults() {
        val callId = "call-002"
        CallRepository.createCall(callId)
        
        val entityId = EntityRepository.addEntity(
            callId = callId,
            entityType = "person_name",
            entityValue = "John Doe"
        )
        
        assertTrue(entityId > 0, "Entity ID should be generated")
        
        val entities = EntityRepository.getEntities(callId)
        assertEquals(1, entities.size)
        
        val entity = entities.first()
        assertEquals("person_name", entity[ExtractedEntities.entityType])
        assertEquals("John Doe", entity[ExtractedEntities.entityValue])
        assertNull(entity[ExtractedEntities.confidence], "Confidence should be null when not provided")
        assertNotNull(entity[ExtractedEntities.extractedAt], "extractedAt should be set to current time")
    }
    
    @Test
    fun testGetEntities() {
        val callId = "call-003"
        CallRepository.createCall(callId)
        
        // Add multiple entities
        EntityRepository.addEntity(callId, "address", "456 Oak Avenue")
        EntityRepository.addEntity(callId, "condition", "chest pain")
        EntityRepository.addEntity(callId, "person_name", "Jane Smith")
        
        val entities = EntityRepository.getEntities(callId)
        
        assertEquals(3, entities.size, "Should retrieve all 3 entities")
        
        val entityTypes = entities.map { it[ExtractedEntities.entityType] }
        assertTrue(entityTypes.contains("address"))
        assertTrue(entityTypes.contains("condition"))
        assertTrue(entityTypes.contains("person_name"))
    }
    
    @Test
    fun testGetEntitiesForNonExistentCall() {
        val entities = EntityRepository.getEntities("non-existent-call")
        
        assertTrue(entities.isEmpty(), "Should return empty list for non-existent call")
    }
    
    @Test
    fun testGetEntitiesByType() {
        val callId = "call-004"
        CallRepository.createCall(callId)
        
        // Add multiple entities of different types
        EntityRepository.addEntity(callId, "address", "123 Main St")
        EntityRepository.addEntity(callId, "address", "456 Oak Ave")
        EntityRepository.addEntity(callId, "condition", "broken leg")
        EntityRepository.addEntity(callId, "landmark", "Central Park")
        EntityRepository.addEntity(callId, "address", "789 Elm Street")
        
        val addressEntities = EntityRepository.getEntitiesByType(callId, "address")
        val conditionEntities = EntityRepository.getEntitiesByType(callId, "condition")
        val landmarkEntities = EntityRepository.getEntitiesByType(callId, "landmark")
        
        assertEquals(3, addressEntities.size, "Should have 3 address entities")
        assertEquals(1, conditionEntities.size, "Should have 1 condition entity")
        assertEquals(1, landmarkEntities.size, "Should have 1 landmark entity")
        
        // Verify all addresses are returned
        val addressValues = addressEntities.map { it[ExtractedEntities.entityValue] }
        assertTrue(addressValues.contains("123 Main St"))
        assertTrue(addressValues.contains("456 Oak Ave"))
        assertTrue(addressValues.contains("789 Elm Street"))
    }
    
    @Test
    fun testGetEntitiesByTypeNonExistent() {
        val callId = "call-005"
        CallRepository.createCall(callId)
        
        EntityRepository.addEntity(callId, "address", "123 Main St")
        
        val phoneEntities = EntityRepository.getEntitiesByType(callId, "phone_number")
        
        assertTrue(phoneEntities.isEmpty(), "Should return empty list for non-existent entity type")
    }
    
    @Test
    fun testMultipleCallsEntities() {
        val callId1 = "call-006"
        val callId2 = "call-007"
        CallRepository.createCall(callId1)
        CallRepository.createCall(callId2)
        
        // Add entities to different calls
        EntityRepository.addEntity(callId1, "address", "100 First Street")
        EntityRepository.addEntity(callId1, "condition", "heart attack")
        EntityRepository.addEntity(callId2, "address", "200 Second Avenue")
        
        val entities1 = EntityRepository.getEntities(callId1)
        val entities2 = EntityRepository.getEntities(callId2)
        
        assertEquals(2, entities1.size, "Call 1 should have 2 entities")
        assertEquals(1, entities2.size, "Call 2 should have 1 entity")
        
        // Verify separation
        assertTrue(entities1.all { it[ExtractedEntities.callId] == callId1 })
        assertTrue(entities2.all { it[ExtractedEntities.callId] == callId2 })
    }
    
    @Test
    fun testEntityTypesVariety() {
        val callId = "call-008"
        CallRepository.createCall(callId)
        
        // Test various entity types
        val entityTypes = listOf(
            "address" to "742 Evergreen Terrace",
            "person_name" to "Homer Simpson",
            "condition" to "unconscious",
            "landmark" to "Springfield Nuclear Plant",
            "phone_number" to "+1-555-0123",
            "vehicle" to "Red sedan, plate ABC-123"
        )
        
        entityTypes.forEach { (type, value) ->
            EntityRepository.addEntity(callId, type, value)
        }
        
        val allEntities = EntityRepository.getEntities(callId)
        assertEquals(entityTypes.size, allEntities.size, "Should store all entity types")
        
        // Verify each type can be retrieved
        entityTypes.forEach { (type, _) ->
            val typeEntities = EntityRepository.getEntitiesByType(callId, type)
            assertEquals(1, typeEntities.size, "Should retrieve entity of type $type")
        }
    }
}
