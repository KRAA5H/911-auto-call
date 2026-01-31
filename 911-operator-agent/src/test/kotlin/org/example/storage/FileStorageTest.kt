package org.example.storage

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime

class FileStorageTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var storage: FileStorage
    
    @BeforeEach
    fun setup() {
        storage = FileStorage(tempDir.toString())
    }
    
    @AfterEach
    fun cleanup() {
        // Clean up temp directory after each test
        tempDir.toFile().deleteRecursively()
    }
    
    @Test
    fun testFileStorageInitialization() {
        val baseDir = tempDir.resolve("test-storage").toString()
        val fileStorage = FileStorage(baseDir)
        
        // Verify directories were created
        assertTrue(File(baseDir).exists(), "Base directory should be created")
        assertTrue(File("$baseDir/audio").exists(), "Audio directory should be created")
        assertTrue(File("$baseDir/transcripts").exists(), "Transcripts directory should be created")
    }
    
    @Test
    fun testSaveAudioFile() {
        val callId = "audio-call-001"
        val audioData = "This is fake audio data".toByteArray()
        
        val filePath = storage.saveAudioFile(callId, audioData, "mp3")
        
        assertNotNull(filePath, "File path should not be null")
        assertTrue(filePath.endsWith(".mp3"), "File should have .mp3 extension")
        assertTrue(filePath.contains(callId), "File path should contain call ID")
        
        // Verify file exists
        val file = File(filePath)
        assertTrue(file.exists(), "Audio file should exist")
        assertTrue(file.isFile, "Audio path should be a file")
        
        // Verify content
        val content = file.readBytes()
        assertArrayEquals(audioData, content, "File content should match saved data")
    }
    
    @Test
    fun testSaveAudioFileWithDifferentFormats() {
        val callId = "audio-call-002"
        val audioData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        
        // Test different formats
        val mp3Path = storage.saveAudioFile(callId, audioData, "mp3")
        val wavPath = storage.saveAudioFile(callId, audioData, "wav")
        val oggPath = storage.saveAudioFile(callId, audioData, "ogg")
        
        assertTrue(mp3Path.endsWith(".mp3"))
        assertTrue(wavPath.endsWith(".wav"))
        assertTrue(oggPath.endsWith(".ogg"))
        
        assertTrue(File(mp3Path).exists())
        assertTrue(File(wavPath).exists())
        assertTrue(File(oggPath).exists())
    }
    
    @Test
    fun testReadAudioFile() {
        val callId = "audio-call-003"
        val audioData = "Sample audio content".toByteArray()
        
        val filePath = storage.saveAudioFile(callId, audioData, "mp3")
        val readData = storage.readAudioFile(filePath)
        
        assertArrayEquals(audioData, readData, "Read data should match original data")
    }
    
    @Test
    fun testReadAudioFileNonExistent() {
        val exception = assertThrows(Exception::class.java) {
            storage.readAudioFile("non/existent/file.mp3")
        }
        
        assertNotNull(exception, "Should throw exception for non-existent file")
    }
    
    @Test
    fun testSaveTranscript() {
        val callId = "transcript-call-001"
        val transcript = "Caller: Help! There's a fire!\nAgent: Where is the fire located?"
        
        val filePath = storage.saveTranscript(callId, transcript, "txt")
        
        assertNotNull(filePath)
        assertTrue(filePath.endsWith(".txt"), "File should have .txt extension")
        assertTrue(filePath.contains(callId), "File path should contain call ID")
        
        // Verify file exists
        val file = File(filePath)
        assertTrue(file.exists(), "Transcript file should exist")
        
        // Verify content
        val content = file.readText()
        assertEquals(transcript, content, "Transcript content should match")
    }
    
    @Test
    fun testSaveTranscriptWithDifferentFormats() {
        val callId = "transcript-call-002"
        val transcript = "Sample transcript"
        
        val txtPath = storage.saveTranscript(callId, transcript, "txt")
        val jsonPath = storage.saveTranscript(callId, transcript, "json")
        
        assertTrue(txtPath.endsWith(".txt"))
        assertTrue(jsonPath.endsWith(".json"))
        assertTrue(File(txtPath).exists())
        assertTrue(File(jsonPath).exists())
    }
    
    @Test
    fun testReadTranscript() {
        val callId = "transcript-call-003"
        val transcript = "This is the call transcript.\nLine 2 of the transcript."
        
        val filePath = storage.saveTranscript(callId, transcript, "txt")
        val readTranscript = storage.readTranscript(filePath)
        
        assertEquals(transcript, readTranscript, "Read transcript should match original")
    }
    
    @Test
    fun testReadTranscriptNonExistent() {
        val exception = assertThrows(Exception::class.java) {
            storage.readTranscript("non/existent/transcript.txt")
        }
        
        assertNotNull(exception, "Should throw exception for non-existent file")
    }
    
    @Test
    fun testSaveStructuredTranscript() {
        val callId = "structured-call-001"
        val segments = listOf(
            TranscriptSegment(
                timestamp = LocalDateTime.now().minusMinutes(5),
                speaker = "agent",
                text = "911, what's your emergency?",
                confidence = 0.98
            ),
            TranscriptSegment(
                timestamp = LocalDateTime.now().minusMinutes(4),
                speaker = "caller",
                text = "My neighbor's house is on fire!",
                confidence = 0.92
            ),
            TranscriptSegment(
                timestamp = LocalDateTime.now().minusMinutes(3),
                speaker = "agent",
                text = "What is the address?",
                confidence = 0.99
            )
        )
        
        val filePath = storage.saveStructuredTranscript(callId, segments)
        
        assertNotNull(filePath)
        assertTrue(filePath.contains(callId))
        assertTrue(filePath.contains("structured"))
        assertTrue(File(filePath).exists())
        
        // Verify content format
        val content = File(filePath).readText()
        assertTrue(content.contains("Call ID: $callId"))
        assertTrue(content.contains("AGENT:"))
        assertTrue(content.contains("CALLER:"))
        assertTrue(content.contains("911, what's your emergency?"))
        assertTrue(content.contains("My neighbor's house is on fire!"))
        assertTrue(content.contains("What is the address?"))
        assertTrue(content.contains("Confidence: 98.00%"))
    }
    
    @Test
    fun testSaveStructuredTranscriptWithoutConfidence() {
        val callId = "structured-call-002"
        val segments = listOf(
            TranscriptSegment(
                timestamp = LocalDateTime.now(),
                speaker = "agent",
                text = "Hello",
                confidence = null
            )
        )
        
        val filePath = storage.saveStructuredTranscript(callId, segments)
        
        assertTrue(File(filePath).exists())
        val content = File(filePath).readText()
        assertTrue(content.contains("Hello"))
    }
    
    @Test
    fun testFileExists() {
        val callId = "exists-call-001"
        val transcript = "Test transcript"
        
        val filePath = storage.saveTranscript(callId, transcript)
        
        assertTrue(storage.fileExists(filePath), "File should exist after saving")
        assertFalse(storage.fileExists("non/existent/file.txt"), "Non-existent file should return false")
    }
    
    @Test
    fun testDeleteFile() {
        val callId = "delete-call-001"
        val transcript = "Test transcript"
        
        val filePath = storage.saveTranscript(callId, transcript)
        assertTrue(File(filePath).exists(), "File should exist before deletion")
        
        val deleted = storage.deleteFile(filePath)
        
        assertTrue(deleted, "Delete should return true")
        assertFalse(File(filePath).exists(), "File should not exist after deletion")
    }
    
    @Test
    fun testDeleteNonExistentFile() {
        val deleted = storage.deleteFile("non/existent/file.txt")
        
        assertFalse(deleted, "Deleting non-existent file should return false")
    }
    
    @Test
    fun testGetStorageSize() {
        // Initially empty
        val initialSize = storage.getStorageSize()
        assertEquals(0L, initialSize, "Initial storage size should be 0")
        
        // Add some files
        val audioData = ByteArray(1000) { it.toByte() }
        val transcript = "A".repeat(500)
        
        storage.saveAudioFile("call-1", audioData)
        storage.saveTranscript("call-2", transcript)
        
        val finalSize = storage.getStorageSize()
        assertTrue(finalSize > 0, "Storage size should be greater than 0 after adding files")
        assertTrue(finalSize >= 1500, "Storage size should be at least the size of saved data")
    }
    
    @Test
    fun testMultipleFilesForSameCall() {
        val callId = "multi-file-call"
        val audioData = "audio".toByteArray()
        val transcript = "transcript"
        
        val audioPath = storage.saveAudioFile(callId, audioData)
        val transcriptPath = storage.saveTranscript(callId, transcript)
        
        assertTrue(File(audioPath).exists())
        assertTrue(File(transcriptPath).exists())
        assertNotEquals(audioPath, transcriptPath, "Audio and transcript should have different paths")
        
        // Both should contain the call ID
        assertTrue(audioPath.contains(callId))
        assertTrue(transcriptPath.contains(callId))
    }
    
    @Test
    fun testLargeFileStorage() {
        val callId = "large-file-call"
        val largeData = ByteArray(1024 * 1024) { (it % 256).toByte() } // 1MB
        
        val filePath = storage.saveAudioFile(callId, largeData)
        val readData = storage.readAudioFile(filePath)
        
        assertEquals(largeData.size, readData.size, "Large file size should be preserved")
        assertArrayEquals(largeData, readData, "Large file content should match")
    }
    
    @Test
    fun testUnicodeTranscriptContent() {
        val callId = "unicode-call"
        val transcript = "Emergency: 紧急情况 🚨 Urgence: émergence Emergencia: Чрезвычайная ситуация"
        
        val filePath = storage.saveTranscript(callId, transcript)
        val readTranscript = storage.readTranscript(filePath)
        
        assertEquals(transcript, readTranscript, "Unicode content should be preserved")
    }
}
