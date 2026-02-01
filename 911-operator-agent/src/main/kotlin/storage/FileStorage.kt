package org.example.storage

import mu.KotlinLogging
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Logger setup (requires kotlin-logging dependency)
private val logger = KotlinLogging.logger {}

/**
 * File storage manager for emergency call recordings and transcripts.
 */
class FileStorage(private val baseDir: String = "data/calls") {

    private val audioDir = "$baseDir/audio"
    private val transcriptDir = "$baseDir/transcripts"

    init {
        // Ensure directories exist
        File(audioDir).mkdirs()
        File(transcriptDir).mkdirs()
        logger.info { "✅ File storage initialized at: $baseDir" }
    }

    /**
     * Save a raw text transcript to disk.
     */
    fun saveTranscript(callId: String, transcript: String, format: String = "txt"): String {
        // Sanitize callId to prevent directory traversal or invalid filenames
        val safeCallId = callId.replace(Regex("[^a-zA-Z0-9-]"), "_")
        val fileName = "${safeCallId}_${timestamp()}.$format"
        val filePath = "$transcriptDir/$fileName"

        try {
            File(filePath).writeText(transcript)
            logger.info { "📝 Saved transcript file: $filePath" }
            return filePath
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to save transcript file: $filePath" }
            throw e
        }
    }

    // ... (Keep your other methods: saveAudioFile, readAudioFile, etc.) ...

    private fun timestamp(): String {
        return LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    }
}

// Data class for future structured usage
data class TranscriptSegment(
    val timestamp: LocalDateTime,
    val speaker: String,
    val text: String,
    val confidence: Double? = null
)