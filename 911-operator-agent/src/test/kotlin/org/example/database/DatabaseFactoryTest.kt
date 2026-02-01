package org.example.database

import database.CallRepository
import database.DatabaseFactory
import database.EntityRepository
import database.TranscriptRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DatabaseFactoryTest {
    
    @Test
    fun testDatabaseCanBeInitialized() {
        // Database may already be initialized by other tests, but we can verify it's ready
        if (!DatabaseFactory.isInitialized()) {
            DatabaseFactory.init()
        }
        
        // Verify initialization status
        assertTrue(DatabaseFactory.isInitialized(), "Database should be marked as initialized")
        
        // Verify we can perform operations (which means tables exist)
        assertDoesNotThrow {
            val calls = CallRepository.getAllCalls()
            assertNotNull(calls, "Should be able to get all calls")
        }
    }
    
    @Test
    fun testDatabaseOperationsWork() {
        // Ensure database is initialized
        if (!DatabaseFactory.isInitialized()) {
            DatabaseFactory.init()
        }
        
        // Verify all repositories can perform basic operations
        assertDoesNotThrow {
            CallRepository.getAllCalls()
        }
        
        assertDoesNotThrow {
            TranscriptRepository.getTranscripts("test-call")
        }
        
        assertDoesNotThrow {
            EntityRepository.getEntities("test-call")
        }
    }
}
