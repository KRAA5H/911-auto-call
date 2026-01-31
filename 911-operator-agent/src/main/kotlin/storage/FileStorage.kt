package org.example.storage

import mu.KotlinLogging
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/**
 * File storage manager for emergency call recordings and transcripts.
 * Handles saving and retrieving audio files and text transcripts to local filesystem.
 */
class FileStorage(private val baseDir: String = "data/calls") {
    
    private val audioDir = "$baseDir/audio"
    private val transcriptDir = "$baseDir/transcripts"
    
    init {
        // Create directory structure on initialization
        File(audioDir).mkdirs()
        File(transcriptDir).mkdirs()
        logger.info { "✅ File storage initialized at: $baseDir" }
        logger.info { "   - Audio directory: $audioDir" }
        logger.info { "   - Transcript directory: $transcriptDir" }
    }
    
    /**
     * Save an audio recording to disk.
     * 
     * @param callId Unique call identifier
     * @param audioData Raw audio bytes
     * @param format File format (mp3, wav, etc.)
     * @return Relative path to the saved file
     */
    fun saveAudioFile(callId: String, audioData: ByteArray, format: String = "mp3"): String {
        val fileName = "${callId}_${timestamp()}.$format"
        val filePath = "$audioDir/$fileName"
        
        try {
            File(filePath).writeBytes(audioData)
            logger.info { "💾 Saved audio file: $filePath (${audioData.size} bytes)" }
            return filePath
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save audio file: $filePath" }
            throw e
        }
    }
    
    /**
     * Save a transcript to disk.
     * 
     * @param callId Unique call identifier
     * @param transcript Full transcript text
     * @param format File format (txt, json, etc.)
     * @return Relative path to the saved file
     */
    fun saveTranscript(callId: String, transcript: String, format: String = "txt"): String {
        val fileName = "${callId}_${timestamp()}.$format"
        val filePath = "$transcriptDir/$fileName"
        
        try {
            File(filePath).writeText(transcript)
            logger.info { "📝 Saved transcript file: $filePath (${transcript.length} characters)" }
            return filePath
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save transcript file: $filePath" }
            throw e
        }
    }
    
    /**
     * Save a structured transcript with timestamps and speakers.
     * 
     * @param callId Unique call identifier
     * @param segments List of transcript segments with metadata
     * @return Relative path to the saved file
     */
    fun saveStructuredTranscript(
        callId: String, 
        segments: List<TranscriptSegment>
    ): String {
        val fileName = "${callId}_${timestamp()}_structured.txt"
        val filePath = "$transcriptDir/$fileName"
        
        try {
            val formattedTranscript = buildString {
                appendLine("Call ID: $callId")
                appendLine("Generated: ${LocalDateTime.now()}")
                appendLine("=" .repeat(60))
                appendLine()
                
                segments.forEach { segment ->
                    appendLine("[${segment.timestamp}] ${segment.speaker.uppercase()}:")
                    appendLine("  ${segment.text}")
                    segment.confidence?.let { appendLine("  (Confidence: ${String.format("%.2f", it * 100)}%)") }
                    appendLine()
                }
            }
            
            File(filePath).writeText(formattedTranscript)
            logger.info { "📝 Saved structured transcript: $filePath" }
            return filePath
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save structured transcript: $filePath" }
            throw e
        }
    }
    
    /**
     * Read an audio file from disk.
     * 
     * @param filePath Path to the audio file
     * @return Audio file bytes
     */
    fun readAudioFile(filePath: String): ByteArray {
        return try {
            File(filePath).readBytes()
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to read audio file: $filePath" }
            throw e
        }
    }
    
    /**
     * Read a transcript file from disk.
     * 
     * @param filePath Path to the transcript file
     * @return Transcript text
     */
    fun readTranscript(filePath: String): String {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to read transcript file: $filePath" }
            throw e
        }
    }
    
    /**
     * Check if a file exists.
     * 
     * @param filePath Path to check
     * @return true if file exists, false otherwise
     */
    fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }
    
    /**
     * Delete a file.
     * 
     * @param filePath Path to the file to delete
     * @return true if deletion was successful
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val deleted = File(filePath).delete()
            if (deleted) {
                logger.info { "🗑️ Deleted file: $filePath" }
            } else {
                logger.warn { "⚠️ File not found or could not be deleted: $filePath" }
            }
            deleted
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to delete file: $filePath" }
            false
        }
    }
    
    /**
     * Get the size of storage directory.
     * 
     * @return Total size in bytes
     */
    fun getStorageSize(): Long {
        return File(baseDir).walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    /**
     * Generate a timestamp string for file naming.
     */
    private fun timestamp(): String {
        return LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    }
}

/**
 * Data class representing a transcript segment.
 */
data class TranscriptSegment(
    val timestamp: LocalDateTime,
    val speaker: String, // "agent" or "caller"
    val text: String,
    val confidence: Double? = null
)
