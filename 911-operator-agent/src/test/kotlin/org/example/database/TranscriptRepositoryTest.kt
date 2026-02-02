package org.example.database

import database.CallRecords
import database.CallRepository
import database.DatabaseFactory
import database.ExtractedEntities
import database.TranscriptRepository
import database.Transcripts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteAll
import java.nio.file.Path
import java.time.LocalDateTime

class TranscriptRepositoryTest {
    
    companion object {
        @TempDir
        @JvmStatic
        lateinit var tempDir: Path
        
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            val dbPath = tempDir.resolve("test_transcripts.db").toString()
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
    fun testAddTranscript() {
        val callId = "call-001"
        CallRepository.createCall(callId)
        
        val timestamp = LocalDateTime.now()
        val transcriptId = TranscriptRepository.addTranscript(
            callId = callId,
            speaker = "caller",
            text = "I need help, there's been an accident",
            timestamp = timestamp,
            confidence = 0.95,
            sentiment = "urgent"
        )
        
        assertTrue(transcriptId > 0, "Transcript ID should be positive")
        
        // Verify transcript was added
        val transcripts = TranscriptRepository.getTranscripts(callId)
        assertEquals(1, transcripts.size, "Should have one transcript")
        
        val transcript = transcripts.first()
        assertEquals(callId, transcript[Transcripts.callId])
        assertEquals("caller", transcript[Transcripts.speaker])
        assertEquals("I need help, there's been an accident", transcript[Transcripts.text])
        assertEquals(0.95, transcript[Transcripts.confidence])
        assertEquals("urgent", transcript[Transcripts.sentiment])
    }
    
    @Test
    fun testAddTranscriptWithDefaults() {
        val callId = "call-002"
        CallRepository.createCall(callId)
        
        val transcriptId = TranscriptRepository.addTranscript(
            callId = callId,
            speaker = "agent",
            text = "What is your emergency?"
        )
        
        assertTrue(transcriptId > 0, "Transcript ID should be generated")
        
        val transcripts = TranscriptRepository.getTranscripts(callId)
        assertEquals(1, transcripts.size)
        
        val transcript = transcripts.first()
        assertEquals("agent", transcript[Transcripts.speaker])
        assertEquals("What is your emergency?", transcript[Transcripts.text])
        assertNull(transcript[Transcripts.confidence], "Confidence should be null when not provided")
        assertNull(transcript[Transcripts.sentiment], "Sentiment should be null when not provided")
        assertNotNull(transcript[Transcripts.timestamp], "Timestamp should be set to current time")
    }
    
    @Test
    fun testGetTranscripts() {
        val callId = "call-003"
        CallRepository.createCall(callId)
        
        // Add multiple transcripts
        val timestamp1 = LocalDateTime.now().minusMinutes(5)
        val timestamp2 = LocalDateTime.now().minusMinutes(3)
        val timestamp3 = LocalDateTime.now().minusMinutes(1)
        
        TranscriptRepository.addTranscript(callId, "agent", "911, what's your emergency?", timestamp1)
        TranscriptRepository.addTranscript(callId, "caller", "There's a fire at 123 Main St", timestamp2)
        TranscriptRepository.addTranscript(callId, "agent", "Is everyone out of the building?", timestamp3)
        
        val transcripts = TranscriptRepository.getTranscripts(callId)
        
        assertEquals(3, transcripts.size, "Should retrieve all 3 transcripts")
        
        // Verify ordering by timestamp
        assertEquals("agent", transcripts[0][Transcripts.speaker])
        assertEquals("999, what's your emergency?", transcripts[0][Transcripts.text])
        
        assertEquals("caller", transcripts[1][Transcripts.speaker])
        assertEquals("There's a fire at 123 Main St", transcripts[1][Transcripts.text])
        
        assertEquals("agent", transcripts[2][Transcripts.speaker])
        assertEquals("Is everyone out of the building?", transcripts[2][Transcripts.text])
    }
    
    @Test
    fun testGetTranscriptsForNonExistentCall() {
        val transcripts = TranscriptRepository.getTranscripts("non-existent-call")
        
        assertTrue(transcripts.isEmpty(), "Should return empty list for non-existent call")
    }
    
    @Test
    fun testTranscriptsOrderedByTimestamp() {
        val callId = "call-004"
        CallRepository.createCall(callId)
        
        // Add transcripts in non-chronological order
        val time1 = LocalDateTime.now().minusMinutes(10)
        val time2 = LocalDateTime.now().minusMinutes(20)
        val time3 = LocalDateTime.now().minusMinutes(15)
        
        TranscriptRepository.addTranscript(callId, "caller", "Third message", time1)
        TranscriptRepository.addTranscript(callId, "agent", "First message", time2)
        TranscriptRepository.addTranscript(callId, "caller", "Second message", time3)
        
        val transcripts = TranscriptRepository.getTranscripts(callId)
        
        // Verify they are returned in chronological order
        assertEquals("First message", transcripts[0][Transcripts.text])
        assertEquals("Second message", transcripts[1][Transcripts.text])
        assertEquals("Third message", transcripts[2][Transcripts.text])
    }
    
    @Test
    fun testMultipleCallsTranscripts() {
        val callId1 = "call-005"
        val callId2 = "call-006"
        CallRepository.createCall(callId1)
        CallRepository.createCall(callId2)
        
        // Add transcripts to different calls
        TranscriptRepository.addTranscript(callId1, "agent", "Call 1 message 1")
        TranscriptRepository.addTranscript(callId1, "caller", "Call 1 message 2")
        TranscriptRepository.addTranscript(callId2, "agent", "Call 2 message 1")
        
        val transcripts1 = TranscriptRepository.getTranscripts(callId1)
        val transcripts2 = TranscriptRepository.getTranscripts(callId2)
        
        assertEquals(2, transcripts1.size, "Call 1 should have 2 transcripts")
        assertEquals(1, transcripts2.size, "Call 2 should have 1 transcript")
        
        // Verify separation
        assertTrue(transcripts1.all { it[Transcripts.callId] == callId1 })
        assertTrue(transcripts2.all { it[Transcripts.callId] == callId2 })
    }
}
