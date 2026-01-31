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

class CallRepositoryTest {
    
    companion object {
        @TempDir
        @JvmStatic
        lateinit var tempDir: Path
        
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            val dbPath = tempDir.resolve("test_calls.db").toString()
            DatabaseFactory.init(dbPath)
        }
    }
    
    @BeforeEach
    fun cleanDatabase() {
        // Clean all records before each test
        transaction {
            ExtractedEntities.deleteAll()
            Transcripts.deleteAll()
            CallRecords.deleteAll()
        }
    }
    
    @Test
    fun testCreateCall() {
        val callId = "test-call-001"
        val startTime = LocalDateTime.now()
        
        val createdId = CallRepository.createCall(callId, startTime, "initiated")
        
        assertEquals(callId, createdId, "Created call ID should match")
        
        // Verify call was created in database
        val retrievedCall = CallRepository.getCall(callId)
        assertNotNull(retrievedCall, "Call should exist in database")
        assertEquals(callId, retrievedCall!![CallRecords.callId])
        assertEquals("initiated", retrievedCall[CallRecords.status])
    }
    
    @Test
    fun testCreateCallWithDefaults() {
        val callId = "test-call-002"
        
        CallRepository.createCall(callId)
        
        val call = CallRepository.getCall(callId)
        assertNotNull(call, "Call should exist")
        assertEquals("initiated", call!![CallRecords.status], "Default status should be 'initiated'")
        assertNotNull(call[CallRecords.startTime], "Start time should be set")
    }
    
    @Test
    fun testUpdateCall() {
        val callId = "test-call-003"
        CallRepository.createCall(callId)
        
        // Update call with various fields
        val endTime = LocalDateTime.now()
        val updateCount = CallRepository.updateCall(
            callId = callId,
            callSid = "sid-12345",
            callerPhone = "+1234567890",
            endTime = endTime,
            duration = 300,
            status = "completed",
            emergencyType = "medical",
            location = "123 Main St, Springfield",
            coordinates = "42.1234,-71.5678",
            severity = 3,
            dispatchStatus = "dispatched",
            audioFileUrl = "/audio/test.mp3",
            transcriptFileUrl = "/transcripts/test.txt",
            metadata = """{"key": "value"}"""
        )
        
        assertEquals(1, updateCount, "One record should be updated")
        
        // Verify all fields were updated
        val call = CallRepository.getCall(callId)
        assertNotNull(call)
        assertEquals("sid-12345", call!![CallRecords.callSid])
        assertEquals("+1234567890", call[CallRecords.callerPhone])
        assertEquals("completed", call[CallRecords.status])
        assertEquals("medical", call[CallRecords.emergencyType])
        assertEquals("123 Main St, Springfield", call[CallRecords.location])
        assertEquals("42.1234,-71.5678", call[CallRecords.coordinates])
        assertEquals(3, call[CallRecords.severity])
        assertEquals("dispatched", call[CallRecords.dispatchStatus])
        assertEquals(300, call[CallRecords.duration])
        assertEquals("/audio/test.mp3", call[CallRecords.audioFileUrl])
        assertEquals("/transcripts/test.txt", call[CallRecords.transcriptFileUrl])
        assertEquals("""{"key": "value"}""", call[CallRecords.metadata])
    }
    
    @Test
    fun testUpdateNonExistentCall() {
        val updateCount = CallRepository.updateCall(
            callId = "non-existent-call",
            status = "completed"
        )
        
        assertEquals(0, updateCount, "No records should be updated for non-existent call")
    }
    
    @Test
    fun testGetCall() {
        val callId = "test-call-004"
        CallRepository.createCall(callId, LocalDateTime.now(), "in_progress")
        
        val call = CallRepository.getCall(callId)
        
        assertNotNull(call, "Call should be retrievable")
        assertEquals(callId, call!![CallRecords.callId])
        assertEquals("in_progress", call[CallRecords.status])
    }
    
    @Test
    fun testGetNonExistentCall() {
        val call = CallRepository.getCall("non-existent")
        
        assertNull(call, "Non-existent call should return null")
    }
    
    @Test
    fun testGetAllCalls() {
        // Create multiple calls
        CallRepository.createCall("call-1", LocalDateTime.now(), "initiated")
        CallRepository.createCall("call-2", LocalDateTime.now(), "in_progress")
        CallRepository.createCall("call-3", LocalDateTime.now(), "completed")
        
        val allCalls = CallRepository.getAllCalls()
        
        assertEquals(3, allCalls.size, "Should retrieve all 3 calls")
        
        val callIds = allCalls.map { it[CallRecords.callId] }
        assertTrue(callIds.contains("call-1"), "Should contain call-1")
        assertTrue(callIds.contains("call-2"), "Should contain call-2")
        assertTrue(callIds.contains("call-3"), "Should contain call-3")
    }
    
    @Test
    fun testGetAllCallsWhenEmpty() {
        val allCalls = CallRepository.getAllCalls()
        
        assertTrue(allCalls.isEmpty(), "Should return empty list when no calls exist")
    }
}
